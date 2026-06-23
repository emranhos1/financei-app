package com.finance.service;

import com.finance.entity.Category;
import com.finance.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Category createCategory(Long userId, String name, Category.CategoryType type) {
        Category category = Category.builder()
                .userId(userId)
                .name(name)
                .type(type)
                .build();
        return categoryRepository.save(category);
    }

    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public List<Category> getCategoriesByUserId(Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    public List<Category> getIncomeCategories(Long userId) {
        return categoryRepository.findByUserIdAndType(userId, Category.CategoryType.income);
    }

    public List<Category> getExpenseCategories(Long userId) {
        return categoryRepository.findByUserIdAndType(userId, Category.CategoryType.expense);
    }

    public Category updateCategory(Long categoryId, String name) {
        Category category = getCategoryById(categoryId);
        category.setName(name);
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }
}
