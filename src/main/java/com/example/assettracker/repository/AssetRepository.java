package com.example.assettracker.repository;

import com.example.assettracker.entity.Asset;
import com.example.assettracker.entity.AssetStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    @Override
    @EntityGraph(attributePaths = {"category", "assignedTo"})
    List<Asset> findAll();

    @Override
    @EntityGraph(attributePaths = {"category", "assignedTo"})
    Optional<Asset> findById(Long id);

    @EntityGraph(attributePaths = {"category", "assignedTo"})
    List<Asset> findByStatus(AssetStatus status);

    @EntityGraph(attributePaths = {"category", "assignedTo"})
    List<Asset> findByAssignedToUsername(String username);

    boolean existsBySerialNumber(String serialNumber);
}
