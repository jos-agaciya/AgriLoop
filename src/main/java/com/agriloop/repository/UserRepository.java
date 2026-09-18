package com.agriloop.repository;

import com.agriloop.model.FarmerProfile;
import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User and Role Profile persistence.
 */
public interface UserRepository extends BaseRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUuid(String uuid);
    List<User> findByRole(UserRole role);

    // Profile-specific queries
    Optional<FarmerProfile> findFarmerProfile(Long userId);
    Optional<ManufacturerProfile> findManufacturerProfile(Long userId);
    Optional<TransporterProfile> findTransporterProfile(Long userId);

    FarmerProfile saveFarmerProfile(FarmerProfile profile);
    ManufacturerProfile saveManufacturerProfile(ManufacturerProfile profile);
    TransporterProfile saveTransporterProfile(TransporterProfile profile);
}
