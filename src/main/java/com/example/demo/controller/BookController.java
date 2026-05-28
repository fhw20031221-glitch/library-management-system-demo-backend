package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.dto.BookRequest;
import com.example.demo.service.BookService;
import com.example.demo.vo.BookVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * 分页查询图书。
     * 管理员和读者都可以访问，支持关键词和状态筛选。
     */
    @GetMapping
    public ApiResponse<PageResult<BookVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status) {
        return ApiResponse.success(bookService.page(current, size, keyword, status));
    }

    /**
     * 新增图书。
     * 只有管理员可以访问，方法级 @PreAuthorize 负责角色判断。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")//校验管理员权限
    public ApiResponse<BookVO> create(@Valid @RequestBody BookRequest request) {
        return ApiResponse.success(bookService.create(request));
    }

    /**
     * 编辑图书。
     * 路径中的 id 指定要修改的图书，请求体提供新的图书信息。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BookVO> update(@PathVariable("id") Long id, @Valid @RequestBody BookRequest request) {
        return ApiResponse.success(bookService.update(id, request));
    }

    /**
     * 删除图书。
     * 这里执行物理删除，真实业务中也可以改成状态停用或逻辑删除。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        bookService.delete(id);
        return ApiResponse.success();
    }
}
