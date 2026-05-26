package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowApplicationVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String isbn;
    private String reason;
    private String status;
    private String approvalComment;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
