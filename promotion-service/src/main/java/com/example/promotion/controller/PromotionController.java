package com.example.promotion.controller;

import com.example.promotion.dto.CouponDTO;
import com.example.promotion.dto.FlashSaleDTO;
import com.example.promotion.entity.Coupon;
import com.example.promotion.entity.FlashSale;
import com.example.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping("/flash-sales")
    public ResponseEntity<FlashSale> createFlashSale(@RequestBody FlashSaleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createFlashSale(dto));
    }

    @GetMapping("/flash-sales/active")
    public ResponseEntity<List<FlashSale>> getActiveFlashSales() {
        return ResponseEntity.ok(promotionService.getActiveFlashSales());
    }

    @PostMapping("/flash-sales/{id}/participate")
    public ResponseEntity<String> participateFlashSale(@PathVariable String id,
                                                       @RequestParam(defaultValue = "1") int quantity) {
        boolean success = promotionService.decrementFlashSaleStock(id, quantity);
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Insufficient flash sale stock");
        }
        return ResponseEntity.ok("Participated successfully");
    }

    @PostMapping("/coupons")
    public ResponseEntity<Coupon> createCoupon(@RequestBody CouponDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createCoupon(dto));
    }

    @GetMapping("/coupons/{code}/validate")
    public ResponseEntity<Coupon> validateCoupon(@PathVariable String code) {
        Optional<Coupon> coupon = promotionService.validateCoupon(code);
        return coupon.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/coupons/{code}/apply")
    public ResponseEntity<Coupon> applyCoupon(@PathVariable String code,
                                              @RequestParam String orderId,
                                              @RequestParam(defaultValue = "0") BigDecimal orderAmount) {
        Optional<Coupon> coupon = promotionService.applyCoupon(code, orderId, orderAmount);
        return coupon.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/coupons/{code}/release")
    public ResponseEntity<Void> releaseCoupon(@PathVariable String code,
                                              @RequestParam String orderId) {
        promotionService.releaseCoupon(code, orderId);
        return ResponseEntity.ok().build();
    }
}
