package com.example.assettracker.service;

import com.example.assettracker.dto.AssetRequest;
import com.example.assettracker.dto.AssetResponse;
import com.example.assettracker.entity.Asset;
import com.example.assettracker.entity.AssetStatus;
import com.example.assettracker.entity.Category;
import com.example.assettracker.entity.Role;
import com.example.assettracker.entity.User;
import com.example.assettracker.exception.BadRequestException;
import com.example.assettracker.exception.ConflictException;
import com.example.assettracker.exception.ResourceNotFoundException;
import com.example.assettracker.repository.AssetRepository;
import com.example.assettracker.repository.CategoryRepository;
import com.example.assettracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AssetService assetService;

    private Category category;
    private User user;

    @BeforeEach
    void setUp() {
        category = new Category("Laptop");
        category.setId(1L);
        user = new User("alice", "hashed", Role.ROLE_STAFF);
        user.setId(7L);
    }

    private Asset assetWithStatus(AssetStatus status) {
        Asset asset = new Asset();
        asset.setId(10L);
        asset.setName("HP ProBook 450");
        asset.setSerialNumber("SN-1");
        asset.setCategory(category);
        asset.setStatus(status);
        return asset;
    }

    @Test
    void assignAvailableAssetSucceeds() {
        Asset asset = assetWithStatus(AssetStatus.AVAILABLE);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetResponse response = assetService.assign(10L, 7L);

        assertEquals(AssetStatus.ASSIGNED, response.status());
        assertEquals("alice", response.assignedTo().username());
    }

    @Test
    void assignAssignedAssetIsRejected() {
        when(assetRepository.findById(10L)).thenReturn(Optional.of(assetWithStatus(AssetStatus.ASSIGNED)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> assetService.assign(10L, 7L));
        verify(assetRepository, never()).save(any());
    }

    @Test
    void assignMaintenanceAssetIsRejected() {
        when(assetRepository.findById(10L)).thenReturn(Optional.of(assetWithStatus(AssetStatus.MAINTENANCE)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> assetService.assign(10L, 7L));
    }

    @Test
    void assignUnknownUserReturnsNotFound() {
        when(assetRepository.findById(10L)).thenReturn(Optional.of(assetWithStatus(AssetStatus.AVAILABLE)));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> assetService.assign(10L, 99L));
    }

    @Test
    void returnClearsAssigneeAndMakesAssetAvailable() {
        Asset asset = assetWithStatus(AssetStatus.ASSIGNED);
        asset.setAssignedTo(user);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetResponse response = assetService.returnAsset(10L);

        assertEquals(AssetStatus.AVAILABLE, response.status());
        assertNull(response.assignedTo());
    }

    @Test
    void createWithDuplicateSerialIsRejected() {
        when(assetRepository.existsBySerialNumber("SN-1")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> assetService.create(new AssetRequest("Laptop", "SN-1", 1L, null)));
    }

    @Test
    void createWithAssignedStatusIsRejected() {
        when(assetRepository.existsBySerialNumber("SN-2")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> assetService.create(new AssetRequest("Laptop", "SN-2", 1L, AssetStatus.ASSIGNED)));
    }

    @Test
    void createDefaultsToAvailable() {
        when(assetRepository.existsBySerialNumber("SN-3")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetResponse response = assetService.create(new AssetRequest("Dell 24 Monitor", "SN-3", 1L, null));

        assertEquals(AssetStatus.AVAILABLE, response.status());
        assertNull(response.assignedTo());
    }
}
