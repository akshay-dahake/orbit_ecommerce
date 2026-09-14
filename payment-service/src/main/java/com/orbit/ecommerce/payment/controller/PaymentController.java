package com.orbit.ecommerce.payment.controller;

import com.orbit.ecommerce.payment.dto.PaymentRequest;
import com.orbit.ecommerce.payment.dto.PaymentResponse;
import com.orbit.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Any authenticated user
    @PostMapping("/api/payments")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    // CUSTOMER / ADMIN
    @GetMapping("/api/payments/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    // CUSTOMER / ADMIN
    @GetMapping("/api/orders/{orderId}/payment")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }

    // ADMIN only - enforced by SecurityConfig
    @PostMapping("/api/payments/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refund(id));
    }
}
