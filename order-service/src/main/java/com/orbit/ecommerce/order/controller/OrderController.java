package com.orbit.ecommerce.order.controller;

import com.orbit.ecommerce.order.dto.OrderRequest;
import com.orbit.ecommerce.order.dto.OrderResponse;
import com.orbit.ecommerce.order.security.CurrentUser;
import com.orbit.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Any authenticated user (CUSTOMER or ADMIN) can place an order for themselves.
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(CurrentUser.id(), request));
    }

    // Returns the CALLER's own orders - userId comes from the JWT, not a param.
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(orderService.getOrdersForUser(CurrentUser.id()));
    }

    // CUSTOMER / ADMIN
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    // Any authenticated caller can attempt to cancel; ownership isn't
    // re-checked in this training version (see README "Known simplifications").
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}
