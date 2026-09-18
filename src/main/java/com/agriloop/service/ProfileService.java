package com.agriloop.service;

import com.agriloop.model.FarmerProfile;
import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.UserRepository;
import com.agriloop.repository.impl.JdbcUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service managing user identity, profile retrieval, and active session state.
 * Strictly avoids mock/fake users.
 */
public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    private static ProfileService instance;

    private final UserRepository userRepository;
    private User currentUser = null; // Strictly null when unauthenticated
    private Consumer<User> userChangeListener;

    private ProfileService() {
        this.userRepository = new JdbcUserRepository();
    }

    public static synchronized ProfileService getInstance() {
        if (instance == null) {
            instance = new ProfileService();
        }
        return instance;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (userChangeListener != null) {
            userChangeListener.accept(currentUser);
        }
    }

    public void setOnUserChangeListener(Consumer<User> listener) {
        this.userChangeListener = listener;
    }

    public Optional<FarmerProfile> getFarmerProfile(Long userId) {
        try {
            return userRepository.findFarmerProfile(userId);
        } catch (Exception e) {
            logger.warn("Could not load farmer profile: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ManufacturerProfile> getManufacturerProfile(Long userId) {
        try {
            return userRepository.findManufacturerProfile(userId);
        } catch (Exception e) {
            logger.warn("Could not load manufacturer profile: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<TransporterProfile> getTransporterProfile(Long userId) {
        try {
            return userRepository.findTransporterProfile(userId);
        } catch (Exception e) {
            logger.warn("Could not load transporter profile: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}
