package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String secret;
    private final long expirationMinutes;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * 根据登录用户生成 JWT。
     * Token 中写入用户名、用户 ID、角色和过期时间，前端后续请求会携带它。
     */
    public String generateToken(LoginUser loginUser) {//生成token
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(loginUser.getUsername())
                .claim("uid", loginUser.getId())
                .claim("role", loginUser.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从 JWT 中读取用户名。
     * JWT 过滤器会用这个用户名重新查询数据库用户信息。
     */
    public String getUsername(String token) {
        return claims(token).getSubject();
    }

    /**
     * 校验 JWT 是否有效。
     * 如果签名错误、格式错误或已过期，jjwt 会抛异常；正常解析则返回 true。
     */
    public boolean isValid(String token) {
        claims(token);
        return true;
    }

    /**
     * 解析 JWT 的 Claims。
     * Claims 可以理解为 Token 里保存的用户声明数据。
     */
    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 根据配置文件中的密钥生成签名 Key。
     * 生成 Token 和校验 Token 都必须使用同一个密钥。
     */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
