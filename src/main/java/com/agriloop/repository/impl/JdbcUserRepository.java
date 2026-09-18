package com.agriloop.repository.impl;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.DatabaseException;
import com.agriloop.model.FarmerProfile;
import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Real JDBC implementation of UserRepository.
 */
public class JdbcUserRepository implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(JdbcUserRepository.class);

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUuid(String uuid) {
        String sql = "SELECT * FROM users WHERE uuid = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by UUID: " + uuid, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id DESC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all users", e);
        }
        return list;
    }

    @Override
    public List<User> findByRole(UserRole role) {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY id DESC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch users by role: " + role, e);
        }
        return list;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            String sql = "INSERT INTO users (uuid, email, password_hash, full_name, phone, role, status, avatar_url, bio) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, user.getUuid());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, user.getPasswordHash());
                stmt.setString(4, user.getFullName());
                stmt.setString(5, user.getPhone());
                stmt.setString(6, user.getRole().name());
                stmt.setString(7, user.getStatus());
                stmt.setString(8, user.getAvatarUrl());
                stmt.setString(9, user.getBio());

                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getLong(1));
                    }
                }
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert new user: " + user.getEmail(), e);
            }
        } else {
            String sql = "UPDATE users SET full_name = ?, phone = ?, role = ?, status = ?, avatar_url = ?, bio = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user.getFullName());
                stmt.setString(2, user.getPhone());
                stmt.setString(3, user.getRole().name());
                stmt.setString(4, user.getStatus());
                stmt.setString(5, user.getAvatarUrl());
                stmt.setString(6, user.getBio());
                stmt.setLong(7, user.getId());
                stmt.executeUpdate();
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update user: " + user.getId(), e);
            }
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete user by ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count users", e);
        }
        return 0;
    }

    @Override
    public Optional<FarmerProfile> findFarmerProfile(Long userId) {
        String sql = "SELECT * FROM farmer_profiles WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    FarmerProfile p = new FarmerProfile();
                    p.setId(rs.getLong("id"));
                    p.setUserId(rs.getLong("user_id"));
                    p.setFarmName(rs.getString("farm_name"));
                    p.setFarmLocation(rs.getString("farm_location"));
                    p.setFarmSizeAcres(rs.getBigDecimal("farm_size_acres"));
                    p.setPrimaryCropTypes(rs.getString("primary_crop_types"));
                    p.setGpsLatitude(rs.getBigDecimal("gps_latitude"));
                    p.setGpsLongitude(rs.getBigDecimal("gps_longitude"));
                    return Optional.of(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find farmer profile for user: " + userId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ManufacturerProfile> findManufacturerProfile(Long userId) {
        String sql = "SELECT * FROM manufacturer_profiles WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ManufacturerProfile p = new ManufacturerProfile();
                    p.setId(rs.getLong("id"));
                    p.setUserId(rs.getLong("user_id"));
                    p.setCompanyName(rs.getString("company_name"));
                    p.setIndustryType(rs.getString("industry_type"));
                    p.setFacilityAddress(rs.getString("facility_address"));
                    p.setRequiredWasteTypes(rs.getString("required_waste_types"));
                    p.setProcessingCapacityTons(rs.getBigDecimal("processing_capacity_tons"));
                    p.setGpsLatitude(rs.getBigDecimal("gps_latitude"));
                    p.setGpsLongitude(rs.getBigDecimal("gps_longitude"));
                    return Optional.of(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find manufacturer profile for user: " + userId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<TransporterProfile> findTransporterProfile(Long userId) {
        String sql = "SELECT * FROM transporter_profiles WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TransporterProfile p = new TransporterProfile();
                    p.setId(rs.getLong("id"));
                    p.setUserId(rs.getLong("user_id"));
                    p.setVehicleType(rs.getString("vehicle_type"));
                    p.setVehicleNumber(rs.getString("vehicle_number"));
                    p.setMaxPayloadTons(rs.getBigDecimal("max_payload_tons"));
                    p.setOperatingRadiusKm(rs.getBigDecimal("operating_radius_km"));
                    p.setLicenseNumber(rs.getString("license_number"));
                    p.setAvailable(rs.getBoolean("is_available"));
                    return Optional.of(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find transporter profile for user: " + userId, e);
        }
        return Optional.empty();
    }

    @Override
    public FarmerProfile saveFarmerProfile(FarmerProfile profile) {
        if (profile.getId() == null) {
            Optional<FarmerProfile> existing = findFarmerProfile(profile.getUserId());
            if (existing.isPresent()) {
                profile.setId(existing.get().getId());
                return saveFarmerProfile(profile);
            }

            String sql = "INSERT INTO farmer_profiles (user_id, farm_name, farm_location, farm_size_acres, primary_crop_types, gps_latitude, gps_longitude) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, profile.getUserId());
                stmt.setString(2, profile.getFarmName());
                stmt.setString(3, profile.getFarmLocation());
                stmt.setBigDecimal(4, profile.getFarmSizeAcres());
                stmt.setString(5, profile.getPrimaryCropTypes());
                stmt.setBigDecimal(6, profile.getGpsLatitude());
                stmt.setBigDecimal(7, profile.getGpsLongitude());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) profile.setId(rs.getLong(1));
                }
                return profile;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to save farmer profile", e);
            }
        } else {
            String sql = "UPDATE farmer_profiles SET farm_name = ?, farm_location = ?, farm_size_acres = ?, primary_crop_types = ?, gps_latitude = ?, gps_longitude = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, profile.getFarmName());
                stmt.setString(2, profile.getFarmLocation());
                stmt.setBigDecimal(3, profile.getFarmSizeAcres());
                stmt.setString(4, profile.getPrimaryCropTypes());
                stmt.setBigDecimal(5, profile.getGpsLatitude());
                stmt.setBigDecimal(6, profile.getGpsLongitude());
                stmt.setLong(7, profile.getId());
                stmt.executeUpdate();
                return profile;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update farmer profile", e);
            }
        }
    }

    @Override
    public ManufacturerProfile saveManufacturerProfile(ManufacturerProfile profile) {
        if (profile.getId() == null) {
            Optional<ManufacturerProfile> existing = findManufacturerProfile(profile.getUserId());
            if (existing.isPresent()) {
                profile.setId(existing.get().getId());
                return saveManufacturerProfile(profile);
            }

            String sql = "INSERT INTO manufacturer_profiles (user_id, company_name, industry_type, facility_address, required_waste_types, processing_capacity_tons, gps_latitude, gps_longitude) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, profile.getUserId());
                stmt.setString(2, profile.getCompanyName());
                stmt.setString(3, profile.getIndustryType());
                stmt.setString(4, profile.getFacilityAddress());
                stmt.setString(5, profile.getRequiredWasteTypes());
                stmt.setBigDecimal(6, profile.getProcessingCapacityTons());
                stmt.setBigDecimal(7, profile.getGpsLatitude());
                stmt.setBigDecimal(8, profile.getGpsLongitude());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) profile.setId(rs.getLong(1));
                }
                return profile;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to save manufacturer profile", e);
            }
        } else {
            String sql = "UPDATE manufacturer_profiles SET company_name = ?, industry_type = ?, facility_address = ?, required_waste_types = ?, processing_capacity_tons = ?, gps_latitude = ?, gps_longitude = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, profile.getCompanyName());
                stmt.setString(2, profile.getIndustryType());
                stmt.setString(3, profile.getFacilityAddress());
                stmt.setString(4, profile.getRequiredWasteTypes());
                stmt.setBigDecimal(5, profile.getProcessingCapacityTons());
                stmt.setBigDecimal(6, profile.getGpsLatitude());
                stmt.setBigDecimal(7, profile.getGpsLongitude());
                stmt.setLong(8, profile.getId());
                stmt.executeUpdate();
                return profile;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update manufacturer profile", e);
            }
        }
    }

    @Override
    public TransporterProfile saveTransporterProfile(TransporterProfile profile) {
        if (profile.getId() == null) {
            Optional<TransporterProfile> existing = findTransporterProfile(profile.getUserId());
            if (existing.isPresent()) {
                profile.setId(existing.get().getId());
                return saveTransporterProfile(profile);
            }

            String sql = "INSERT INTO transporter_profiles (user_id, vehicle_type, vehicle_number, max_payload_tons, operating_radius_km, license_number, is_available) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, profile.getUserId());
                stmt.setString(2, profile.getVehicleType());
                stmt.setString(3, profile.getVehicleNumber());
                stmt.setBigDecimal(4, profile.getMaxPayloadTons());
                stmt.setBigDecimal(5, profile.getOperatingRadiusKm());
                stmt.setString(6, profile.getLicenseNumber());
                stmt.setBoolean(7, profile.isAvailable());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) profile.setId(rs.getLong(1));
                }
                return profile;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to save transporter profile", e);
            }
        } else {
            String sql = "UPDATE transporter_profiles SET vehicle_type = ?, vehicle_number = ?, max_payload_tons = ?, operating_radius_km = ?, license_number = ?, is_available = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, profile.getVehicleType());
                stmt.setString(2, profile.getVehicleNumber());
                stmt.setBigDecimal(3, profile.getMaxPayloadTons());
                stmt.setBigDecimal(4, profile.getOperatingRadiusKm());
                stmt.setString(5, profile.getLicenseNumber());
                stmt.setBoolean(6, profile.isAvailable());
                stmt.setLong(7, profile.getId());
                stmt.executeUpdate();
                return profile;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update transporter profile", e);
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUuid(rs.getString("uuid"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setRole(UserRole.valueOf(rs.getString("role")));
        u.setStatus(rs.getString("status"));
        u.setAvatarUrl(rs.getString("avatar_url"));
        u.setBio(rs.getString("bio"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) u.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) u.setUpdatedAt(updated.toLocalDateTime());

        return u;
    }
}
