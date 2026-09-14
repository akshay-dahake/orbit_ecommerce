package com.orbit.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

// inventory-service's increase/decrease endpoints require role=ADMIN or the
// internal API key - InventoryFeignClientConfig attaches that key to every
// request this client makes.
@FeignClient(
        name = "inventory-service",
        url = "${inventory-service.base-url}",
        configuration = InventoryFeignClientConfig.class
)
public interface InventoryFeignClient {

    @PatchMapping("/api/inventory/{productId}/decrease")
    void decreaseStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity);

    @PatchMapping("/api/inventory/{productId}/increase")
    void increaseStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity);
}
