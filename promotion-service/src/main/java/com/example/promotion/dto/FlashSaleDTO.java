package com.example.promotion.dto;

import com.example.promotion.entity.FlashSale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlashSaleDTO {
    private String name;
    private String description;
    private FlashSale.FlashSaleStatus status;
    private List<String> productIds;
    private BigDecimal discountPercentage;
    private Integer totalStock;
    private Integer remainingStock;
    private Integer soldQuantity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime premiumStartTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
