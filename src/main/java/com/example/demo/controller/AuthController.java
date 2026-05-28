package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.security.LoginUser;
import com.example.demo.service.AuthService;
import com.example.demo.vo.AuthVO;
import com.example.demo.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录接口。
     * 前端提交用户名和密码，Service 校验成功后返回 JWT Token 和当前用户信息。
     */
    @PostMapping("/login")
    public ApiResponse<AuthVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 获取当前登录用户信息。
     * @AuthenticationPrincipal 会从 Spring Security 上下文中取出 JWT 解析后的 LoginUser。
     */
    @GetMapping("/me")
    public ApiResponse<UserVO> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(authService.current(loginUser));
    }
}
