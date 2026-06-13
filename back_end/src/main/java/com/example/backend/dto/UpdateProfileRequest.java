package com.example.backend.dto;

/**
 * 更新用户个人资料请求 DTO
 */
public class UpdateProfileRequest {

    private String email;
    private String phone;
    private Short gender;
    private Short age;
    private String region;
    private String organization;
    private String bio;

    public UpdateProfileRequest() {
    }

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
}
