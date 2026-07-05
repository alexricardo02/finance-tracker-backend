package com.example.controllers;


import com.example.dataTransferObjects.ExchangeRateResponseDTO;
import com.example.service.ExchangeRateService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/usd-ars")
    public ExchangeRateResponseDTO getUsdArsOficial() {
        return exchangeRateService.getOficialRate();
    }
}