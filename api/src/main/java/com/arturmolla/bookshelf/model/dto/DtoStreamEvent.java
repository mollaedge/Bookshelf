package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.StreamEventType;
import lombok.Builder;

/**
 * Payload pushed to all SSE subscribers when something changes on a stream.
 *
 * @param type         what happened
 * @param streamId     the host's userId (uniquely identifies the stream)
 * @param actorId      userId of the person who triggered the event (host or watcher)
 * @param actorName    display name of the actor
 * @param payload      optional free-form data (e.g. WebRTC SDP/ICE JSON strings)
 * @param watcherCount current number of active watchers
 */
@Builder
public record DtoStreamEvent(
        StreamEventType type,
        Long streamId,
        Long actorId,
        String actorName,
        String payload,
        int watcherCount
) {
}

