package com.example.loyalty.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "loyalty_accounts", indexes = {
    @Index(name = "idx_loyalty_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_loyalty_tier", columnList = "tier")
})
public class LoyaltyAccount {
    @Id
    @UuidGenerator
    @GeneratedValue(generator = "uuid")
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String userId;

    @Column(length = 255)
    private String userEmail;

    @Column(nullable = false)
    private Long currentPoints;

    @Column(nullable = false)
    private Long lifetimePoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyTier tier;

    @Column(nullable = false)
    private Long lifetimeSpend;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        currentPoints = 0L;
        lifetimePoints = 0L;
        tier = LoyaltyTier.BRONZE;
        lifetimeSpend = 0L;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum LoyaltyTier {
        BRONZE,     // 0-999
        SILVER,     // 1000-4999
        GOLD,       // 5000-9999
        PLATINUM    // 10000+
    }
}

