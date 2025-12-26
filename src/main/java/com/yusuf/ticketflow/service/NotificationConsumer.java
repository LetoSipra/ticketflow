package com.yusuf.ticketflow.service;

import com.yusuf.ticketflow.dto.TicketConfirmationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = "ticket-notifications", groupId = "ticket-group")
    public void handleTicketConfirmation(TicketConfirmationEvent event) {
        log.info("📧 Email Service received event: {}", event);

        try {
            Thread.sleep(1000);
            log.info("✅ Email successfully sent to {} for Ticket ID: {}", event.getEmail(), event.getTicketId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Error sending email", e);
        }
    }
}