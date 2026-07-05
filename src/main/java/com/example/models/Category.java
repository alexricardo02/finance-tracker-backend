package com.example.models;

import jakarta.persistence.*;

@Entity
@Table(name="categories", indexes = {
    @Index(name = "idx_category_user", columnList = "user_id"),
    @Index(name = "idx_category_type", columnList = "type")
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="category_id", nullable = false)
    private Integer categoryId;

    @Column(name="name", nullable = false, length = 100)
    private String name;

    @Column(name="type", nullable = false, length = 50)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = true)
    private User user;

    public Category() {
    }

    public Category(String name, String type, User user) {
        this.name = name;
        this.type = type;
        this.user = user;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}