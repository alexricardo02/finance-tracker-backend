package com.example.dataTransferObjects;

public class CategoryDTO {
    private Integer categoryId;
    private String name;
    private String type; 

    public CategoryDTO() {}

    public CategoryDTO(Integer categoryId, String name, String type) {
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
    }


    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}