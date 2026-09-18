-- =======================================================================
-- AGRILOOP — Relational Database Schema Definition
-- Smart Agricultural Waste Marketplace
-- Normalized MySQL Schema (InnoDB, UTF-8 MB4)
-- =======================================================================

CREATE DATABASE IF NOT EXISTS `agriloop_db` 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `agriloop_db`;

-- -----------------------------------------------------------------------
-- 1. Users Table (Core Authentication & System Actor Identity)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `uuid` VARCHAR(36) NOT NULL UNIQUE,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(150) NOT NULL,
    `phone` VARCHAR(30) NULL,
    `role` ENUM('FARMER', 'MANUFACTURER', 'TRANSPORTER', 'ADMIN') NOT NULL,
    `status` ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION') NOT NULL DEFAULT 'ACTIVE',
    `avatar_url` VARCHAR(500) NULL,
    `bio` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`),
    INDEX `idx_users_role` (`role`),
    INDEX `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 2. Farmer Profiles Table
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `farmer_profiles` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL UNIQUE,
    `farm_name` VARCHAR(200) NOT NULL,
    `farm_location` VARCHAR(300) NOT NULL,
    `farm_size_acres` DECIMAL(10, 2) NULL,
    `primary_crop_types` VARCHAR(300) NULL,
    `gps_latitude` DECIMAL(10, 7) NULL,
    `gps_longitude` DECIMAL(10, 7) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_farmer_profiles_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_farmer_location` (`farm_location`(50))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 3. Manufacturer Profiles Table
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `manufacturer_profiles` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL UNIQUE,
    `company_name` VARCHAR(200) NOT NULL,
    `industry_type` VARCHAR(150) NOT NULL,
    `facility_address` VARCHAR(300) NOT NULL,
    `required_waste_types` TEXT NULL,
    `processing_capacity_tons` DECIMAL(10, 2) NULL,
    `gps_latitude` DECIMAL(10, 7) NULL,
    `gps_longitude` DECIMAL(10, 7) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_manufacturer_profiles_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_mfg_industry` (`industry_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 4. Transporter Profiles Table
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transporter_profiles` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL UNIQUE,
    `vehicle_type` VARCHAR(100) NOT NULL,
    `vehicle_number` VARCHAR(50) NOT NULL UNIQUE,
    `max_payload_tons` DECIMAL(10, 2) NOT NULL,
    `operating_radius_km` DECIMAL(10, 2) NOT NULL DEFAULT 50.00,
    `license_number` VARCHAR(100) NOT NULL,
    `is_available` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_transporter_profiles_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_transporter_availability` (`is_available`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 5. Waste Listings Table (Agri-Waste Catalog)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `waste_listings` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `farmer_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `waste_type` VARCHAR(100) NOT NULL,
    `category` ENUM('CROP_RESIDUE', 'STALK_AND_STRAW', 'HUSKS_AND_SHELLS', 'BAGASSE', 'MANURE_AND_ORGANIC', 'OTHER') NOT NULL DEFAULT 'CROP_RESIDUE',
    `description` TEXT NULL,
    `quantity_tons` DECIMAL(12, 2) NOT NULL,
    `price_per_ton` DECIMAL(12, 2) NOT NULL,
    `moisture_content_pct` DECIMAL(5, 2) NULL,
    `location` VARCHAR(300) NOT NULL,
    `gps_latitude` DECIMAL(10, 7) NULL,
    `gps_longitude` DECIMAL(10, 7) NULL,
    `status` ENUM('AVAILABLE', 'RESERVED', 'SOLD_OUT', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'AVAILABLE',
    `available_from` DATE NOT NULL,
    `available_until` DATE NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_waste_listings_farmer` FOREIGN KEY (`farmer_id`) 
        REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_listings_status` (`status`),
    INDEX `idx_listings_category` (`category`),
    INDEX `idx_listings_price` (`price_per_ton`),
    INDEX `idx_listings_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 6. Orders Table (Purchases between Buyer & Seller)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `order_number` VARCHAR(50) NOT NULL UNIQUE,
    `buyer_id` BIGINT UNSIGNED NOT NULL,
    `seller_id` BIGINT UNSIGNED NOT NULL,
    `status` ENUM('PENDING', 'CONFIRMED', 'PROCESSING', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    `total_amount` DECIMAL(12, 2) NOT NULL,
    `payment_status` ENUM('UNPAID', 'ESCROW_HELD', 'RELEASED_TO_SELLER', 'REFUNDED') NOT NULL DEFAULT 'UNPAID',
    `delivery_address` VARCHAR(350) NOT NULL,
    `notes` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_orders_buyer` FOREIGN KEY (`buyer_id`) 
        REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_orders_seller` FOREIGN KEY (`seller_id`) 
        REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_orders_status` (`status`),
    INDEX `idx_orders_buyer` (`buyer_id`),
    INDEX `idx_orders_seller` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 7. Order Items Table
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_items` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `listing_id` BIGINT UNSIGNED NOT NULL,
    `quantity_tons` DECIMAL(12, 2) NOT NULL,
    `unit_price` DECIMAL(12, 2) NOT NULL,
    `subtotal` DECIMAL(12, 2) NOT NULL,
    CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) 
        REFERENCES `orders` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_order_items_listing` FOREIGN KEY (`listing_id`) 
        REFERENCES `waste_listings` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_order_items_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 8. Deliveries Table (Transport Coordination & Tracking)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `deliveries` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT UNSIGNED NOT NULL UNIQUE,
    `transporter_id` BIGINT UNSIGNED NULL,
    `pickup_location` VARCHAR(300) NOT NULL,
    `delivery_location` VARCHAR(300) NOT NULL,
    `distance_km` DECIMAL(10, 2) NULL,
    `delivery_cost` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `status` ENUM('ASSIGNMENT_PENDING', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED') NOT NULL DEFAULT 'ASSIGNMENT_PENDING',
    `pickup_time` DATETIME NULL,
    `delivery_time` DATETIME NULL,
    `tracking_code` VARCHAR(50) NOT NULL UNIQUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_deliveries_order` FOREIGN KEY (`order_id`) 
        REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_deliveries_transporter` FOREIGN KEY (`transporter_id`) 
        REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX `idx_deliveries_status` (`status`),
    INDEX `idx_deliveries_tracking` (`tracking_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 9. Transactions Table (Financial Records & Audit Trail)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transactions` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `payer_id` BIGINT UNSIGNED NOT NULL,
    `payee_id` BIGINT UNSIGNED NOT NULL,
    `amount` DECIMAL(12, 2) NOT NULL,
    `payment_method` VARCHAR(50) NOT NULL DEFAULT 'ESCROW_BANK_TRANSFER',
    `transaction_reference` VARCHAR(100) NOT NULL UNIQUE,
    `status` ENUM('INITIATED', 'PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'INITIATED',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_transactions_order` FOREIGN KEY (`order_id`) 
        REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_transactions_payer` FOREIGN KEY (`payer_id`) 
        REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_transactions_payee` FOREIGN KEY (`payee_id`) 
        REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_transactions_status` (`status`),
    INDEX `idx_transactions_ref` (`transaction_reference`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 10. Sustainability Records Table (CO2 and Environmental Tracking)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sustainability_records` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `waste_diverted_tons` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `co2_saved_kg` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `methane_prevented_kg` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `energy_generated_kwh` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `calculation_date` DATE NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sustainability_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_sustainability_user` (`user_id`),
    INDEX `idx_sustainability_date` (`calculation_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 11. Notifications Table (User-Specific Notifications & Alerts)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `recipient_user_id` BIGINT UNSIGNED NOT NULL,
    `notification_type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `message` TEXT NOT NULL,
    `related_order_id` BIGINT UNSIGNED NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notifications_recipient` FOREIGN KEY (`recipient_user_id`) 
        REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_notifications_order` FOREIGN KEY (`related_order_id`) 
        REFERENCES `orders` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX `idx_notifications_recipient` (`recipient_user_id`),
    INDEX `idx_notifications_read` (`is_read`),
    INDEX `idx_notifications_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

