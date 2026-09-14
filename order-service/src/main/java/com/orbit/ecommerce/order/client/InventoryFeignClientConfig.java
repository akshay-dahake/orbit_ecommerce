package com.orbit.ecommerce.order.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;

// Deliberately NOT annotated with @Configuration: Feign builds a separate,
// per-client context for classes passed via @FeignClient(configuration = ...).
// Adding @Configuration here would also let Spring's main component scan pick
// it up and apply this interceptor globally, which we don't want.
public class InventoryFeignClientConfig {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @org.springframework.context.annotation.Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Api-Key", internalApiKey);
    }
}
