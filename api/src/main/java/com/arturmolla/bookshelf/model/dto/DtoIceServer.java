package com.arturmolla.bookshelf.model.dto;

import lombok.Builder;

import java.util.List;

/**
 * ICE server configuration sent to the front-end so it can initialise
 * {@code RTCPeerConnection} with the correct STUN/TURN servers.
 *
 * @param urls       one or more STUN or TURN URLs (e.g. "stun:stun.l.google.com:19302")
 * @param username   credential username (null for STUN-only servers)
 * @param credential credential password (null for STUN-only servers)
 */
@Builder
public record DtoIceServer(
        List<String> urls,
        String username,
        String credential
) {
}

