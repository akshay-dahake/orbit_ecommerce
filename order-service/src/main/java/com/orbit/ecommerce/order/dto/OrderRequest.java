package com.orbit.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderRequest {

    // userId is no longer taken from the client - it comes from the
    // authenticated JWT (see CurrentUser.id() in OrderController). Trusting a
    // client-supplied userId here would let anyone place orders as anyone else.
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
