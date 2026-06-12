package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.request.CategoryRequest;
import com.example.DocumentManagement.dto.response.ApiResponse;
import com.example.DocumentManagement.dto.response.CategoryResponse;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        List<CategoryResponse> categories = categoryService.getAll();
        return ResponseEntity.ok(ApiResponse.ok("Categories retrieved", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Category retrieved", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request,
                                                                 @AuthenticationPrincipal User user) {
        CategoryResponse response = categoryService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody CategoryRequest request,
                                                                 @AuthenticationPrincipal User user) {
        CategoryResponse response = categoryService.update(id, request, user);
        return ResponseEntity.ok(ApiResponse.ok("Category updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                     @AuthenticationPrincipal User user) {
        categoryService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted", null));
    }
}
