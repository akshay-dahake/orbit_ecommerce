package com.orbit.ecommerce.order.client;

import java.math.BigDecimal;

// Mirrors the relevant fields of product-service's ProductResponse.
public class ProductInfo {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
