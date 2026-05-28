package com.example.demo.security;

import com.example.demo.common.Constants;
import com.example.demo.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class LoginUser implements UserDetails {//通过接口UserDetails识别，这是一个过滤器专用的数据模型

    private final Long id;
    private final String username;
    private final String password;
    private final String nickname;
    private final String role;
    private final String status;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * 把数据库中的 User 实体转换成 Spring Security 可识别的登录用户。
     * 角色需要加上 ROLE_ 前缀，hasRole('ADMIN') 才能匹配到 ROLE_ADMIN。
     */
    public LoginUser(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.nickname = user.getNickname();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    /**
     * 返回当前登录用户 ID。
     * 业务层会用它判断“我的申请”等数据归属。
     */
    public Long getId() {
        return id;
    }

    /**
     * 返回用户昵称。
     * 前端展示当前用户信息时会使用。
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 返回业务角色。
     * 业务代码中使用 ADMIN、READER 判断用户类型。
     */
    public String getRole() {
        return role;
    }

    /**
     * 返回用户启用状态。
     * ENABLED 表示可登录，DISABLED 表示禁用。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 返回 Spring Security 识别的权限集合。
     * 这里的 ROLE_ADMIN、ROLE_READER 会被 @PreAuthorize 和 hasRole 使用。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * 返回加密后的密码。
     * Spring Security 登录校验时会用它和前端提交的明文密码做 BCrypt 匹配。
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 返回登录用户名。
     * 该值也是 JWT subject 中保存的身份标识。
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 判断账号是否未过期。
     * 当前 Demo 不做账号过期控制，所以固定返回 true。
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 判断账号是否未锁定。
     * 当前 Demo 不做锁定次数控制，所以固定返回 true。
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 判断密码凭证是否未过期。
     * 当前 Demo 不做密码过期控制，所以固定返回 true。
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 判断账号是否启用。
     * 用户状态为 ENABLED 才能被认证为有效登录用户。
     */
    @Override
    public boolean isEnabled() {
        return Constants.STATUS_ENABLED.equals(status);
    }
}
