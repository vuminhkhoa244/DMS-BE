package com.example.DocumentManagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DocumentRequest {

    @NotBlank
    private String title;

    private String description;

    private Long categoryId;

    private List<String> tags;
}
