package com.example.controllers;

import com.example.dataTransferObjects.CategoryDTO;
import com.example.models.Category;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CategoryController categoryController;

    private User john;
    private User intruder;

    @BeforeEach
    void setUp() {
        john = new User("john", "john@test.com", "hash", new Date());
        john.setUser_id(1);

        intruder = new User("intruder", "intruder@test.com", "hash", new Date());
        intruder.setUser_id(2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    @Test
    void getUserCategories_returnsOwnAndGlobalCategories() {
        authenticateAs("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));

        Category own = new Category("Food", "expense", john);
        own.setCategoryId(5);
        Category global = new Category("Salary", "income", null);
        global.setCategoryId(1);

        when(categoryRepository.findByUserUserIdOrUserIsNull(1)).thenReturn(List.of(own, global));

        ResponseEntity<List<CategoryDTO>> response = categoryController.getUserCategories();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Food");
    }

    @Test
    void getUserCategories_userNotFound_throwsResponseStatusException() {
        authenticateAs("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryController.getUserCategories())
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createCategory_success_returns201WithSavedCategory() {
        authenticateAs("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));

        CategoryDTO input = new CategoryDTO(null, "Travel", "expense");
        Category saved = new Category("Travel", "expense", john);
        saved.setCategoryId(7);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        ResponseEntity<CategoryDTO> response = categoryController.createCategory(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCategoryId()).isEqualTo(7);
        assertThat(response.getBody().getName()).isEqualTo("Travel");
    }

    @Test
    void deleteCategory_ownedByRequester_deletesSuccessfully() {
        authenticateAs("john");
        Category category = new Category("Food", "expense", john);
        category.setCategoryId(5);
        when(categoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(expenseRepository.existsByCategory_CategoryId(5)).thenReturn(false);
        when(incomeRepository.existsByCategory_CategoryId(5)).thenReturn(false);

        ResponseEntity<Void> response = categoryController.deleteCategory(5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_belongingToAnotherUser_throwsForbidden_doesNotDelete() {
        // WHY: IDOR check — a user must not be able to delete another user's category
        // just by guessing/incrementing the categoryId in the path.
        authenticateAs("intruder");
        Category othersCategory = new Category("Food", "expense", john);
        othersCategory.setCategoryId(5);
        when(categoryRepository.findById(5)).thenReturn(Optional.of(othersCategory));

        assertThatThrownBy(() -> categoryController.deleteCategory(5))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not authorized");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_notFound_throwsResponseStatusException() {
        authenticateAs("john");
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryController.deleteCategory(999))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteCategory_inUseByExpenses_throwsConflict_doesNotDelete() {
        authenticateAs("john");
        Category category = new Category("Food", "expense", john);
        category.setCategoryId(5);
        when(categoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(expenseRepository.existsByCategory_CategoryId(5)).thenReturn(true);

        assertThatThrownBy(() -> categoryController.deleteCategory(5))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("in use");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_inUseByIncomes_throwsConflict_doesNotDelete() {
        authenticateAs("john");
        Category category = new Category("Salary", "income", john);
        category.setCategoryId(6);
        when(categoryRepository.findById(6)).thenReturn(Optional.of(category));
        when(expenseRepository.existsByCategory_CategoryId(6)).thenReturn(false);
        when(incomeRepository.existsByCategory_CategoryId(6)).thenReturn(true);

        assertThatThrownBy(() -> categoryController.deleteCategory(6))
                .isInstanceOf(ResponseStatusException.class);

        verify(categoryRepository, never()).delete(any());
    }
}