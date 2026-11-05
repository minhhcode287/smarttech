package com.smarttech.service;

import com.smarttech.model.Category;
import com.smarttech.repository.CategoryRepository;

import java.util.List;

/**
 * Service xử lý các nghiệp vụ Danh mục phân loại sản phẩm (Category).
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService() {
        this.categoryRepository = new CategoryRepository();
    }

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return categoryRepository.findById(id.trim());
    }

    public boolean createCategory(Category category) {
        if (category == null || category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            System.err.println("[CategoryService] Tên danh mục không được để trống!");
            return false;
        }

        Category existing = categoryRepository.findByName(category.getCategoryName().trim());
        if (existing != null) {
            System.err.println("[CategoryService] Tên danh mục '" + category.getCategoryName() + "' đã tồn tại!");
            return false;
        }

        return categoryRepository.save(category);
    }

    public boolean updateCategory(Category category) {
        if (category == null || category.getId() == null) {
            return false;
        }
        return categoryRepository.update(category);
    }

    public boolean deleteCategory(String id) {
        return categoryRepository.deleteById(id);
    }
}
