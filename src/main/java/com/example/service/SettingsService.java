package com.example.service;

import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.dataTransferObjects.UserSettingsDTO;

import com.example.events.PrimaryCurrencyChangedListener;
import com.example.events.PrimaryCurrencyChangedEvent;
import com.example.models.OutboxEvent;
import com.example.models.SupportedCurrency;
import com.example.models.User;
import com.example.repository.OutboxEventRepository;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Service
public class SettingsService {

    @Autowired private UserRepository userRepository;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private PrimaryCurrencyChangedListener currencyRecalculationHandler;


    public UserSettingsDTO getSettings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserSettingsDTO(user.getPrimaryCurrency());
    }

    @Transactional
    public UserSettingsDTO updatePrimaryCurrency(String username, String newCurrency) {
        // Also enforced on UserSettingsDTO; checked again because this method is the last stop before
        // the value is stored and used to build FX lookups.
        if (!SupportedCurrency.isSupported(newCurrency)) {
            throw new BadRequestException("Unsupported currency");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (newCurrency.equals(user.getPrimaryCurrency())) {
            return new UserSettingsDTO(newCurrency);
        }

        user.setPrimaryCurrency(newCurrency);
        userRepository.save(user);
        
        PrimaryCurrencyChangedEvent event = new PrimaryCurrencyChangedEvent(user.getUserId(), username, newCurrency);
        currencyRecalculationHandler.recalculate(event);
        
        String jsonPayload = String.format("{\"userId\": %d, \"username\": \"%s\", \"newCurrency\": \"%s\"}", 
                user.getUserId(), username, newCurrency);
		OutboxEvent outboxEvent = new OutboxEvent("PRIMARY_CURRENCY_CHANGED", jsonPayload);
		outboxEventRepository.save(outboxEvent);
		
		return new UserSettingsDTO(newCurrency);

    }
}