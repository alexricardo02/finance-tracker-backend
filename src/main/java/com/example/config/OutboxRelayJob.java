package com.example.config;

import com.example.models.OutboxEvent;
import com.example.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class OutboxRelayJob {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pendingEvents) {
            try {
                rabbitTemplate.convertAndSend("tu_exchange_name", "tu_routing_key", event.getPayload());
                
                event.setProcessed(true);
                outboxEventRepository.save(event);
                
            } catch (Exception e) {
                System.err.println("Error sending event to RabbitMQ; it will be retried: " + e.getMessage());
            }
        }
    }
}