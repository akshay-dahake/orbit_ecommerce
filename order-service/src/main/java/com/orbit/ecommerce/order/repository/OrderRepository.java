package com.orbit.ecommerce.order.repository;

import com.orbit.ecommerce.order.model.Order;
import com.orbit.ecommerce.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
}
