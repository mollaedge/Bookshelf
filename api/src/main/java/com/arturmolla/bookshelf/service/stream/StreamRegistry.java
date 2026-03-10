package com.arturmolla.bookshelf.service.stream;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory registry of all active live streams.
 * <p>
 * Key = host's userId.  There is exactly one stream per host at any time.
 */
@Component
public class StreamRegistry {

    private final ConcurrentHashMap<Long, LiveStream> streams = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Registers a new stream.  Overwrites any existing entry for the same host
     * (should not happen in normal flow because {@link #hasStream} is checked first).
     */
    public LiveStream register(LiveStream stream) {
        streams.put(stream.getHostId(), stream);
        return stream;
    }

    /**
     * Removes and returns the stream owned by {@code hostId}, or empty if absent.
     */
    public Optional<LiveStream> remove(Long hostId) {
        return Optional.ofNullable(streams.remove(hostId));
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Optional<LiveStream> find(Long hostId) {
        return Optional.ofNullable(streams.get(hostId));
    }

    public boolean hasStream(Long hostId) {
        return streams.containsKey(hostId);
    }

    public Collection<LiveStream> allStreams() {
        return streams.values();
    }

    public int activeCount() {
        return streams.size();
    }
}

