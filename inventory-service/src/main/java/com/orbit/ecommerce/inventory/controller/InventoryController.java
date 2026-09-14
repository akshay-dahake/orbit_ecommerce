package com.orbit.ecommerce.inventory.controller;

import com.orbit.ecommerce.inventory.dto.InventoryRequest;
import com.orbit.ecommerce.inventory.dto.InventoryResponse;
import com.orbit.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ADMIN only - enforced by SecurityConfig (hasRole("ADMIN")), or the internal API key
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll() {
        return ResponseEntity.ok(inventoryService.getAll());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> update(@PathVariable Long productId,
                                                      @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.update(productId, request));
    }

    @PatchMapping("/{productId}/increase")
    public ResponseEntity<InventoryResponse> increase(@PathVariable Long productId, @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.increase(productId, quantity));
    }

    @PatchMapping("/{productId}/decrease")
    public ResponseEntity<InventoryResponse> decrease(@PathVariable Long productId, @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.decrease(productId, quantity));
    }
}
