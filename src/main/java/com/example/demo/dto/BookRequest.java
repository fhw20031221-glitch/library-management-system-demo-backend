package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookRequest {

    @NotBlank(message = "书名不能为空")
    private String title;

    @NotBlank(message = "作者不能为空")
    private String author;

    @NotBlank(message = "ISBN不能为空")
    private String isbn;

    private String publisher;

    private String category;

    @NotNull(message = "总库存不能为空")
    @Min(value = 0, message = "总库存不能小于0")
    private Integer totalStock;

    @Min(value = 0, message = "可借库存不能小于0")
    private Integer availableStock;

    private String status;

    private String description;
}
