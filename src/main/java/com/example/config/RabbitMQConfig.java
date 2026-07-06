package com.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "finance.events";
    public static final String CURRENCY_QUEUE = "finance.currency-changed.queue";
    public static final String CURRENCY_ROUTING_KEY = "currency.changed";

    // Dead Letter Queue: si el consumer falla 3 veces, el mensaje cae aquí en vez de perderse
    public static final String DLQ = "finance.currency-changed.dlq";

    @Bean
    public TopicExchange financeExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue currencyChangedQueue() {
        return QueueBuilder.durable(CURRENCY_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue currencyChangedDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding currencyChangedBinding(Queue currencyChangedQueue, TopicExchange financeExchange) {
        return BindingBuilder.bind(currencyChangedQueue).to(financeExchange).with(CURRENCY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}