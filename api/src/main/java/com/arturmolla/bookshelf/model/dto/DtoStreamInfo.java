package com.arturmolla.bookshelf.model.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public info about an active live stream.
 *
 * @param streamId     the host's userId (serves as the unique stream identifier)
 * @param hostId       host userId
 * @param hostName     host display name
 * @param title        stream title set by the host
 * @param startedAt    when the stream began
 * @param watcherCount current live watcher count
 * @param watcherNames display names of current watchers
 * @param watcherIds   userIds of current watchers — needed by the host to target
 *                     WebRTC signal messages after a reconnect
 */
@Builder
public record DtoStreamInfo(
        Long streamId,
        Long hostId,
        String hostName,
        String title,
        LocalDateTime startedAt,
        int watcherCount,
        List<String> watcherNames,
        List<Long> watcherIds
) {
}

