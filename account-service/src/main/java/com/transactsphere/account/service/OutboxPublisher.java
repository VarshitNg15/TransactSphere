package com.transactsphere.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactsphere.account.dto.AccountUpdatedEvent;
import com.transactsphere.account.model.OutboxEvent;
import com.transactsphere.account.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.polling.interval:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pendingEvents) {
            try {
                AccountUpdatedEvent updatedEvent = objectMapper.readValue(event.getPayload(), AccountUpdatedEvent.class);

                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), updatedEvent).get(); 

                event.setStatus("SENT");
                outboxEventRepository.save(event);

                log.info("Successfully published outbox event {} for account {}", event.getId(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                break; 
            }
        }
    }
}
