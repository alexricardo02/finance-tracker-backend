package com.example.config;

import com.example.models.Category;
import com.example.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CategorySeeder implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    
    @Override
    public void run(String... args) throws Exception {
        // Check whether global categories already exist (user_id IS NULL)
        List<Category> globalCategories = categoryRepository.findByUserIsNull();
        
        if (globalCategories.isEmpty()) {
            System.out.println("Seeding default categories into the database...");
            
            List<Category> defaults = Arrays.asList(
                new Category("Salary", "income", null),
                new Category("Freelance", "income", null),
                new Category("Gift", "income", null),
                new Category("Investment", "income", null),
                new Category("Other", "income", null),
                new Category("Food", "expense", null),
                new Category("Rent", "expense", null),
                new Category("Transport", "expense", null),
                new Category("Entertainment", "expense", null),
                new Category("Health", "expense", null),
                new Category("Bills", "expense", null),
                new Category("Shopping", "expense", null)
            );
            
            categoryRepository.saveAll(defaults);
            System.out.println("Default categories created successfully.");
        }
    }
}