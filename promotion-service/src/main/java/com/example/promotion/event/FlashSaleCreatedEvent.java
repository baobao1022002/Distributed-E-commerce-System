package com.example.promotion.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleCreatedEvent {
    private String flashSaleName;
    private List<String> listProduct;
    private BigDecimal discount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer stock;
}
