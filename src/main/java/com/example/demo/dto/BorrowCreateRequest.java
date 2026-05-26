package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowCreateRequest {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    private String reason;
}
