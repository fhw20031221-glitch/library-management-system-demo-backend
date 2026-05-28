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

    /**
     * 执行登录认证。
     * 认证成功后生成 JWT 并返回用户信息；认证失败时抛出 401 业务异常。
     */
    public AuthVO login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())//使用安全框架进行用户验证
            );
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();//成功了给Token
            return AuthVO.builder()
                    .token(jwtTokenProvider.generateToken(loginUser))
                    .user(toVO(loginUser))
                    .build();
        } catch (AuthenticationException ex) {
            throw new BusinessException(401, "用户名或密码错误");//失败了用异常捕获返回
        }
    }

    /**
     * 返回当前登录用户信息。
     * Controller 已经从 SecurityContext 中拿到 LoginUser，这里只负责转换成前端需要的 VO。
     */
    public UserVO current(LoginUser loginUser) {
        return toVO(loginUser);
    }

    /**
     * 把安全框架中的 LoginUser 转换成接口返回对象。
     * 这样不会把密码等敏感字段返回给前端。
     */
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
