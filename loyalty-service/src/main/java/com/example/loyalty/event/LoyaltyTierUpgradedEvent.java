package com.example.loyalty.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTierUpgradedEvent {
    private String userId;
    private String userEmail;
    private String oldTier;
    private String newTier;
    private LocalDateTime upgradedAt;
}
