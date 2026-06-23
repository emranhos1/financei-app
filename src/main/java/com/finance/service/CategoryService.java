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
        boolean exists = categoryRepository.existsByUserIdAndNameAndType(userId, name.trim(), type);
        if (exists) throw new IllegalArgumentException("Category '" + name + "' (" + type.name() + ") already exists");
        return categoryRepository.save(Category.builder()
                .userId(userId).name(name.trim()).type(type).build());
    }

    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public List<Category> getCategoriesByUserId(Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    public List<Category> getIncomeCategories(Long userId) {
        // income + both
        return categoryRepository.findByUserIdAndTypeIn(userId,
                List.of(Category.CategoryType.income, Category.CategoryType.both));
    }

    public List<Category> getExpenseCategories(Long userId) {
        // expense + both
        return categoryRepository.findByUserIdAndTypeIn(userId,
                List.of(Category.CategoryType.expense, Category.CategoryType.both));
    }

    public Category updateCategory(Long categoryId, Long userId, String name, Category.CategoryType type) {
        Category category = getCategoryById(categoryId);
        if (!category.getUserId().equals(userId)) throw new SecurityException("Access denied");

        boolean nameChanged = !category.getName().equals(name.trim());
        boolean typeChanged = category.getType() != type;
        if (nameChanged || typeChanged) {
            boolean exists = categoryRepository.existsByUserIdAndNameAndType(userId, name.trim(), type);
            if (exists) throw new IllegalArgumentException("Category '" + name + "' (" + type.name() + ") already exists");
        }
        category.setName(name.trim());
        category.setType(type);
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }
}