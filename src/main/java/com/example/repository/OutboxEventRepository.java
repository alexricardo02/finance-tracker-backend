package com.example.repository;

import com.example.models.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    // Consulta para que el Job busque solo los eventos que aún no se han enviado a RabbitMQ
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}