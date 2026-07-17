package com.transactsphere.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactsphere.transaction.dto.TransactionEvent;
import com.transactsphere.transaction.model.OutboxEvent;
import com.transactsphere.transaction.repository.OutboxEventRepository;
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
                // Deserialize payload back to TransactionEvent for sending
                TransactionEvent transactionEvent = objectMapper.readValue(event.getPayload(), TransactionEvent.class);

                // Publish to Kafka
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), transactionEvent).get(); // .get() to make it synchronous/wait for ack

                // Mark as SENT
                event.setStatus("SENT");
                outboxEventRepository.save(event);

                log.info("Successfully published outbox event {} for transaction {}", event.getId(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                // Break to retry this and other events next polling cycle
                break; 
            }
        }
    }
}
