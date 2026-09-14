package com.orbit.ecommerce.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Bound from orbit.security.* in application.yml.
@ConfigurationProperties(prefix = "orbit.security")
public class SecurityPathsProperties {

    private List<String> publicPaths = List.of();
    private List<String> adminPaths = List.of();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getAdminPaths() {
        return adminPaths;
    }

    public void setAdminPaths(List<String> adminPaths) {
        this.adminPaths = adminPaths;
    }
}
