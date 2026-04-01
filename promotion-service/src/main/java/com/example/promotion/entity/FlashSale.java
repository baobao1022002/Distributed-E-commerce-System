package com.example.promotion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "flash_sales", indexes = {
    @Index(name = "idx_flashsale_active", columnList = "status"),
    @Index(name = "idx_flashsale_dates", columnList = "startTime, endTime")
})
public class FlashSale {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlashSaleStatus status;

    @Column(nullable = false, length = 500)
    private List<String> productIds; // Comma-separated list of product IDs

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountPercentage;

    @Column(nullable = false)
    private Integer totalStock;

    @Column(nullable = false)
    private Integer remainingStock;

    @Column(nullable = false)
    private Integer soldQuantity;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private LocalDateTime premiumStartTime; // Early access for premium members

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        soldQuantity = 0;
        remainingStock = totalStock;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum FlashSaleStatus {
        PENDING,
        ACTIVE,
        PAUSED,
        ENDED,
        CANCELLED
    }
}
