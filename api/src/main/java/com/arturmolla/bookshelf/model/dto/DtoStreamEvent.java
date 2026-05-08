package com.arturmolla.bookshelf.model.dto;

import com.arturmolla.bookshelf.model.enums.StreamEventType;
import lombok.Builder;

/**
 * Payload pushed to all SSE subscribers when something changes on a stream.
 *
 * @param type          what happened (STREAM_STARTED, WATCHER_JOINED, SIGNAL, etc.)
 * @param streamId      the host's userId (uniquely identifies the stream)
 * @param streamTitle   human-readable title of the stream
 * @param actorId       userId of the person who triggered the event (host or watcher)
 * @param actorName     display name of the actor
 * @param targetUserId  userId of the intended signal recipient (only for SIGNAL events)
 * @param signalType    WebRTC signal type: "offer", "answer", "ice-candidate" (SIGNAL events only)
 * @param payload       raw SDP or ICE candidate JSON string (SIGNAL events only)
 * @param watcherCount  current number of active watchers
 */
@Builder
public record DtoStreamEvent(
        StreamEventType type,
        Long streamId,
        String streamTitle,
        Long actorId,
        String actorName,
        Long targetUserId,
        String signalType,
        String payload,
        int watcherCount
) {
}
