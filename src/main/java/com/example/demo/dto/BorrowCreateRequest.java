package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BorrowCreateRequest {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @NotNull(message = "预计归还日期不能为空")
    private LocalDate dueDate;

    private String reason;
}
