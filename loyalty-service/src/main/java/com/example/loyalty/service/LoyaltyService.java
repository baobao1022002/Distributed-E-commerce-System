package com.example.loyalty.service;

import com.example.loyalty.entity.LoyaltyAccount;
import com.example.loyalty.entity.PointTransaction;
import com.example.loyalty.entity.Referral;
import com.example.loyalty.event.LoyaltyPointsEarnedEvent;
import com.example.loyalty.event.LoyaltyTierUpgradedEvent;
import com.example.loyalty.repository.LoyaltyAccountRepository;
import com.example.loyalty.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
//    private final ReferralRepository referralRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoyaltyAccount getOrCreateLoyaltyAccount(String userId) {
        return getOrCreateLoyaltyAccount(userId, null);
    }

    public LoyaltyAccount getOrCreateLoyaltyAccount(String userId, String userEmail) {
        Optional<LoyaltyAccount> existing = loyaltyAccountRepository.findByUserId(userId);
        if (existing.isPresent()) {
            LoyaltyAccount account = existing.get();
            // Update email if we now have it and didn't before
            if (userEmail != null && account.getUserEmail() == null) {
                account.setUserEmail(userEmail);
                loyaltyAccountRepository.save(account);
            }
            return account;
        }

        LoyaltyAccount account = LoyaltyAccount.builder()
                .userId(userId)
                .userEmail(userEmail)
                .currentPoints(0L)
                .lifetimePoints(0L)
                .lifetimeSpend(0L)
                .build();

        return loyaltyAccountRepository.save(account);
    }

    public void earnPoints(String userId, Long points, PointTransaction.PointSource source, String orderId) {
        log.info("Earning {} points for user: {}, source: {}", points, userId, source);

        LoyaltyAccount account = getOrCreateLoyaltyAccount(userId);
        long finalPoints = applyTierBonus(points, account.getTier());
        account.setCurrentPoints(account.getCurrentPoints() + finalPoints);
        account.setLifetimePoints(account.getLifetimePoints() + finalPoints);
        loyaltyAccountRepository.save(account);

        PointTransaction transaction = PointTransaction.builder()
                .userId(userId)
                .points(finalPoints)
                .type(PointTransaction.PointType.EARN)
                .source(source)
                .orderId(orderId)
                .build();

        pointTransactionRepository.save(transaction);
        kafkaTemplate.send("loyalty-points-earned", new LoyaltyPointsEarnedEvent(
                userId, account.getUserEmail(), finalPoints, source.name(), orderId, LocalDateTime.now()
        ));

        // Check for tier upgrade
        checkAndUpgradeTier(account);
    }

    public void earnPointsFromOrder(String userId, String orderId, java.math.BigDecimal finalAmount, String userEmail) {
        LoyaltyAccount account = getOrCreateLoyaltyAccount(userId, userEmail);
        long basePoints = finalAmount.setScale(0, java.math.RoundingMode.DOWN).longValue();
        account.setLifetimeSpend(account.getLifetimeSpend() + basePoints);
        loyaltyAccountRepository.save(account);
        earnPoints(userId, basePoints, PointTransaction.PointSource.ORDER_COMPLETION, orderId);
    }

    public void redeemPoints(String userId, Long points) {
        log.info("Redeeming {} points for user: {}", points, userId);

        Optional<LoyaltyAccount> optional = loyaltyAccountRepository.findByUserId(userId);
        if (optional.isEmpty()) {
            throw new RuntimeException("Loyalty account not found for user: " + userId);
        }

        LoyaltyAccount account = optional.get();
        if (account.getCurrentPoints() < points) {
            throw new RuntimeException("Insufficient points");
        }

        account.setCurrentPoints(account.getCurrentPoints() - points);
        loyaltyAccountRepository.save(account);

        PointTransaction transaction = PointTransaction.builder()
                .userId(userId)
                .points(points)
                .type(PointTransaction.PointType.REDEEM)
                .source(PointTransaction.PointSource.ORDER_COMPLETION)
                .build();

        pointTransactionRepository.save(transaction);
    }

    private void checkAndUpgradeTier(LoyaltyAccount account) {
        LoyaltyAccount.LoyaltyTier oldTier = account.getTier();
        LoyaltyAccount.LoyaltyTier newTier = calculateTier(account.getLifetimeSpend());

        if (!oldTier.equals(newTier)) {
            account.setTier(newTier);
            loyaltyAccountRepository.save(account);

            log.info("User {} upgraded from {} to {}", account.getUserId(), oldTier, newTier);

            kafkaTemplate.send("loyalty-tier-upgraded", new LoyaltyTierUpgradedEvent(
                    account.getUserId(), account.getUserEmail(), oldTier.name(), newTier.name(), LocalDateTime.now()
            ));
        }
    }

    private LoyaltyAccount.LoyaltyTier calculateTier(Long lifetimeSpend) {
        if (lifetimeSpend >= 10000) {
            return LoyaltyAccount.LoyaltyTier.PLATINUM;
        } else if (lifetimeSpend >= 5000) {
            return LoyaltyAccount.LoyaltyTier.GOLD;
        } else if (lifetimeSpend >= 1000) {
            return LoyaltyAccount.LoyaltyTier.SILVER;
        } else {
            return LoyaltyAccount.LoyaltyTier.BRONZE;
        }
    }

    public long getCurrentPoints(String userId) {
        Optional<LoyaltyAccount> account = loyaltyAccountRepository.findByUserId(userId);
        return account.map(LoyaltyAccount::getCurrentPoints).orElse(0L);
    }

    public LoyaltyAccount.LoyaltyTier getTier(String userId) {
        return loyaltyAccountRepository.findByUserId(userId)
                .map(LoyaltyAccount::getTier)
                .orElse(LoyaltyAccount.LoyaltyTier.BRONZE);
    }

    public List<PointTransaction> getHistory(String userId) {
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
//
//    public String generateReferralCode(String referrerId) {
//        String code = ("REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)).toUpperCase();
//        Referral referral = Referral.builder()
//                .referrerId(referrerId)
//                .code(code)
//                .referrerRewardPoints(200L)
//                .refereeRewardPoints(100L)
//                .build();
//        referralRepository.save(referral);
//        return code;
//    }
//
//    public boolean validateReferralCode(String code) {
//        return referralRepository.findByCode(code)
//                .filter(referral -> referral.getStatus() == Referral.ReferralStatus.PENDING)
//                .filter(referral -> referral.getExpiresAt().isAfter(LocalDateTime.now()))
//                .isPresent();
//    }

    private long applyTierBonus(Long points, LoyaltyAccount.LoyaltyTier tier) {
        return switch (tier) {
            case SILVER -> Math.round(points * 1.05);
            case GOLD -> Math.round(points * 1.10);
            case PLATINUM -> Math.round(points * 1.15);
            case BRONZE -> points;
        };
    }
}
