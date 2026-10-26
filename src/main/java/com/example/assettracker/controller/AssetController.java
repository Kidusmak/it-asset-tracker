package com.example.assettracker.controller;

import com.example.assettracker.dto.AssetRequest;
import com.example.assettracker.dto.AssetResponse;
import com.example.assettracker.entity.AssetStatus;
import com.example.assettracker.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /** GET /api/assets?status=AVAILABLE */
    @GetMapping
    public List<AssetResponse> list(@RequestParam(required = false) AssetStatus status) {
        return assetService.findAll(status);
    }

    @GetMapping("/my-assets")
    public List<AssetResponse> myAssets(Authentication authentication) {
        return assetService.findMyAssets(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
    }

    @PatchMapping("/{id}/assign/{userId}")
    public AssetResponse assign(@PathVariable Long id, @PathVariable Long userId) {
        return assetService.assign(id, userId);
    }

    @PatchMapping("/{id}/return")
    public AssetResponse returnAsset(@PathVariable Long id) {
        return assetService.returnAsset(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
