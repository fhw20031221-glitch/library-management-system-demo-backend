package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 每次请求都会经过这个过滤器。
     * 它从 Authorization 请求头取出 JWT，校验成功后把用户身份放入 Spring Security 上下文。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);//调用下面的那个方法截取
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {//需要token不为空但是上下文为空才行
            try {
                if (jwtTokenProvider.isValid(token)) {
                    String username = jwtTokenProvider.getUsername(token);
                    LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(username);
                    if (loginUser.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                loginUser,
                                null,
                                loginUser.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 请求头中解析 Bearer Token。
     * 例如请求头为 Authorization: Bearer xxx，返回的就是 xxx。
     */
    private String resolveToken(HttpServletRequest request) {//截取token体，消掉前缀，第七个开始截
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
