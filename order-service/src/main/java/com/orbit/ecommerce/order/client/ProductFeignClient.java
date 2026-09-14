package com.orbit.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// GET /api/products/{id} is public in product-service, so no auth header
// needs to be attached here - contrast with InventoryFeignClient below.
@FeignClient(name = "product-service", url = "${product-service.base-url}")
public interface ProductFeignClient {

    @GetMapping("/api/products/{id}")
    ProductInfo getProduct(@PathVariable("id") Long id);
}
