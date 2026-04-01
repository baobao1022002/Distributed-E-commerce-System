package com.example.notification.scheduler;

import com.example.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailScheduler {
    private final EmailService emailService;

    @Scheduled(fixedDelay = 15000)
    public void processPendingEmails() {
        long pendingCount = emailService.getPendingEmailCount();
        if (pendingCount > 0) {
            log.info("Found {} pending emails. Processing now.", pendingCount);
            emailService.processPendingEmails();
        }
    }
}
