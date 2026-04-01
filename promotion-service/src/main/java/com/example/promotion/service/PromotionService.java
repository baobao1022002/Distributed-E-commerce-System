package com.example.promotion.service;

import com.example.promotion.client.ProductClient;
import com.example.promotion.client.dto.ProductDTO;
import com.example.promotion.client.dto.ProductFilterDTO;
import com.example.promotion.dto.CouponDTO;
import com.example.promotion.dto.FlashSaleDTO;
import com.example.promotion.entity.Coupon;
import com.example.promotion.entity.FlashSale;
import com.example.promotion.entity.FlashSale.FlashSaleStatus;
import com.example.promotion.event.FlashSaleCreatedEvent;
import com.example.promotion.repository.CouponRepository;
import com.example.promotion.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final CouponRepository couponRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ProductClient productClient;

    public Optional<Coupon> validateCoupon(String code) {
        log.info("Validating coupon: {}", code);
        return couponRepository.findByCode(code)
                .filter(Coupon::isActive)
                .filter(coupon -> coupon.getEndDate().isAfter(LocalDateTime.now()))
                .filter(coupon -> coupon.getStartDate().isBefore(LocalDateTime.now()));
    }

    public Optional<FlashSale> getActiveFlashSale(String flashSaleId) {
        log.info("Retrieving flash sale: {}", flashSaleId);
        return flashSaleRepository.findById(flashSaleId);
    }

    @Transactional
    public FlashSale createFlashSale(FlashSaleDTO flashSaleDTO) {
        flashSaleRepository.findByNameWithLock(flashSaleDTO.getName()).ifPresent(existing -> {
            throw new IllegalArgumentException("Flash sale name already exists: " + flashSaleDTO.getName());
        });

        // Get product details
        List<ProductDTO> products = productClient.getProductsByIds(new ProductFilterDTO(flashSaleDTO.getProductIds())).stream().toList();
        log.info("Retrieved {} products from product service", products.size());

        flashSaleDTO.getProductIds().forEach(id -> {
            if (products.stream().noneMatch(p -> p.getId().equals(id))) {
                throw new IllegalArgumentException("Product ID not found: " + id);
            }
        });

        log.info("Creating flash sale: {}", flashSaleDTO.getName());
        FlashSale flashSale = FlashSale.builder()
                .name(flashSaleDTO.getName())
                .description(flashSaleDTO.getDescription())
                .productIds(flashSaleDTO.getProductIds())
                .status(flashSaleDTO.getStatus())
                .discountPercentage(flashSaleDTO.getDiscountPercentage())
                .totalStock(flashSaleDTO.getTotalStock())
                .remainingStock(flashSaleDTO.getTotalStock())
                .startTime(flashSaleDTO.getStartTime())
                .endTime(flashSaleDTO.getEndTime())
                .premiumStartTime(flashSaleDTO.getPremiumStartTime())
                .build();

        FlashSale saved = flashSaleRepository.save(flashSale);
        log.info("Flash sale created: {}", saved.getId());

        String stockKey = "flashsale:" + saved.getId() + ":stock";
        redisTemplate.opsForValue().set(stockKey, String.valueOf(saved.getRemainingStock()),
                Duration.between(LocalDateTime.now(), saved.getEndTime())); // third parameter is TTL of redis

        kafkaTemplate.send("flashsale-created", toCreatedEvent(saved));
        return saved;
    }

    public boolean decrementFlashSaleStock(String flashSaleId, int quantity) {
        String stockKey = "flashsale:" + flashSaleId + ":stock";
        String lockKey = "lock:flashsale:" + flashSaleId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // Try to acquire lock (wait max 10 seconds, auto-release after 30 seconds)
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("Failed to acquire lock for flash sale: {}", flashSaleId);
                return false;
            }

            // Step 1: Atomic decrement in Redis
            Long remaining = redisTemplate.opsForValue().decrement(stockKey, quantity);
            if (remaining == null || remaining < 0) {
                if (remaining != null) {
                    redisTemplate.opsForValue().increment(stockKey, quantity);
                }
                log.warn("Insufficient stock in redis for flash sale: {}", flashSaleId);
                return false;
            }

            // Step 2: Update database
            Optional<FlashSale> flashSaleOpt = flashSaleRepository.findById(flashSaleId);
            if (flashSaleOpt.isEmpty()) {
                redisTemplate.opsForValue().increment(stockKey, quantity);
                return false;
            }
            FlashSale sale = flashSaleOpt.get();
            if (sale.getRemainingStock() < quantity) {
                redisTemplate.opsForValue().increment(stockKey, quantity);
                log.warn("Insufficient stock in DB for flash sale: {}", flashSaleId);
                return false;
            }

            // Update DB stock
            sale.setRemainingStock(sale.getRemainingStock() - quantity);
            sale.setSoldQuantity(sale.getSoldQuantity() + quantity);
            flashSaleRepository.save(sale);
            log.info("Stock decremented: flashSale={}, remaining={}", flashSaleId, sale.getRemainingStock());
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock interrupted for flash sale: {}", flashSaleId, e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Coupon createCoupon(CouponDTO dto) {
        couponRepository.findByCode(dto.getCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Coupon code already exists: " + dto.getCode());
        });

        Coupon coupon = Coupon.builder()
                .code(dto.getCode())
                .description(dto.getDescription())
                .type(Coupon.CouponType.valueOf(dto.getType()))
                .discountValue(dto.getDiscountValue())
                .minimumOrderValue(dto.getMinimumOrderValue())
                .active(dto.isActive())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        return couponRepository.save(coupon);
    }

    public Optional<Coupon> applyCoupon(String code, String orderId, BigDecimal orderAmount) {
        Optional<Coupon> validCoupon = validateCoupon(code)
                .filter(coupon -> coupon.getMinimumOrderValue() == null || orderAmount.compareTo(coupon.getMinimumOrderValue()) >= 0);
        validCoupon.ifPresent(coupon -> {
            couponRepository.save(coupon);
            log.info("Coupon {} applied for order {}", code, orderId);
        });
        return validCoupon;
    }

    /**
     * Saga compensation: decrement usage count so the coupon slot is freed when an order fails.
     * Floor-clamped to 0 to guard against double-release races.
     */
    public void releaseCoupon(String code, String orderId) {
        couponRepository.findByCode(code).ifPresentOrElse(coupon -> {
            couponRepository.save(coupon);
            log.info("Coupon usage released: code={}, orderId={}", code, orderId);
        }, () -> log.warn("Coupon not found for release: code={}, orderId={}", code, orderId));
    }

    public List<FlashSale> getActiveFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> candidates = flashSaleRepository.findAll().stream()
                .filter(sale -> sale.getStartTime().isBefore(now) && sale.getEndTime().isAfter(now))
                .collect(Collectors.toList());
        candidates.forEach(sale -> {
            if (sale.getStatus() != FlashSaleStatus.ACTIVE) {
                sale.setStatus(FlashSaleStatus.ACTIVE);
                flashSaleRepository.save(sale);
            }
        });
        return candidates;
    }

    private FlashSaleCreatedEvent toCreatedEvent(FlashSale sale) {
        List<ProductDTO> products = productClient.getAllProducts();
        List<String> productNames = products.stream()
                .filter(p -> sale.getProductIds().contains(p.getId()))
                .map(ProductDTO::getName)
                .toList();
        return new FlashSaleCreatedEvent(
                sale.getName(),
                productNames,
                sale.getDiscountPercentage(),
                sale.getStartTime(),
                sale.getEndTime(),
                sale.getTotalStock()
        );
    }
}
