package com.agriloop.model;

import com.agriloop.model.enums.UserRole;
import java.util.UUID;

/**
 * Core User entity.
 */
public class User extends BaseEntity {
    private String uuid;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private UserRole role;
    private String status;
    private String avatarUrl;
    private String bio;

    public User() {
        this.uuid = UUID.randomUUID().toString();
        this.status = "ACTIVE";
    }

    public User(String email, String passwordHash, String fullName, String phone, UserRole role) {
        this();
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    @Override
    public String toString() {
        return fullName != null ? fullName + " (" + (role != null ? role.getDisplayName() : "Unknown") + ")" : "User#" + id;
    }
}
