package com.example.notification.service;

import com.example.notification.entity.EmailMessage;
import com.example.notification.repository.EmailMessageRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Maps EmailType enum name → Thymeleaf template filename (without .html). */
    private static final Map<String, String> TEMPLATE_MAP = Map.of(
            "ORDER_CONFIRMATION",    "order-confirmation",
            "CANCELLED",             "order-cancelled",
            "LOYALTY_POINTS_EARNED", "loyalty-points-earned",
            "LOYALTY_TIER_UPGRADED", "loyalty-tier-upgraded"
    );

    private final EmailMessageRepository emailMessageRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendEmail(String toEmail, String subject, String content, String type, String userId) {
        if (!isValidEmail(toEmail)) {
            log.warn("Skipping email — invalid address: '{}', type: {}, userId: {}", toEmail, type, userId);
            return;
        }

        log.info("Sending email to: {}, type: {}, userId: {}", toEmail, type, userId);

        EmailMessage email = EmailMessage.builder()
                .userId(userId)
                .toEmail(toEmail)
                .subject(subject)
                .content(content)
                .type(EmailMessage.EmailType.valueOf(type))
                .status(EmailMessage.EmailStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .build();

        try {
            doSend(email);
            email.setStatus(EmailMessage.EmailStatus.SENT);
            email.setSentAt(LocalDateTime.now());
            log.info("Email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to: {}, will retry via scheduler", toEmail, e);
            email.setStatus(EmailMessage.EmailStatus.PENDING);
            email.setFailureReason(e.getMessage());
        }

        emailMessageRepository.save(email);
    }

    /** Retry loop — only picks up PENDING (first-attempt failures). */
    public void processPendingEmails() {
        List<EmailMessage> pendingEmails = emailMessageRepository.findByStatus(EmailMessage.EmailStatus.PENDING);
        if (pendingEmails.isEmpty()) return;

        log.info("Retrying {} pending email(s)", pendingEmails.size());
        for (EmailMessage email : pendingEmails) {
            try {
                doSend(email);
                email.setStatus(EmailMessage.EmailStatus.SENT);
                email.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Retry failed for email id={}, to={}", email.getId(), email.getToEmail(), e);
                email.setRetryCount(email.getRetryCount() + 1);
                if (email.getRetryCount() >= email.getMaxRetries()) {
                    email.setStatus(EmailMessage.EmailStatus.FAILED);
                    email.setFailureReason(e.getMessage());
                    log.warn("Email permanently failed after {} retries: id={}", email.getMaxRetries(), email.getId());
                }
            }
            emailMessageRepository.save(email);
        }
    }

    @Async("notificationExecutor")
    public void sendBatchAsync(List<String> toEmails, String subject, String content, String type) {
        for (String email : toEmails) {
            sendEmail(email, subject, content, type, email);
        }
    }

    public long getPendingEmailCount() {
        return emailMessageRepository.findByStatus(EmailMessage.EmailStatus.PENDING).size();
    }

    private void doSend(EmailMessage email) throws Exception {
        String templateName = TEMPLATE_MAP.getOrDefault(email.getType().name(), "email-base");

        Context ctx = new Context();
        ctx.setVariable("subject", email.getSubject());
        ctx.setVariable("message", email.getContent());

        String htmlBody = templateEngine.process(templateName, ctx);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(email.getToEmail());
        helper.setSubject(email.getSubject());
        helper.setText(htmlBody, true);  // true = isHtml

        mailSender.send(mimeMessage);
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
