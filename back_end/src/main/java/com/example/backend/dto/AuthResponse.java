package com.example.backend.dto;

import com.example.backend.entity.User;

public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private Long userId;
    private String username;
    private Short role;
    private String email;
    private String phone;
    private Short gender;
    private Short age;
    private String region;
    private String organization;
    private String bio;
    private String avatarUrl;
    
    public AuthResponse() {
    }
    
    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public AuthResponse(boolean success, String message, String token, Long userId, String username, Short role) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    /**
     * 便捷构造：从 User 实体填充全部字段
     */
    public static AuthResponse fromUser(boolean success, String message, String token, User user) {
        AuthResponse resp = new AuthResponse();
        resp.success = success;
        resp.message = message;
        resp.token = token;
        resp.userId = user.getUserId();
        resp.username = user.getUsername();
        resp.role = user.getRole();
        resp.email = user.getEmail();
        resp.phone = user.getPhone();
        resp.gender = user.getGender();
        resp.age = user.getAge();
        resp.region = user.getRegion();
        resp.organization = user.getOrganization();
        resp.bio = user.getBio();
        resp.avatarUrl = user.getAvatarUrl();
        return resp;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Short getRole() {
        return role;
    }
    
    public void setRole(Short role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Short getGender() {
        return gender;
    }

    public void setGender(Short gender) {
        this.gender = gender;
    }

    public Short getAge() {
        return age;
    }

    public void setAge(Short age) {
        this.age = age;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
