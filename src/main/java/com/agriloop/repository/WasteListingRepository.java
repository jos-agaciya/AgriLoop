package com.agriloop.repository;

import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.ListingStatus;
import com.agriloop.model.enums.WasteCategory;

import java.util.List;

/**
 * Repository interface for Agricultural Waste Listings.
 */
public interface WasteListingRepository extends BaseRepository<WasteListing, Long> {
    List<WasteListing> findByFarmerId(Long farmerId);
    List<WasteListing> findByStatus(ListingStatus status);
    List<WasteListing> findByCategory(WasteCategory category);
    List<WasteListing> searchAvailableListings(String keyword, WasteCategory category);
}
