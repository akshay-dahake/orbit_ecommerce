package com.orbit.ecommerce.order.service;

import com.orbit.ecommerce.order.client.InventoryFeignClient;
import com.orbit.ecommerce.order.client.ProductFeignClient;
import com.orbit.ecommerce.order.client.ProductInfo;
import com.orbit.ecommerce.order.dto.*;
import com.orbit.ecommerce.order.exception.InsufficientStockException;
import com.orbit.ecommerce.order.exception.InvalidOrderException;
import com.orbit.ecommerce.order.exception.ResourceNotFoundException;
import com.orbit.ecommerce.order.model.Order;
import com.orbit.ecommerce.order.model.OrderItem;
import com.orbit.ecommerce.order.model.OrderStatus;
import com.orbit.ecommerce.order.repository.OrderRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductFeignClient productFeignClient;
    private final InventoryFeignClient inventoryFeignClient;

    public OrderService(OrderRepository orderRepository, ProductFeignClient productFeignClient,
                         InventoryFeignClient inventoryFeignClient) {
        this.orderRepository = orderRepository;
        this.productFeignClient = productFeignClient;
        this.inventoryFeignClient = inventoryFeignClient;
    }

    // Valid forward transitions per the spec's order status diagram
    // (CREATED -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED, with CANCELLED
    // reachable from any non-terminal state).
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId(userId)
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductInfo product;
            try {
                product = productFeignClient.getProduct(itemRequest.getProductId());
            } catch (FeignException.NotFound e) {
                throw new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId());
            }

            if (!product.isActive()) {
                throw new InvalidOrderException("Product is not available: " + product.getName());
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .subtotal(subtotal)
                    .build();
            order.addItem(item);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        for (OrderItem item : saved.getItems()) {
            inventoryFeignClient.decreaseStock(item.getProductId(), item.getQuantity());
        }

        return toResponse(saved);
    }

    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public OrderResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = findEntity(id);
        assertTransitionAllowed(order.getStatus(), OrderStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        for (OrderItem item : saved.getItems()) {
            inventoryFeignClient.increaseStock(item.getProductId(), item.getQuantity());
        }

        return toResponse(saved);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getOrdersByStatus(String status) {
        OrderStatus orderStatus = parseStatus(status);
        return orderRepository.findByStatus(orderStatus).stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = findEntity(id);
        OrderStatus newStatus = parseStatus(request.getStatus());
        assertTransitionAllowed(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
    }

    private void assertTransitionAllowed(OrderStatus current, OrderStatus target) {
        if (!VALID_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidOrderException("Cannot transition order from " + current + " to " + target);
        }
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderException("Unknown order status: " + status);
        }
    }

    private Order findEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
