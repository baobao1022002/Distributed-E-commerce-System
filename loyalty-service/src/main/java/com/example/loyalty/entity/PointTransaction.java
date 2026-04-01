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
@Table(name = "point_transactions", indexes = {
    @Index(name = "idx_point_user_id", columnList = "userId"),
    @Index(name = "idx_point_type", columnList = "type"),
    @Index(name = "idx_point_order_id", columnList = "orderId")
})
public class PointTransaction {
    @Id
    @UuidGenerator
    @GeneratedValue(generator = "uuid")
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type; // EARN, REDEEM, EXPIRE, ADJUST

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointSource source; // ORDER_COMPLETION, FIRST_PURCHASE, BIRTHDAY, REVIEW, REFERRAL, LOYALTY_TIER

    private String orderId;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt; // Only set for earned points

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null && PointType.EARN.equals(type)) {
            expiresAt = LocalDateTime.now().plusYears(1); // 12 months expiration
        }
    }

    public enum PointType {
        EARN,
        REDEEM,
        EXPIRE,
        ADJUST
    }

    public enum PointSource {
        ORDER_COMPLETION,
        FIRST_PURCHASE,
        BIRTHDAY,
        REVIEW,
        REFERRAL,
        LOYALTY_TIER
    }
}
