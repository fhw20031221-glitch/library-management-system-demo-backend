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

    /**
     * 分页查询用户列表。
     * 支持用户名/昵称模糊搜索，并可按角色、状态过滤。
     */
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

    /**
     * 创建用户。
     * 校验用户名唯一，使用 BCrypt 加密密码，再保存用户基础信息。
     */
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

    /**
     * 更新用户基础信息。
     * 不在这里修改密码；密码重置由 resetPassword 单独处理。
     */
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

    /**
     * 更新用户状态。
     * 用于管理员启用或禁用用户账号。
     */
    @Transactional
    public UserVO updateStatus(Long id, UserStatusRequest request) {
        User user = getRequired(id);
        user.setStatus(normalizeStatus(request.getStatus(), user.getStatus()));
        userMapper.updateById(user);
        return toVO(user);
    }

    /**
     * 重置用户密码。
     * 新密码会先 BCrypt 加密，再写入数据库。
     */
    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        User user = getRequired(id);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
    }

    /**
     * 根据 ID 查询用户，不存在就抛 404。
     * 这个方法复用在更新、禁用、重置密码等业务中。
     */
    public User getRequired(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    /**
     * 把数据库实体 User 转换成接口返回对象。
     * 这里不会返回 password 字段，避免密码泄露到前端。
     */
    public UserVO toVO(User user) {//转换
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

    /**
     * 规范化并校验角色。
     * 没有传角色时使用默认角色，只允许 ADMIN 或 READER。
     */
    private String normalizeRole(String role, String defaultRole) {
        String value = StringUtils.hasText(role) ? role.trim().toUpperCase() : defaultRole;
        if (!Constants.ROLE_ADMIN.equals(value) && !Constants.ROLE_READER.equals(value)) {
            throw new BusinessException("角色只能是 ADMIN 或 READER");
        }
        return value;
    }

    /**
     * 规范化并校验用户状态。
     * 没有传状态时使用默认状态，只允许 ENABLED 或 DISABLED。
     */
    private String normalizeStatus(String status, String defaultStatus) {
        String value = StringUtils.hasText(status) ? status.trim().toUpperCase() : defaultStatus;
        if (!Constants.STATUS_ENABLED.equals(value) && !Constants.STATUS_DISABLED.equals(value)) {
            throw new BusinessException("用户状态只能是 ENABLED 或 DISABLED");
        }
        return value;
    }
}
