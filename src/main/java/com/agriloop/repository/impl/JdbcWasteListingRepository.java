package com.agriloop.repository.impl;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.DatabaseException;
import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.ListingStatus;
import com.agriloop.model.enums.WasteCategory;
import com.agriloop.repository.WasteListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
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
 * Real JDBC implementation of WasteListingRepository.
 */
public class JdbcWasteListingRepository implements WasteListingRepository {
    private static final Logger logger = LoggerFactory.getLogger(JdbcWasteListingRepository.class);

    @Override
    public Optional<WasteListing> findById(Long id) {
        String sql = "SELECT l.*, u.full_name as farmer_name FROM waste_listings l " +
                     "JOIN users u ON l.farmer_id = u.id WHERE l.id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find waste listing by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<WasteListing> findAll() {
        String sql = "SELECT l.*, u.full_name as farmer_name FROM waste_listings l " +
                     "JOIN users u ON l.farmer_id = u.id ORDER BY l.id DESC";
        List<WasteListing> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapListing(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all waste listings", e);
        }
        return list;
    }

    @Override
    public List<WasteListing> findByFarmerId(Long farmerId) {
        String sql = "SELECT l.*, u.full_name as farmer_name FROM waste_listings l " +
                     "JOIN users u ON l.farmer_id = u.id WHERE l.farmer_id = ? ORDER BY l.id DESC";
        List<WasteListing> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, farmerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch listings for farmer: " + farmerId, e);
        }
        return list;
    }

    @Override
    public List<WasteListing> findByStatus(ListingStatus status) {
        String sql = "SELECT l.*, u.full_name as farmer_name FROM waste_listings l " +
                     "JOIN users u ON l.farmer_id = u.id WHERE l.status = ? ORDER BY l.id DESC";
        List<WasteListing> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch listings by status: " + status, e);
        }
        return list;
    }

    @Override
    public List<WasteListing> findByCategory(WasteCategory category) {
        String sql = "SELECT l.*, u.full_name as farmer_name FROM waste_listings l " +
                     "JOIN users u ON l.farmer_id = u.id WHERE l.category = ? ORDER BY l.id DESC";
        List<WasteListing> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch listings by category: " + category, e);
        }
        return list;
    }

    @Override
    public List<WasteListing> searchAvailableListings(String keyword, WasteCategory category) {
        StringBuilder sql = new StringBuilder(
            "SELECT l.*, u.full_name as farmer_name FROM waste_listings l " +
            "JOIN users u ON l.farmer_id = u.id WHERE l.status = 'AVAILABLE'"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (l.title LIKE ? OR l.waste_type LIKE ? OR l.location LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (category != null) {
            sql.append(" AND l.category = ?");
            params.add(category.name());
        }

        sql.append(" ORDER BY l.created_at DESC");

        List<WasteListing> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search listings", e);
        }
        return list;
    }

    @Override
    public WasteListing save(WasteListing listing) {
        if (listing.getId() == null) {
            String sql = "INSERT INTO waste_listings (farmer_id, title, waste_type, category, description, " +
                         "quantity_tons, price_per_ton, moisture_content_pct, location, gps_latitude, gps_longitude, " +
                         "status, available_from, available_until) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, listing.getFarmerId());
                stmt.setString(2, listing.getTitle());
                stmt.setString(3, listing.getWasteType());
                stmt.setString(4, listing.getCategory().name());
                stmt.setString(5, listing.getDescription());
                stmt.setBigDecimal(6, listing.getQuantityTons());
                stmt.setBigDecimal(7, listing.getPricePerTon());

                if (listing.getMoistureContentPct() != null) {
                    stmt.setBigDecimal(8, listing.getMoistureContentPct());
                } else {
                    stmt.setNull(8, java.sql.Types.DECIMAL);
                }

                stmt.setString(9, listing.getLocation());

                if (listing.getGpsLatitude() != null) {
                    stmt.setBigDecimal(10, listing.getGpsLatitude());
                } else {
                    stmt.setNull(10, java.sql.Types.DECIMAL);
                }

                if (listing.getGpsLongitude() != null) {
                    stmt.setBigDecimal(11, listing.getGpsLongitude());
                } else {
                    stmt.setNull(11, java.sql.Types.DECIMAL);
                }

                stmt.setString(12, listing.getStatus() != null ? listing.getStatus().name() : ListingStatus.AVAILABLE.name());
                
                if (listing.getAvailableFrom() != null) {
                    stmt.setDate(13, Date.valueOf(listing.getAvailableFrom()));
                } else {
                    stmt.setDate(13, Date.valueOf(java.time.LocalDate.now()));
                }

                if (listing.getAvailableUntil() != null) {
                    stmt.setDate(14, Date.valueOf(listing.getAvailableUntil()));
                } else {
                    stmt.setNull(14, java.sql.Types.DATE);
                }

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) listing.setId(rs.getLong(1));
                }
                listing.setCreatedAt(LocalDateTime.now());
                listing.setUpdatedAt(LocalDateTime.now());
                logger.info("Successfully inserted waste listing: ID {} - '{}'", listing.getId(), listing.getTitle());
                return listing;
            } catch (SQLException e) {
                logger.error("[DATABASE EXCEPTION] Insert failed on waste_listings | Farmer ID: {} | Title: '{}' | SQLState: {} | Vendor Code: {} | Message: {}", 
                    listing.getFarmerId(), listing.getTitle(), e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
                throw new DatabaseException("Failed to insert waste listing: " + listing.getTitle(), e);
            }
        } else {
            String sql = "UPDATE waste_listings SET title = ?, waste_type = ?, category = ?, description = ?, " +
                         "quantity_tons = ?, price_per_ton = ?, moisture_content_pct = ?, location = ?, " +
                         "gps_latitude = ?, gps_longitude = ?, status = ?, available_from = ?, available_until = ?, " +
                         "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, listing.getTitle());
                stmt.setString(2, listing.getWasteType());
                stmt.setString(3, listing.getCategory().name());
                stmt.setString(4, listing.getDescription());
                stmt.setBigDecimal(5, listing.getQuantityTons());
                stmt.setBigDecimal(6, listing.getPricePerTon());

                if (listing.getMoistureContentPct() != null) {
                    stmt.setBigDecimal(7, listing.getMoistureContentPct());
                } else {
                    stmt.setNull(7, java.sql.Types.DECIMAL);
                }

                stmt.setString(8, listing.getLocation());

                if (listing.getGpsLatitude() != null) {
                    stmt.setBigDecimal(9, listing.getGpsLatitude());
                } else {
                    stmt.setNull(9, java.sql.Types.DECIMAL);
                }

                if (listing.getGpsLongitude() != null) {
                    stmt.setBigDecimal(10, listing.getGpsLongitude());
                } else {
                    stmt.setNull(10, java.sql.Types.DECIMAL);
                }

                stmt.setString(11, listing.getStatus() != null ? listing.getStatus().name() : ListingStatus.AVAILABLE.name());
                
                if (listing.getAvailableFrom() != null) {
                    stmt.setDate(12, Date.valueOf(listing.getAvailableFrom()));
                } else {
                    stmt.setDate(12, Date.valueOf(java.time.LocalDate.now()));
                }

                if (listing.getAvailableUntil() != null) {
                    stmt.setDate(13, Date.valueOf(listing.getAvailableUntil()));
                } else {
                    stmt.setNull(13, java.sql.Types.DATE);
                }

                stmt.setLong(14, listing.getId());

                stmt.executeUpdate();
                listing.setUpdatedAt(LocalDateTime.now());
                logger.info("Successfully updated waste listing: ID {} - '{}'", listing.getId(), listing.getTitle());
                return listing;
            } catch (SQLException e) {
                logger.error("[DATABASE EXCEPTION] Update failed on waste_listings (ID: {}) | SQLState: {} | Vendor Code: {} | Message: {}", 
                    listing.getId(), e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
                throw new DatabaseException("Failed to update waste listing: " + listing.getId(), e);
            }
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM waste_listings WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete waste listing by ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM waste_listings";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count waste listings", e);
        }
        return 0;
    }

    private WasteListing mapListing(ResultSet rs) throws SQLException {
        WasteListing l = new WasteListing();
        l.setId(rs.getLong("id"));
        l.setFarmerId(rs.getLong("farmer_id"));
        l.setTitle(rs.getString("title"));
        l.setWasteType(rs.getString("waste_type"));
        l.setCategory(WasteCategory.valueOf(rs.getString("category")));
        l.setDescription(rs.getString("description"));
        l.setQuantityTons(rs.getBigDecimal("quantity_tons"));
        l.setPricePerTon(rs.getBigDecimal("price_per_ton"));
        l.setMoistureContentPct(rs.getBigDecimal("moisture_content_pct"));
        l.setLocation(rs.getString("location"));
        l.setGpsLatitude(rs.getBigDecimal("gps_latitude"));
        l.setGpsLongitude(rs.getBigDecimal("gps_longitude"));
        l.setStatus(ListingStatus.valueOf(rs.getString("status")));

        Date from = rs.getDate("available_from");
        if (from != null) l.setAvailableFrom(from.toLocalDate());

        Date until = rs.getDate("available_until");
        if (until != null) l.setAvailableUntil(until.toLocalDate());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) l.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) l.setUpdatedAt(updated.toLocalDateTime());

        try {
            l.setFarmerName(rs.getString("farmer_name"));
        } catch (Exception ignored) {}

        return l;
    }
}
