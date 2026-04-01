package com.example.loyalty.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPointsEarnedEvent {
    private String userId;
    private String userEmail;
    private Long points;
    private String source;
    private String orderId;
    private LocalDateTime earnedAt;
}
