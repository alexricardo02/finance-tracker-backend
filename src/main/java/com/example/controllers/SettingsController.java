package com.example.controllers;

import com.example.dataTransferObjects.UserSettingsDTO;
import com.example.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public UserSettingsDTO getSettings(Principal principal) {
        return settingsService.getSettings(principal.getName());
    }

    @PutMapping("/currency")
    public UserSettingsDTO updatePrimaryCurrency(Principal principal, @Valid @RequestBody UserSettingsDTO dto) {
        return settingsService.updatePrimaryCurrency(principal.getName(), dto.getPrimaryCurrency());
    }
}