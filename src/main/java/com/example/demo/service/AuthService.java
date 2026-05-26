package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.LoginRequest;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.LoginUser;
import com.example.demo.vo.AuthVO;
import com.example.demo.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthVO login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            return AuthVO.builder()
                    .token(jwtTokenProvider.generateToken(loginUser))
                    .user(toVO(loginUser))
                    .build();
        } catch (AuthenticationException ex) {
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    public UserVO current(LoginUser loginUser) {
        return toVO(loginUser);
    }

    private UserVO toVO(LoginUser loginUser) {
        return UserVO.builder()
                .id(loginUser.getId())
                .username(loginUser.getUsername())
                .nickname(loginUser.getNickname())
                .role(loginUser.getRole())
                .status(loginUser.getStatus())
                .build();
    }
}
