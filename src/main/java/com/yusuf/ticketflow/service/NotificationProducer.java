package com.yusuf.ticketflow.service;

import com.yusuf.ticketflow.dto.TicketConfirmationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);
    private static final String TOPIC = "ticket-notifications";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean kafkaEnabled;

    public NotificationProducer(
            ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider,
            @Value("${notification.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        this.kafkaEnabled = kafkaEnabled && this.kafkaTemplate != null;
    }

    public void sendTicketConfirmation(TicketConfirmationEvent event) {
        if (!kafkaEnabled) {
            log.info("✅ Kafka disabled - Simulating notification sent to: {}", event.getEmail());
            return;
        }
        try {
            log.info("Publishing ticket confirmation event for: {}", event.getEmail());
            kafkaTemplate.send(TOPIC, event);
        } catch (Exception e) {
            log.warn("⚠️ Kafka unavailable - Simulating success notification for: {}", event.getEmail());
            log.debug("Kafka error details: {}", e.getMessage());
        }
    }
}