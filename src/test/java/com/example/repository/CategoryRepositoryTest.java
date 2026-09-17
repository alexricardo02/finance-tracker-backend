package com.example.repository;

import com.example.models.Category;
import com.example.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserIsNull_returnsOnlyGlobalCategories() {
        User user = new User("alice", "alice@example.com", "hash", new Date());
        entityManager.persist(user);

        Category globalCategory = new Category("Salary", "income", null);
        Category userCategory = new Category("Custom Expense", "expense", user);
        entityManager.persist(globalCategory);
        entityManager.persist(userCategory);
        entityManager.flush();

        List<Category> globals = categoryRepository.findByUserIsNull();

        assertThat(globals).isNotEmpty();
        assertThat(globals).allMatch(c -> c.getUser() == null);
        assertThat(globals).extracting(Category::getName).contains("Salary");
        assertThat(globals).extracting(Category::getName).doesNotContain("Custom Expense");
    }

    @Test
    void findByUserUserIdOrUserIsNull_returnsUserSpecificAndGlobalCategories_excludesOtherUsers() {
        User user1 = new User("user1", "user1@example.com", "hash", new Date());
        User user2 = new User("user2", "user2@example.com", "hash", new Date());
        entityManager.persist(user1);
        entityManager.persist(user2);

        Category global = new Category("Global Cat", "income", null);
        Category u1Cat = new Category("User 1 Cat", "expense", user1);
        Category u2Cat = new Category("User 2 Cat", "expense", user2);

        entityManager.persist(global);
        entityManager.persist(u1Cat);
        entityManager.persist(u2Cat);
        entityManager.flush();

        List<Category> user1Categories = categoryRepository.findByUserUserIdOrUserIsNull(user1.getUserId());

        assertThat(user1Categories).extracting(Category::getName).contains("Global Cat", "User 1 Cat");
        assertThat(user1Categories).extracting(Category::getName).doesNotContain("User 2 Cat");
    }

    @Test
    void deleteByUserUserId_removesOnlyCategoriesOfSpecifiedUser() {
        User user = new User("charlie", "charlie@example.com", "hash", new Date());
        entityManager.persist(user);

        Category global = new Category("Global Intact", "income", null);
        Category custom = new Category("Charlie Custom", "expense", user);
        entityManager.persist(global);
        entityManager.persist(custom);
        entityManager.flush();

        categoryRepository.deleteByUserUserId(user.getUserId());
        entityManager.flush();
        entityManager.clear();

        List<Category> remaining = categoryRepository.findAll();
        assertThat(remaining).extracting(Category::getName).contains("Global Intact");
        assertThat(remaining).extracting(Category::getName).doesNotContain("Charlie Custom");
    }
}
