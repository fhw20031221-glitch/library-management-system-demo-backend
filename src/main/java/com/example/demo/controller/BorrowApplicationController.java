package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.dto.BorrowApprovalRequest;
import com.example.demo.dto.BorrowCreateRequest;
import com.example.demo.security.LoginUser;
import com.example.demo.service.BorrowApplicationService;
import com.example.demo.vo.BorrowApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrow-applications")
@RequiredArgsConstructor
public class BorrowApplicationController {

    private final BorrowApplicationService borrowApplicationService;

    @PostMapping
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<BorrowApplicationVO> create(
            @Valid @RequestBody BorrowCreateRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.create(request, loginUser));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<PageResult<BorrowApplicationVO>> pageMine(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(defaultValue = "") String status,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.pageMine(current, size, status, loginUser));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResult<BorrowApplicationVO>> pageAll(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(defaultValue = "") String status) {
        return ApiResponse.success(borrowApplicationService.pageAll(current, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ApiResponse<BorrowApplicationVO> detail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.detail(id, loginUser));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BorrowApplicationVO> approve(
            @PathVariable("id") Long id,
            @Valid @RequestBody BorrowApprovalRequest request) {
        return ApiResponse.success(borrowApplicationService.approve(id, request));
    }

    @PatchMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ApiResponse<BorrowApplicationVO> returnBook(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.returnBook(id, loginUser));
    }
}
