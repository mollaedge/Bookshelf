package com.arturmolla.bookshelf.model.dto;

import lombok.Builder;

/**
 * Lightweight info about a single watcher currently connected to a stream.
 *
 * @param watcherId   userId of the watcher
 * @param watcherName display name of the watcher
 */
@Builder
public record DtoWatcherInfo(
        Long watcherId,
        String watcherName
) {
}

