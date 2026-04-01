package com.example.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTierUpgradedEvent {
    private String userId;
    private String userEmail;
    private String newTier;
}
