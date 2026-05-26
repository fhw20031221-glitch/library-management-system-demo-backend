package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.Constants;
import com.example.demo.common.PageResult;
import com.example.demo.dto.PasswordResetRequest;
import com.example.demo.dto.UserCreateRequest;
import com.example.demo.dto.UserStatusRequest;
import com.example.demo.dto.UserUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(long current, long size, String keyword, String role, String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    @Transactional
    public UserVO create(UserCreateRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setRole(normalizeRole(request.getRole(), Constants.ROLE_READER));
        user.setStatus(normalizeStatus(request.getStatus(), Constants.STATUS_ENABLED));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        userMapper.insert(user);
        return toVO(user);
    }

    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        User user = getRequired(id);
        user.setNickname(request.getNickname().trim());
        if (StringUtils.hasText(request.getRole())) {
            user.setRole(normalizeRole(request.getRole(), user.getRole()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            user.setStatus(normalizeStatus(request.getStatus(), user.getStatus()));
        }
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        userMapper.updateById(user);
        return toVO(user);
    }

    @Transactional
    public UserVO updateStatus(Long id, UserStatusRequest request) {
        User user = getRequired(id);
        user.setStatus(normalizeStatus(request.getStatus(), user.getStatus()));
        userMapper.updateById(user);
        return toVO(user);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        User user = getRequired(id);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
    }

    public User getRequired(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    public UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .phone(user.getPhone())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String normalizeRole(String role, String defaultRole) {
        String value = StringUtils.hasText(role) ? role.trim().toUpperCase() : defaultRole;
        if (!Constants.ROLE_ADMIN.equals(value) && !Constants.ROLE_READER.equals(value)) {
            throw new BusinessException("角色只能是 ADMIN 或 READER");
        }
        return value;
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String value = StringUtils.hasText(status) ? status.trim().toUpperCase() : defaultStatus;
        if (!Constants.STATUS_ENABLED.equals(value) && !Constants.STATUS_DISABLED.equals(value)) {
            throw new BusinessException("用户状态只能是 ENABLED 或 DISABLED");
        }
        return value;
    }
}
