package com.example.config;

import com.example.models.Category;
import com.example.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategorySeederTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategorySeeder categorySeeder;

    @Test
    @SuppressWarnings("unchecked")
    void run_emptyDatabase_seedsAllDefaultCategories() throws Exception {
        when(categoryRepository.findByUserIsNull()).thenReturn(List.of());

        categorySeeder.run();

        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());

        List<Category> savedCategories = captor.getValue();
        assertThat(savedCategories).hasSize(12);

        // Check income categories seeded
        assertThat(savedCategories).extracting(Category::getName)
                .contains("Salary", "Freelance", "Gift", "Investment", "Other",
                          "Food", "Rent", "Transport", "Entertainment", "Health", "Bills", "Shopping");

        // Verify all global categories have user == null
        assertThat(savedCategories).allMatch(c -> c.getUser() == null);
    }

    @Test
    void run_globalCategoriesAlreadyExist_doesNotSeed() throws Exception {
        Category existing = new Category("Salary", "income", null);
        when(categoryRepository.findByUserIsNull()).thenReturn(List.of(existing));

        categorySeeder.run();

        verify(categoryRepository, never()).saveAll(any());
    }
}
