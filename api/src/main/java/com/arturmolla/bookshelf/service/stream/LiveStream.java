package com.arturmolla.bookshelf.service.stream;

import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory representation of one active live stream.
 * <p>
 * Thread-safe: all mutable state is held in {@link ConcurrentHashMap}.
 */
@Getter
public class LiveStream {

    /**
     * userId of the host — also serves as the unique stream identifier.
     */
    private final Long hostId;
    private final String hostName;
    private final String title;
    private final LocalDateTime startedAt;

    /**
     * userId → SseEmitter for every connected participant
     * (includes both the host and watchers).
     */
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * userId → display name for every connected participant.
     */
    private final ConcurrentHashMap<Long, String> participantNames = new ConcurrentHashMap<>();

    public LiveStream(Long hostId, String hostName, String title) {
        this.hostId = hostId;
        this.hostName = hostName;
        this.title = title;
        this.startedAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Participant management
    // -------------------------------------------------------------------------

    public void addParticipant(Long userId, String displayName, SseEmitter emitter) {
        emitters.put(userId, emitter);
        participantNames.put(userId, displayName);
    }

    public void removeParticipant(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        participantNames.remove(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    public boolean hasParticipant(Long userId) {
        return emitters.containsKey(userId);
    }

    public Collection<Long> getParticipantIds() {
        return emitters.keySet();
    }

    /**
     * Watcher count = all participants minus the host.
     */
    public int getWatcherCount() {
        int total = emitters.size();
        return emitters.containsKey(hostId) ? total - 1 : total;
    }

    public java.util.List<String> getWatcherNames() {
        return participantNames.entrySet().stream()
                .filter(e -> !e.getKey().equals(hostId))
                .map(java.util.Map.Entry::getValue)
                .toList();
    }

    /**
     * Retrieves the emitter for a specific participant, or null if not connected.
     */
    public SseEmitter getEmitter(Long userId) {
        return emitters.get(userId);
    }

    /**
     * Removes a participant from the internal maps <strong>without</strong> completing
     * their emitter.
     *
     * <p>Use this in reconnect scenarios before calling {@code emitter.complete()} on
     * the old emitter.  Because the participant is no longer present in the maps,
     * the old emitter's cleanup callback will see {@code hasParticipant == false}
     * and return early — preventing spurious {@code WATCHER_LEFT} / {@code STREAM_STOPPED}
     * broadcasts or accidental stream destruction.</p>
     *
     * @return the old emitter that was removed, or {@code null} if none was registered
     */
    public SseEmitter silentRemove(Long userId) {
        participantNames.remove(userId);
        return emitters.remove(userId);
    }

    /**
     * Completes all SSE emitters — called when the stream is stopped.
     */
    public void closeAll() {
        emitters.values().forEach(SseEmitter::complete);
        emitters.clear();
        participantNames.clear();
    }
}

