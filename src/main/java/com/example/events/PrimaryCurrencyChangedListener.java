package com.example.events;

import com.example.config.RabbitMQConfig;
import com.example.models.Expense;
import com.example.models.Income;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.service.CacheService;
import com.example.service.ExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PrimaryCurrencyChangedListener {

    private static final Logger log = LoggerFactory.getLogger(PrimaryCurrencyChangedListener.class);

    @Autowired private IncomeRepository incomeRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ExchangeRateService exchangeRateService;
    @Autowired private CacheService cacheService;

    // WHY: recalculation runs in the background so the settings request returns
    // instantly. Each transaction is reconverted using the FX rate of ITS OWN
    // date, not today's rate, so historical totals stay accurate.
    @RabbitListener(queues = RabbitMQConfig.CURRENCY_QUEUE)
    @Transactional
    public void onPrimaryCurrencyChanged(PrimaryCurrencyChangedEvent event) {
    	recalculate(event);
    }
    
    @Transactional
    public void recalculate(PrimaryCurrencyChangedEvent event) {
        String newCurrency = event.getNewCurrency();
        log.info("Recalculating amounts to {} for user {}", newCurrency, event.getUsername());

        incomeRepository.findByUserUserId(event.getUserId(), Pageable.unpaged() )
                .forEach(income -> {
                    double rate = exchangeRateService.getConversionRate(
                            income.getCurrency(), newCurrency, income.getDate());
                    income.setAmountPrimaryCurrency(income.getAmount() * rate);
                    incomeRepository.save(income);
                });

        expenseRepository.findByUserUserId(event.getUserId(), Pageable.unpaged())
                .forEach(expense -> {
                    double rate = exchangeRateService.getConversionRate(
                            expense.getCurrency(), newCurrency, expense.getExpenseDate());
                    expense.setAmountPrimaryCurrency(expense.getExpenseAmount() * rate);
                    expenseRepository.save(expense);
                });

        cacheService.evictUserFinancialCache(event.getUsername());
        log.info("Recalculation finished for user {}", event.getUsername());
     }
 }
