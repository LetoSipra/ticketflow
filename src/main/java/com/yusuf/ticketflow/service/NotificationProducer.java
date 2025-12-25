package com.yusuf.ticketflow.service;

import com.yusuf.ticketflow.dto.TicketConfirmationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    // Manual Logger definition
    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "ticket-notifications";

    public NotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTicketConfirmation(TicketConfirmationEvent event) {
        log.info("Publishing ticket confirmation event for: {}", event.getEmail());
        kafkaTemplate.send(TOPIC, event);
    }
}