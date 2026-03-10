package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * WebRTC signalling payload relayed through the server.
 *
 * @param targetUserId the recipient of this signal (null = broadcast to all watchers)
 * @param signalType   signal type, e.g. "offer", "answer", "ice-candidate"
 * @param payload      the raw JSON string (SDP or ICE candidate object)
 */
public record DtoSignalRequest(
        Long targetUserId,

        @NotBlank(message = "Signal type must not be blank")
        String signalType,

        @NotBlank(message = "Payload must not be blank")
        String payload
) {
}

