package com.example.backend.controller;

import com.example.backend.dto.AdminUserSaveRequest;
import com.example.backend.dto.UserProfileResponse;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 管理员接口 - 用户管理
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<User> allUsers = userService.findAll();

        // 过滤
        List<User> filtered = allUsers.stream()
                .filter(u -> {
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.toLowerCase();
                        return (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                                || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw))
                                || (u.getPhone() != null && u.getPhone().contains(kw));
                    }
                    return true;
                })
                .filter(u -> role == null || (u.getRole() != null && u.getRole().equals(role)))
                .collect(Collectors.toList());

        int total = filtered.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<UserProfileResponse> pageData = filtered.subList(start, end).stream()
                .map(UserProfileResponse::fromUser)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageData);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/users/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        List<User> allUsers = userService.findAll();
        long totalUsers = allUsers.size();
        long adminCount = allUsers.stream().filter(u -> u.getRole() != null && u.getRole() == 1).count();
        long normalCount = totalUsers - adminCount;
        OffsetDateTime now = OffsetDateTime.now();
        long newUsers = allUsers.stream()
            .filter(u -> u.getCreatedAt() != null
                && u.getCreatedAt().getYear() == now.getYear()
                && u.getCreatedAt().getMonthValue() == now.getMonthValue())
            .count();

        // 性别统计
        long maleCount = allUsers.stream().filter(u -> u.getGender() != null && u.getGender() == 1).count();
        long femaleCount = allUsers.stream().filter(u -> u.getGender() != null && u.getGender() == 2).count();
        long unknownGender = totalUsers - maleCount - femaleCount;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("adminCount", adminCount);
        stats.put("normalCount", normalCount);
        stats.put("newUsers", newUsers);
        stats.put("bannedUsers", 0);
        stats.put("genderDistribution", Map.of("male", maleCount, "female", femaleCount, "unknown", unknownGender));
        return ResponseEntity.ok(stats);
    }

    /**
     * 管理员新增用户
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody AdminUserSaveRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密码长度不少于6位"));
        }
        if (userService.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名已存在"));
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : 0);
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setGender(request.getGender() != null ? request.getGender() : 0);
        user.setAge(request.getAge());
        user.setRegion(normalizeNullable(request.getRegion()));
        user.setOrganization(normalizeNullable(request.getOrganization()));
        user.setBio(normalizeNullable(request.getBio()));

        userService.save(user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "用户创建成功",
                "user", UserProfileResponse.fromUser(user)
        ));
    }

    /**
     * 管理员更新用户基础信息（不含密码）
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserSaveRequest request
    ) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();

        if (request.getUsername() != null) {
            String newUsername = request.getUsername().trim();
            if (newUsername.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
            }
            Optional<User> conflict = userService.findByUsername(newUsername);
            if (conflict.isPresent() && !conflict.get().getUserId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名已存在"));
            }
            user.setUsername(newUsername);
        }

        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        user.setAge(request.getAge());
        user.setRegion(normalizeNullable(request.getRegion()));
        user.setOrganization(normalizeNullable(request.getOrganization()));
        user.setBio(normalizeNullable(request.getBio()));
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        userService.save(user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "用户更新成功",
                "user", UserProfileResponse.fromUser(user)
        ));
    }

    /**
     * 获取单个用户详情
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileResponse> getUser(@PathVariable Long id) {
        Optional<User> userOpt = userService.findById(id);
        return userOpt.map(u -> ResponseEntity.ok(UserProfileResponse.fromUser(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新用户角色
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        Number roleNum = (Number) body.get("role");
        if (roleNum == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "角色不能为空"));
        }
        user.setRole(roleNum.shortValue());
        userService.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "角色更新成功"));
    }

    /**
     * 重置用户密码（管理员操作）
     */
    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "新密码长度不少于6位"));
        }
        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userService.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "密码重置成功"));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
