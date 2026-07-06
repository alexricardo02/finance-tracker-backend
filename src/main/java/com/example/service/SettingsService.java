package com.example.service;

import com.example.dataTransferObjects.UserSettingsDTO;
import com.example.events.PrimaryCurrencyChangedEvent;
import com.example.models.User;
import com.example.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class SettingsService {

    // WHY: whitelist avoids storing garbage currency codes that would break FX lookups
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "ARS", "JPY");

    @Autowired private UserRepository userRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;

    public UserSettingsDTO getSettings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return new UserSettingsDTO(user.getPrimaryCurrency());
    }

    @Transactional
    public UserSettingsDTO updatePrimaryCurrency(String username, String newCurrency) {
        if (newCurrency == null || !SUPPORTED_CURRENCIES.contains(newCurrency)) {
            throw new IllegalArgumentException("Unsupported currency: " + newCurrency);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (newCurrency.equals(user.getPrimaryCurrency())) {
            return new UserSettingsDTO(newCurrency);
        }

        user.setPrimaryCurrency(newCurrency);
        userRepository.save(user);

        eventPublisher.publishEvent(
                new PrimaryCurrencyChangedEvent(user.getUserId(), username, newCurrency));

        return new UserSettingsDTO(newCurrency);
    }
}