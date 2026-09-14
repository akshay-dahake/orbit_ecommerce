package com.orbit.ecommerce.gateway.filter;

import com.orbit.ecommerce.gateway.config.SecurityPathsProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Runs on EVERY request before it's routed to a backend service.
 *
 * What this filter does (and deliberately does NOT do):
 *  - Rejects requests to protected routes that have no valid JWT (401).
 *  - Rejects requests to admin-only route patterns unless role=ADMIN (403).
 *  - Forwards the original Authorization header AND adds X-User-Id /
 *    X-User-Role headers downstream, so services can use either.
 *
 * It intentionally does NOT check business rules like "is this the order
 * owner?" or "is this payment refundable?" - the gateway has no idea what an
 * order or a payment is. That authorization stays inside each service (see
 * the JwtAuthenticationFilter + SecurityConfig copied into each of them).
 * This filter's checks are advisory/defense-in-depth on top of that; every
 * service still validates the JWT itself and would reject a forged or
 * missing token even if this filter were bypassed entirely.
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey signingKey;
    private final SecurityPathsProperties securityPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtGatewayFilter(@Value("${jwt.secret}") String secret,
                             SecurityPathsProperties securityPaths) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.securityPaths = securityPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Never intercept CORS preflight - browsers send OPTIONS without an
        // Authorization header, and Spring's CORS filter needs to answer it
        // before any auth logic runs.
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        if (isPublic(method, path)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(header.substring(7))
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        if (isAdminOnly(path) && !"ADMIN".equalsIgnoreCase(role)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "Admin role required");
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublic(String method, String path) {
        for (String rule : securityPaths.getPublicPaths()) {
            String[] parts = rule.split(":", 2);
            if (parts.length == 2) {
                if (parts[0].equalsIgnoreCase(method) && pathMatcher.match(parts[1], path)) {
                    return true;
                }
            } else if (pathMatcher.match(rule, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdminOnly(String path) {
        return securityPaths.getAdminPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status.value(), status.getReasonPhrase(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // run before Gateway's own routing filters
    }
}
