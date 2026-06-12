package com.example.DocumentManagement.service;

import com.example.DocumentManagement.dto.request.CategoryRequest;
import com.example.DocumentManagement.dto.response.CategoryResponse;
import com.example.DocumentManagement.entity.Category;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.exception.BadRequestException;
import com.example.DocumentManagement.exception.ResourceNotFoundException;
import com.example.DocumentManagement.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    public CategoryService(CategoryRepository categoryRepository, AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
    }

    public CategoryResponse create(CategoryRequest request, User user) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category '" + request.getName() + "' already exists");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        categoryRepository.save(category);

        auditLogService.log(user, "CREATE_CATEGORY", "Category", category.getId(),
                "Created category: " + category.getName());

        return CategoryResponse.from(category);
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return CategoryResponse.from(category);
    }

    public CategoryResponse update(Long id, CategoryRequest request, User user) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category '" + request.getName() + "' already exists");
        }

        String oldName = category.getName();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        categoryRepository.save(category);

        auditLogService.log(user, "UPDATE_CATEGORY", "Category", category.getId(),
                "Updated category: " + oldName + " -> " + category.getName());

        return CategoryResponse.from(category);
    }

    public void delete(Long id, User user) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        auditLogService.log(user, "DELETE_CATEGORY", "Category", id,
                "Deleted category: " + category.getName());

        categoryRepository.delete(category);
    }
}
