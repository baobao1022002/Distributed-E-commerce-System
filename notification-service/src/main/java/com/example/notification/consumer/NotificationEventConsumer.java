package com.example.notification.consumer;

import com.example.notification.client.UserClient;
import com.example.notification.client.dto.UserInforDto;
import com.example.notification.event.*;
import com.example.notification.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final UserClient userClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order-completed", groupId = "notification-service")
    public void consumeOrderCompleted(String orderCompletedEventString) throws JsonProcessingException {
        OrderCompletedEvent orderCompletedEvent = objectMapper.readValue(orderCompletedEventString, OrderCompletedEvent.class);

        if (orderCompletedEvent.getCustomerEmail() == null) {
            log.warn("order-completed orderCompletedEvent missing customerEmail for order={}, skipping notification", orderCompletedEvent.getOrderId());
            return;
        }
        emailService.sendEmail(
                orderCompletedEvent.getCustomerEmail(),
                "Order Completed",
                "Your order " + orderCompletedEvent.getOrderId() + " has been completed. Thank you for your purchase!",
                "ORDER_CONFIRMATION",
                orderCompletedEvent.getCustomerId()
        );
    }

    @KafkaListener(topics = "order-failed", groupId = "notification-service")
    public void consumeOrderFailed(String orderFailedEventString) throws JsonProcessingException {
        OrderFailedEvent orderFailedEvent = objectMapper.readValue(orderFailedEventString, OrderFailedEvent.class);

        if (orderFailedEvent.getCustomerEmail() == null) {
            log.warn("order-failed orderFailedEvent missing customerEmail for order={}, skipping notification", orderFailedEvent.getOrderId());
            return;
        }
        emailService.sendEmail(
                orderFailedEvent.getCustomerEmail(),
                "Order Failed",
                "We're sorry, your order " + orderFailedEvent.getOrderId() + " could not be processed. Reason: " + orderFailedEvent.getFailureReason(),
                "CANCELLED",
                orderFailedEvent.getCustomerId()
        );
    }

    @KafkaListener(topics = "flashsale-created", groupId = "notification-service")
    public void consumeFlashSaleCreated(String flashSaleCreatedEventString) throws JsonProcessingException {
        FlashSaleCreatedEvent flashSaleCreatedEvent = objectMapper.readValue(flashSaleCreatedEventString, FlashSaleCreatedEvent.class);
        log.info("Received flashsale-created {}", flashSaleCreatedEvent.getFlashSaleName());
        String emailContent = buildFlashSaleEmail(flashSaleCreatedEvent);
        userClient.streamAllUsers(1000)
                .map(user -> EmailNotificationEvent.builder()
                        .email(user.getEmail())
                        .subject("🔥 Upcoming Flash Sale – Don't Miss It!")
                        .content(emailContent)
                        .type("FLASH_SALE_REMINDER")
                        .userId(user.getCustomerId())
                        .build())
                .subscribe(emailEvent ->
                        kafkaTemplate.send("email-notifications", emailEvent)
                );
    }

    @KafkaListener(topics = "loyalty-points-earned", groupId = "notification-service")
    public void consumeLoyaltyPoints(String loyaltyPointsEarnedEventString) throws JsonProcessingException {
        LoyaltyPointsEarnedEvent loyaltyPointsEarnedEven = objectMapper.readValue(loyaltyPointsEarnedEventString, LoyaltyPointsEarnedEvent.class);

        if (loyaltyPointsEarnedEven.getUserEmail() == null) {
            log.warn("loyalty-points-earned loyaltyPointsEarnedEven missing userEmail for user={}, skipping notification", loyaltyPointsEarnedEven.getUserId());
            return;
        }
        emailService.sendEmail(
                loyaltyPointsEarnedEven.getUserEmail(),
                "Loyalty Points Earned",
                "Great news! You just earned " + loyaltyPointsEarnedEven.getPoints() + " loyalty points.",
                "LOYALTY_POINTS_EARNED",
                loyaltyPointsEarnedEven.getUserId()
        );
    }

    @KafkaListener(topics = "loyalty-tier-upgraded", groupId = "notification-service")
    public void consumeLoyaltyTier(String loyaltyTierUpgradedEventString) throws JsonProcessingException {
        LoyaltyTierUpgradedEvent loyaltyTierUpgradedEvent = objectMapper.readValue(loyaltyTierUpgradedEventString, LoyaltyTierUpgradedEvent.class);

        if (loyaltyTierUpgradedEvent.getUserEmail() == null) {
            log.warn("loyalty-tier-upgraded loyaltyTierUpgradedEvent missing userEmail for user={}, skipping notification", loyaltyTierUpgradedEvent.getUserId());
            return;
        }
        emailService.sendEmail(
                loyaltyTierUpgradedEvent.getUserEmail(),
                "Loyalty Tier Upgraded!",
                "Congratulations! Your loyalty tier has been upgraded to " + loyaltyTierUpgradedEvent.getNewTier() + ". Enjoy your new benefits!",
                "LOYALTY_TIER_UPGRADED",
                loyaltyTierUpgradedEvent.getUserId()
        );
    }


    private String buildFlashSaleEmail(FlashSaleCreatedEvent event) {
        return String.format("""
            Hello,

            🎉 A new Flash Sale is coming soon on our store!

            Flash Sale Details:
            • Flash Sale: %s
            • Discount: %s%%
            • Start Time: %s
            • End Time: %s
            • Available Stock: %d items

            Selected Products:
            %s

            ⏰ Be ready when the sale starts — quantities are limited!

            👉 Visit our store and grab your favorite products at the best price.

            Best regards,
            E-Commerce Team
            """,
                event.getFlashSaleName(),
                event.getDiscount(),
                event.getStartTime(),
                event.getEndTime(),
                event.getStock(),
                String.join(", ", event.getListProduct())
        );
    }

}
