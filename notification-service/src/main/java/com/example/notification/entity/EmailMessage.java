package com.example.notification.entity;

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
@Table(name = "email_messages", indexes = {
    @Index(name = "idx_email_user_id", columnList = "userId"),
    @Index(name = "idx_email_status", columnList = "status"),
    @Index(name = "idx_email_type", columnList = "type")
})
public class EmailMessage {
    @Id
    @UuidGenerator
    @GeneratedValue(generator = "uuid")
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 255)
    private String toEmail;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType type; // ORDER_CONFIRMATION, FLASH_SALE_ALERT, WELCOME, PASSWORD_RESET, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status; // PENDING, SENT, FAILED, BOUNCED

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private Integer maxRetries;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        retryCount = 0;
        maxRetries = 3;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EmailType {
        ORDER_CONFIRMATION,
        PAYMENT_RECEIVED,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED,
        FLASH_SALE_ALERT,
        FLASH_SALE_REMINDER,
        BACK_IN_STOCK,
        WELCOME,
        PASSWORD_RESET,
        LOGIN_ALERT,
        LOYALTY_POINTS_EARNED,
        LOYALTY_TIER_UPGRADED
    }

    public enum EmailStatus {
        PENDING,
        SENT,
        FAILED,
        BOUNCED
    }
}
