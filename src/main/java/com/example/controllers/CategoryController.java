package com.example.controllers;

import com.example.dataTransferObjects.CategoryDTO;
import com.example.models.Category;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:3000") 
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    // Utilitário para obter o username do token JWT
    private String getAuthenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // 1. Obter todas as categorias do utilizador
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getUserCategories() {
        User user = userRepository.findByUsername(getAuthenticatedUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<CategoryDTO> categories = categoryRepository.findByUserUserIdOrUserIsNull(user.getUserId())
                .stream()
                .map(c -> new CategoryDTO(c.getCategoryId(), c.getName(), c.getType()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }

    // 2. Criar uma nova categoria
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO categoryDTO) {
        User user = userRepository.findByUsername(getAuthenticatedUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Category category = new Category(categoryDTO.getName(), categoryDTO.getType(), user);
        Category savedCategory = categoryRepository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CategoryDTO(savedCategory.getCategoryId(), savedCategory.getName(), savedCategory.getType()));
    }
    
    // 3. Eliminar uma categoria
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
                
        // Proteção: Garantir que o utilizador só apaga as suas próprias categorias
        if (!category.getUser().getUsername().equals(getAuthenticatedUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        
        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }
}