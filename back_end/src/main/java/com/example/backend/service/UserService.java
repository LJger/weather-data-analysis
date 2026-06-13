package com.example.backend.service;

import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.UpdateProfileRequest;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(userMapper.findById(userId));
    }
    
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return email != null && !email.isBlank() && userMapper.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return phone != null && !phone.isBlank() && userMapper.existsByPhone(phone);
    }
    
    /**
     * 注册新用户（支持完整字段）
     */
    public User register(RegisterRequest request, Short role) {
        if (existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (existsByEmail(request.getEmail())) {
            throw new RuntimeException("该邮箱已被注册，请直接登录或更换邮箱");
        }
        if (existsByPhone(request.getPhone())) {
            throw new RuntimeException("该手机号已被注册，请更换手机号");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getUsername(), passwordHash, role);

        // 设置可选字段
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }
        user.setGender(request.getGender() != null ? request.getGender() : (short) 0);
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getRegion() != null && !request.getRegion().isBlank()) {
            user.setRegion(request.getRegion());
        }
        if (request.getOrganization() != null && !request.getOrganization().isBlank()) {
            user.setOrganization(request.getOrganization());
        }
        if (request.getBio() != null && !request.getBio().isBlank()) {
            user.setBio(request.getBio());
        }

        userMapper.insert(user);
        return user;
    }

    /**
     * 更新用户个人资料（不含密码）
     */
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = Optional.ofNullable(userMapper.findById(userId))
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().isBlank() ? null : request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().isBlank() ? null : request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getRegion() != null) {
            user.setRegion(request.getRegion().isBlank() ? null : request.getRegion());
        }
        if (request.getOrganization() != null) {
            user.setOrganization(request.getOrganization().isBlank() ? null : request.getOrganization());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().isBlank() ? null : request.getBio());
        }

        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.update(user);
        return user;
    }
    
    public User save(User user) {
        if (user.getUserId() == null) {
            userMapper.insert(user);
        } else {
            user.setUpdatedAt(OffsetDateTime.now());
            userMapper.update(user);
        }
        return user;
    }

    /**
     * 注销（删除）用户账户
     */
    public void deleteUser(Long userId) {
        User user = Optional.ofNullable(userMapper.findById(userId))
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        userMapper.deleteById(user.getUserId());
    }

    /**
     * 获取所有用户列表
     */
    public java.util.List<User> findAll() {
        return userMapper.findAll();
    }
}
