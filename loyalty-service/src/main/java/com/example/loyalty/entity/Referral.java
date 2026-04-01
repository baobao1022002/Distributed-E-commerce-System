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
@Table(name = "referrals", indexes = {
    @Index(name = "idx_referral_code", columnList = "code", unique = true),
    @Index(name = "idx_referral_status", columnList = "status"),
    @Index(name = "idx_referral_referrer", columnList = "referrerId"),
    @Index(name = "idx_referral_referee", columnList = "refereeId")
})
public class Referral {
    @Id
    @UuidGenerator
    @GeneratedValue(generator = "uuid")
    private String id;

    @Column(nullable = false)
    private String referrerId;

    private String refereeId;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status; // PENDING, COMPLETED, EXPIRED

    @Column(nullable = false)
    private Long referrerRewardPoints;

    @Column(nullable = false)
    private Long refereeRewardPoints;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt; // Referral code expires after 6 months

    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMonths(6);
        status = ReferralStatus.PENDING;
    }

    public enum ReferralStatus {
        PENDING,
        COMPLETED,
        EXPIRED
    }
}
