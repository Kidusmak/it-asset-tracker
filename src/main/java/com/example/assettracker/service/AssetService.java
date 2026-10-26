package com.example.assettracker.service;

import com.example.assettracker.dto.AssetRequest;
import com.example.assettracker.dto.AssetResponse;
import com.example.assettracker.entity.Asset;
import com.example.assettracker.entity.AssetStatus;
import com.example.assettracker.entity.Category;
import com.example.assettracker.entity.User;
import com.example.assettracker.exception.BadRequestException;
import com.example.assettracker.exception.ConflictException;
import com.example.assettracker.exception.ResourceNotFoundException;
import com.example.assettracker.repository.AssetRepository;
import com.example.assettracker.repository.CategoryRepository;
import com.example.assettracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public AssetService(AssetRepository assetRepository,
                        CategoryRepository categoryRepository,
                        UserRepository userRepository) {
        this.assetRepository = assetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> findAll(AssetStatus status) {
        List<Asset> assets = (status == null)
                ? assetRepository.findAll()
                : assetRepository.findByStatus(status);
        return assets.stream().map(AssetService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> findMyAssets(String username) {
        return assetRepository.findByAssignedToUsername(username).stream()
                .map(AssetService::toResponse)
                .toList();
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        String serial = request.serialNumber().trim();
        if (assetRepository.existsBySerialNumber(serial)) {
            throw new ConflictException("An asset with serial number '" + serial + "' already exists");
        }

        AssetStatus status = request.status() == null ? AssetStatus.AVAILABLE : request.status();
        if (status == AssetStatus.ASSIGNED) {
            throw new BadRequestException(
                    "Assets cannot be created with status ASSIGNED; create it as AVAILABLE and use the assign endpoint");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with id " + request.categoryId() + " not found"));

        Asset asset = new Asset();
        asset.setName(request.name().trim());
        asset.setSerialNumber(serial);
        asset.setStatus(status);
        asset.setCategory(category);
        asset.setAssignedTo(null);

        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse assign(Long assetId, Long userId) {
        Asset asset = getAsset(assetId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new BadRequestException(
                    "Asset " + assetId + " cannot be assigned because its status is " + asset.getStatus()
                            + " (only AVAILABLE assets can be assigned)");
        }

        asset.setAssignedTo(user);
        asset.setStatus(AssetStatus.ASSIGNED);
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse returnAsset(Long assetId) {
        Asset asset = getAsset(assetId);

        if (asset.getStatus() != AssetStatus.ASSIGNED) {
            throw new BadRequestException(
                    "Asset " + assetId + " cannot be returned because its status is " + asset.getStatus()
                            + " (only ASSIGNED assets can be returned)");
        }

        asset.setAssignedTo(null);
        asset.setStatus(AssetStatus.AVAILABLE);
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public void delete(Long assetId) {
        Asset asset = getAsset(assetId);
        assetRepository.delete(asset);
    }

    private Asset getAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset with id " + id + " not found"));
    }

    static AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getSerialNumber(),
                asset.getStatus(),
                CategoryService.toResponse(asset.getCategory()),
                asset.getAssignedTo() == null ? null : AuthService.toResponse(asset.getAssignedTo()),
                asset.getCreatedAt()
        );
    }
}
