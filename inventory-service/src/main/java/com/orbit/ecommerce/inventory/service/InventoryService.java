package com.orbit.ecommerce.inventory.service;

import com.orbit.ecommerce.inventory.dto.InventoryRequest;
import com.orbit.ecommerce.inventory.dto.InventoryResponse;
import com.orbit.ecommerce.inventory.exception.DuplicateResourceException;
import com.orbit.ecommerce.inventory.exception.InsufficientStockException;
import com.orbit.ecommerce.inventory.exception.ResourceNotFoundException;
import com.orbit.ecommerce.inventory.model.Inventory;
import com.orbit.ecommerce.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public InventoryResponse getByProductId(Long productId) {
        return toResponse(findEntity(productId));
    }

    public InventoryResponse create(InventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new DuplicateResourceException("Inventory record already exists for productId: " + request.getProductId());
        }
        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build();
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse update(Long productId, InventoryRequest request) {
        Inventory inventory = findEntity(productId);
        inventory.setQuantity(request.getQuantity());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse increase(Long productId, int quantity) {
        Inventory inventory = findEntity(productId);
        inventory.setQuantity(inventory.getQuantity() + quantity);
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse decrease(Long productId, int quantity) {
        Inventory inventory = findEntity(productId);
        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stock for productId: " + productId);
        }
        inventory.setQuantity(inventory.getQuantity() - quantity);
        return toResponse(inventoryRepository.save(inventory));
    }

    private Inventory findEntity(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No inventory record for productId: " + productId));
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .quantity(inventory.getQuantity())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
