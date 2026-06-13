package com.example.backend.controller;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.UpdateProfileRequest;
import com.example.backend.dto.UserProfileResponse;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import com.example.backend.service.EmailVerificationService;
import com.example.backend.util.JwtUtil;
import com.example.backend.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    @Value("${file.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailVerificationService = emailVerificationService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // 校验邮箱验证码
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.ok(new AuthResponse(false, "请填写邮箱地址"));
            }
            if (request.getEmailCode() == null || request.getEmailCode().isBlank()) {
                return ResponseEntity.ok(new AuthResponse(false, "请填写邮箱验证码"));
            }
            if (!emailVerificationService.verifyCode(request.getEmail(), request.getEmailCode())) {
                return ResponseEntity.ok(new AuthResponse(false, "邮箱验证码错误或已过期"));
            }

            // 检查用户名是否已存在
            if (userService.existsByUsername(request.getUsername())) {
                return ResponseEntity.ok(new AuthResponse(false, "用户名已存在"));
            }
            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.ok(new AuthResponse(false, "该邮箱已被注册，请直接登录或更换邮箱"));
            }
            if (userService.existsByPhone(request.getPhone())) {
                return ResponseEntity.ok(new AuthResponse(false, "该手机号已被注册，请更换手机号"));
            }
            
            // 强制普通用户角色（管理员只能通过后台创建）
            Short role = (short) 0;
            
            // 注册用户（含完整字段）
            User user = userService.register(request, role);
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername(), user.getUserId(), user.getRole());
            
            return ResponseEntity.ok(AuthResponse.fromUser(true, "注册成功", token, user));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.ok(new AuthResponse(false, resolveRegisterIntegrityMessage(e)));
        } catch (Exception e) {
            return ResponseEntity.ok(new AuthResponse(false, safeAuthMessage(e, "注册失败，请稍后重试")));
        }
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-email-code")
    public ResponseEntity<Map<String, Object>> sendEmailCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "邮箱地址不能为空"));
        }
        // 简单格式校验
        if (!email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "邮箱格式不正确"));
        }
        String error = emailVerificationService.sendVerificationCode(email);
        if (error != null) {
            return ResponseEntity.ok(Map.of("success", false, "message", error));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "验证码已发送至您的邮箱"));
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 查找用户
            Optional<User> userOptional = userService.findByUsername(request.getUsername());
            
            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(new AuthResponse(false, "用户名或密码错误"));
            }
            
            User user = userOptional.get();
            
            // 验证密码
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.ok(new AuthResponse(false, "用户名或密码错误"));
            }
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername(), user.getUserId(), user.getRole());
            
            return ResponseEntity.ok(AuthResponse.fromUser(true, "登录成功", token, user));
        } catch (Exception e) {
            return ResponseEntity.ok(new AuthResponse(false, "登录失败，请稍后重试"));
        }
    }

    private String resolveRegisterIntegrityMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "注册失败，请稍后重试";
        }
        if (message.contains("sys_user_email_key") || message.contains("(email)=")) {
            return "该邮箱已被注册，请直接登录或更换邮箱";
        }
        if (message.contains("sys_user_phone_key") || message.contains("(phone)=")) {
            return "该手机号已被注册，请更换手机号";
        }
        if (message.contains("sys_user_username_key") || message.contains("(username)=")) {
            return "用户名已存在";
        }
        return "注册失败，请稍后重试";
    }

    private String safeAuthMessage(Exception e, String fallback) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return fallback;
        }
        if (message.contains("用户名已存在")
                || message.contains("邮箱已被注册")
                || message.contains("手机号已被注册")) {
            return message;
        }
        return fallback;
    }

    /**
     * 获取用户个人资料（从 JWT Token 获取当前用户）
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestParam(required = false) Long userId) {
        // 优先使用 Token 中的 userId，兼容旧前端传参
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long targetUserId = currentUserId != null ? currentUserId : userId;
        if (targetUserId == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> userOpt = userService.findById(targetUserId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserProfileResponse.fromUser(userOpt.get()));
    }

    /**
     * 更新用户个人资料
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestParam(required = false) Long userId,
            @RequestBody UpdateProfileRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long targetUserId = currentUserId != null ? currentUserId : userId;
        if (targetUserId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            User updated = userService.updateProfile(targetUserId, request);
            return ResponseEntity.ok(UserProfileResponse.fromUser(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密码不能为空且新密码长度不少于6位"));
        }

        Optional<User> userOpt = userService.findById(currentUserId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户不存在"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "原密码错误"));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userService.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "密码修改成功"));
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam(required = false) Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long targetUserId = currentUserId != null ? currentUserId : userId;
        if (targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户未登录"));
        }
        try {
            // 校验文件类型
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "仅支持 JPG/PNG/GIF 格式"));
            }

            // 校验文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "文件大小不能超过 5MB"));
            }

            // 确保上传目录存在
            Path uploadPath = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // 生成文件名: userId_timestamp.ext
            String originalFilename = file.getOriginalFilename();
            String ext = "jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            }
            String filename = targetUserId + "_" + System.currentTimeMillis() + "." + ext;

            // 保存文件
            Path targetPath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 构建可访问 URL
            String avatarUrl = "/uploads/avatars/" + filename;

            // 更新数据库中的 avatar_url
            Optional<User> userOpt = userService.findById(targetUserId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户不存在"));
            }
            User user = userOpt.get();

            // 删除旧头像文件（如果存在）
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                String oldFilename = user.getAvatarUrl().replace("/uploads/avatars/", "");
                Path oldPath = uploadPath.resolve(oldFilename);
                Files.deleteIfExists(oldPath);
            }

            user.setAvatarUrl(avatarUrl);
            userService.save(user);

            return ResponseEntity.ok(Map.of("success", true, "avatarUrl", avatarUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "文件保存失败: " + e.getMessage()));
        }
    }

    /**
     * 注销（删除）用户账户
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestParam(required = false) Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long targetUserId = currentUserId != null ? currentUserId : userId;
        if (targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户未登录"));
        }
        try {
            Optional<User> userOpt = userService.findById(targetUserId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户不存在"));
            }

            User user = userOpt.get();

            // 删除头像文件（如果存在）
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                try {
                    Path uploadPath = Paths.get(avatarDir).toAbsolutePath().normalize();
                    String filename = user.getAvatarUrl().replace("/uploads/avatars/", "");
                    Path avatarPath = uploadPath.resolve(filename);
                    Files.deleteIfExists(avatarPath);
                } catch (IOException ignored) {
                    // 头像文件删除失败不影响账户注销
                }
            }

            // 删除用户记录
            userService.deleteUser(targetUserId);

            return ResponseEntity.ok(Map.of("success", true, "message", "账户已成功注销"));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "注销失败: " + e.getMessage()));
        }
    }
}
