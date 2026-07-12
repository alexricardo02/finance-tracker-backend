package com.example.service;

import com.example.dataTransferObjects.ImportResultDTO;
import com.example.dataTransferObjects.TransactionImportRowDTO;
import com.example.models.*;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ImportService {

    private static final int BATCH_SIZE = 200;

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private IncomeRepository incomeRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ExchangeRateService exchangeRateService;
    @Autowired private CacheService cacheService;

    @Transactional
    public ImportResultDTO importTransactions(List<TransactionImportRowDTO> rows, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Map<String, Category> categoryCache = new HashMap<>();
        List<Income> incomesToSave = new ArrayList<>();
        List<Expense> expensesToSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int skipped = 0;

        for (int i = 0; i < rows.size(); i++) {
            TransactionImportRowDTO row = rows.get(i);
            try {
                Category category = resolveCategory(row.getCategoryName(), row.getKind(), user, categoryCache);
                double rate = exchangeRateService.getConversionRate(row.getCurrency(), user.getPrimaryCurrency(), row.getDate());

                if ("income".equals(row.getKind())) {
                    Income income = new Income();
                    income.setAmount(row.getAmount());
                    income.setCurrency(row.getCurrency());
                    income.setDate(row.getDate());
                    income.setDescription(row.getDescription());
                    income.setCategory(category);
                    income.setPaymentMethod(row.getPaymentMethod());
                    income.setUser(user);
                    income.setAmountPrimaryCurrency(row.getAmount() * rate);
                    incomesToSave.add(income);
                } else {
                    Expense expense = new Expense();
                    expense.setExpenseAmount(row.getAmount());
                    expense.setCurrency(row.getCurrency());
                    expense.setExpenseDate(row.getDate());
                    expense.setExpenseDescription(row.getDescription());
                    expense.setCategory(category);
                    expense.setPaymentMethod(row.getPaymentMethod());
                    expense.setUser(user);
                    expense.setAmountPrimaryCurrency(row.getAmount() * rate);
                    expensesToSave.add(expense);
                }
            } catch (Exception e) {
                skipped++;
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }

        saveInBatches(incomeRepository, incomesToSave);
        saveInBatches(expenseRepository, expensesToSave);
        cacheService.evictUserFinancialCache(username);

        return new ImportResultDTO(incomesToSave.size() + expensesToSave.size(), skipped, errors);
    }

    private Category resolveCategory(String name, String kind, User user, Map<String, Category> cache) {
        String finalName = (name == null || name.isBlank()) ? "Uncategorized" : name;
        String type = "income".equals(kind) ? "income" : "expense";
        String cacheKey = user.getUserId() + "_" + type + "_" + finalName.toLowerCase();

        return cache.computeIfAbsent(cacheKey, k -> categoryRepository
                .findByUserUserIdOrUserIsNull(user.getUserId()).stream()
                .filter(c -> c.getType().equalsIgnoreCase(type) && c.getName().equalsIgnoreCase(finalName))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new Category(finalName, type, user))));
    }

    private <T> void saveInBatches(JpaRepository<T, Integer> repo, List<T> entities) {
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            repo.saveAll(entities.subList(i, Math.min(i + BATCH_SIZE, entities.size())));
        }
    }
}