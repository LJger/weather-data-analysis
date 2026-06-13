package com.example.backend.dto;

import com.example.backend.entity.User;

import java.time.OffsetDateTime;

/**
 * 用户个人资料响应 DTO（不含密码等敏感字段）
 */
public class UserProfileResponse {

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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UserProfileResponse() {
    }

    public static UserProfileResponse fromUser(User user) {
        UserProfileResponse dto = new UserProfileResponse();
        dto.userId = user.getUserId();
        dto.username = user.getUsername();
        dto.role = user.getRole();
        dto.email = user.getEmail();
        dto.phone = user.getPhone();
        dto.gender = user.getGender();
        dto.age = user.getAge();
        dto.region = user.getRegion();
        dto.organization = user.getOrganization();
        dto.bio = user.getBio();
        dto.avatarUrl = user.getAvatarUrl();
        dto.createdAt = user.getCreatedAt();
        dto.updatedAt = user.getUpdatedAt();
        return dto;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Short getRole() { return role; }
    public void setRole(Short role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Short getGender() { return gender; }
    public void setGender(Short gender) { this.gender = gender; }

    public Short getAge() { return age; }
    public void setAge(Short age) { this.age = age; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
