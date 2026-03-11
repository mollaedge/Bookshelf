package com.arturmolla.bookshelf.model.enums;

public enum StreamEventType {
    /**
     * Stream has just been started by the host.
     */
    STREAM_STARTED,
    /**
     * A new watcher joined the stream.
     */
    WATCHER_JOINED,
    /**
     * A watcher left the stream.
     */
    WATCHER_LEFT,
    /**
     * Stream was stopped by the host.
     */
    STREAM_STOPPED,
    /**
     * WebRTC signalling: SDP offer/answer or ICE candidate relayed between peers.
     */
    SIGNAL
}

