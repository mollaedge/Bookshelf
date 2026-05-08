package com.arturmolla.bookshelf.config;

import com.arturmolla.bookshelf.config.exceptions.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate.limit.capacity:100}")
    private Integer capacity;
    @Value("${rate.limit.tokens:100}")
    private Integer tokens;
    @Value("${rate.limit.minutes:1}")
    private Integer minutes;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Skip rate-limiting for:
     * <ul>
     *   <li>{@code /streams/**} – SSE connections are long-lived (1 request, open forever)
     *       and the WebRTC signalling endpoint is called at high frequency (one call per
     *       ICE candidate / SDP exchange). Applying a token-bucket here would disconnect
     *       active streamers.</li>
     *   <li>Async dispatches – internal Tomcat re-dispatches for SSE writes; these are
     *       never real client requests and must never consume tokens.</li>
     * </ul>
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = path.startsWith(contextPath)
                ? path.substring(contextPath.length())
                : path;
        // Bypass for all streaming paths
        return relativePath.startsWith("/streams/") || relativePath.equals("/streams");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        Bucket bucket = resolveBucket(ip);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            throw new RateLimitExceededException();
        }
    }

    private Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(tokens, Duration.ofMinutes(minutes)));
            return Bucket.builder().addLimit(limit).build();
        });
    }
}


