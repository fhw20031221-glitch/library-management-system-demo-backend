package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.dto.PasswordResetRequest;
import com.example.demo.dto.UserCreateRequest;
import com.example.demo.dto.UserStatusRequest;
import com.example.demo.dto.UserUpdateRequest;
import com.example.demo.service.UserService;
import com.example.demo.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表。
     * 支持按用户名/昵称关键词、角色和状态筛选，只允许管理员访问。
     */
    @GetMapping
    public ApiResponse<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String status) {
        return ApiResponse.success(userService.page(current, size, keyword, role, status));
    }

    /**
     * 新增用户。
     * 管理员创建读者或管理员账号，密码会在 Service 层加密后保存。
     */
    @PostMapping
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    /**
     * 编辑用户基础信息。
     * 可修改昵称、角色、状态、手机号和邮箱，不直接修改密码。
     */
    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable("id") Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    /**
     * 单独启用或禁用用户。
     * 被禁用的用户即使 Token 还存在，也不能通过 JWT 过滤器完成登录态认证。
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<UserVO> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.success(userService.updateStatus(id, request));
    }

    /**
     * 重置用户密码。
     * 新密码会用 BCrypt 加密后保存，接口不返回密码内容。
     */
    @PatchMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.success();
    }
}
