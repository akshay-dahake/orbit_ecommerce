package com.orbit.ecommerce.product.service;

import com.orbit.ecommerce.product.dto.ProductRequest;
import com.orbit.ecommerce.product.dto.ProductResponse;
import com.orbit.ecommerce.product.exception.ResourceNotFoundException;
import com.orbit.ecommerce.product.model.Product;
import com.orbit.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    public List<ProductResponse> search(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategoryIgnoreCaseAndActiveTrue(category).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();
        return toResponse(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findEntity(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        return toResponse(productRepository.save(product));
    }

    public void delete(Long id) {
        Product product = findEntity(id);
        productRepository.delete(product);
    }

    public ProductResponse activate(Long id) {
        Product product = findEntity(id);
        product.setActive(true);
        return toResponse(productRepository.save(product));
    }

    public ProductResponse deactivate(Long id) {
        Product product = findEntity(id);
        product.setActive(false);
        return toResponse(productRepository.save(product));
    }

    private Product findEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .category(p.getCategory())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
