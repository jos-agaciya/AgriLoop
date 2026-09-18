package com.agriloop.service;

import com.agriloop.exception.AgriLoopException;
import com.agriloop.exception.ValidationException;
import com.agriloop.model.FarmerProfile;
import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.UserRepository;
import com.agriloop.repository.impl.JdbcUserRepository;
import com.agriloop.util.SecurityUtil;
import com.agriloop.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service managing user authentication, registration, and role profile initialization.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static AuthService instance;

    private final UserRepository userRepository = new JdbcUserRepository();

    private AuthService() {}

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Authenticates user against MySQL with BCrypt verification.
     */
    public User login(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Please enter your email address");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Please enter your password");
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            throw new ValidationException("Invalid email address or password");
        }

        User user = userOpt.get();
        if (!SecurityUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new ValidationException("Invalid email address or password");
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AgriLoopException("Your account is currently inactive. Please contact support.");
        }

        logger.info("User {} authenticated successfully with role {}", user.getEmail(), user.getRole());
        return user;
    }

    /**
     * Registers a new stakeholder and creates their role profile in MySQL.
     */
    public User register(String fullName, String email, String phone, String password, String confirmPassword, String location, UserRole role) {
        if (fullName == null || fullName.trim().length() < 2) {
            throw new ValidationException("Please enter a valid full name");
        }
        if (email == null || !ValidationUtil.isValidEmail(email.trim())) {
            throw new ValidationException("Please enter a valid email address");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long");
        }
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match");
        }
        if (role == null) {
            throw new ValidationException("Please select an account role");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ValidationException("An account with this email address already exists");
        }

        String passwordHash = SecurityUtil.hashPassword(password);
        User user = new User(normalizedEmail, passwordHash, fullName.trim(), phone != null ? phone.trim() : "", role);
        User savedUser = userRepository.save(user);

        // Initialize corresponding role profile
        String loc = location != null && !location.isBlank() ? location.trim() : "Regional";
        try {
            switch (role) {
                case FARMER -> {
                    FarmerProfile fp = new FarmerProfile(savedUser.getId(), fullName.trim() + "'s Farm", loc);
                    userRepository.saveFarmerProfile(fp);
                }
                case MANUFACTURER -> {
                    ManufacturerProfile mp = new ManufacturerProfile(savedUser.getId(), fullName.trim() + " Plant", "Bio-Processing", loc);
                    userRepository.saveManufacturerProfile(mp);
                }
                case TRANSPORTER -> {
                    TransporterProfile tp = new TransporterProfile();
                    tp.setUserId(savedUser.getId());
                    tp.setVehicleType("Standard Commercial Vehicle");
                    tp.setVehicleNumber("VEH-" + (savedUser.getId() * 100 + (int)(Math.random() * 90 + 10)));
                    tp.setMaxPayloadTons(new BigDecimal("15.00"));
                    tp.setOperatingRadiusKm(new BigDecimal("50.00"));
                    tp.setLicenseNumber("LIC-" + savedUser.getId());
                    tp.setAvailable(true);
                    userRepository.saveTransporterProfile(tp);
                }
                default -> {}
            }
        } catch (Exception e) {
            logger.warn("Could not auto-create profile for user {}: {}", savedUser.getId(), e.getMessage());
        }

        logger.info("Successfully registered user {} (ID: {}) with role {}", savedUser.getEmail(), savedUser.getId(), savedUser.getRole());
        return savedUser;
    }
}
