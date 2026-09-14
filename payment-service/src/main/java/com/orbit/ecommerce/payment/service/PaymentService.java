package com.orbit.ecommerce.payment.service;

import com.orbit.ecommerce.payment.dto.PaymentRequest;
import com.orbit.ecommerce.payment.dto.PaymentResponse;
import com.orbit.ecommerce.payment.exception.InvalidPaymentException;
import com.orbit.ecommerce.payment.exception.ResourceNotFoundException;
import com.orbit.ecommerce.payment.model.Payment;
import com.orbit.ecommerce.payment.model.PaymentStatus;
import com.orbit.ecommerce.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // MOCK implementation: real payment gateway integration (Stripe, Razorpay, etc.)
    // would replace the body of this method. For training purposes it always succeeds.
    public PaymentResponse createPayment(PaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .method(request.getMethod())
                .status(PaymentStatus.COMPLETED)
                .build();

        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for orderId: " + orderId));
        return toResponse(payment);
    }

    public PaymentResponse refund(Long id) {
        Payment payment = findEntity(id);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidPaymentException("Only completed payments can be refunded");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        return toResponse(paymentRepository.save(payment));
    }

    private Payment findEntity(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus().name())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
