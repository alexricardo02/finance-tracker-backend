package com.example.repository;

import com.example.models.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    // Query so the job only finds events that have not yet been sent to RabbitMQ
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}