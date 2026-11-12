package com.example.controllers;

import com.example.exceptions.ConflictException;
import com.example.exceptions.ForbiddenResourceException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.dataTransferObjects.CategoryDTO;
import com.example.models.Category;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private UserRepository userRepository;

    // Utility method to obtain the username from the JWT token
    private String getAuthenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // 1. Retrieve all categories for the current user
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getUserCategories() {
        User user = userRepository.findByUsername(getAuthenticatedUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CategoryDTO> categories = categoryRepository.findByUserUserIdOrUserIsNull(user.getUserId())
                .stream()
                .map(c -> new CategoryDTO(c.getCategoryId(), c.getName(), c.getType()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }

    // 2. Create a new category
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO categoryDTO) {
        User user = userRepository.findByUsername(getAuthenticatedUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = new Category(categoryDTO.getName(), categoryDTO.getType(), user);
        Category savedCategory = categoryRepository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CategoryDTO(savedCategory.getCategoryId(), savedCategory.getName(), savedCategory.getType()));
    }
    
    // The ownership check reads the lazy Category.user, and open-in-view is off.
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
                
        if (category.getUser() == null || !category.getUser().getUsername().equals(getAuthenticatedUsername())) {
            throw new ForbiddenResourceException("Not authorized to delete this category");
        }
        
        if (expenseRepository.existsByCategory_CategoryId(id) || incomeRepository.existsByCategory_CategoryId(id)) {
            throw new ConflictException("Cannot delete category in use by existing transactions");
        }
        
        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }
}