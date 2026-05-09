package com.arturmolla.bookshelf.service.messaging;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory registry that tracks which users are currently
 * connected to the messaging SSE channel.
 * <p>
 * Key = userId.  Each user can have at most one active SSE connection.
 * A new connection from the same browser tab (reconnect) simply replaces
 * the old emitter.
 */
@Component
public class MessageEmitterRegistry {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(Long userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
    }

    public void remove(Long userId) {
        emitters.remove(userId);
    }

    public Optional<SseEmitter> find(Long userId) {
        return Optional.ofNullable(emitters.get(userId));
    }

    public boolean isOnline(Long userId) {
        return emitters.containsKey(userId);
    }

    public int onlineCount() {
        return emitters.size();
    }
}

