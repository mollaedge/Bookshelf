package com.arturmolla.bookshelf.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Binds the {@code webrtc.ice} section from application YAML so ICE server
 * configuration can be changed without recompiling.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "webrtc")
public class WebRtcProperties {

    private List<IceServer> iceServers = List.of(
            new IceServer(List.of("stun:stun.l.google.com:19302"), null, null),
            new IceServer(List.of("stun:stun1.l.google.com:19302"), null, null)
    );

    @Getter
    @Setter
    public static class IceServer {
        private List<String> urls;
        private String username;
        private String credential;

        public IceServer() {}

        public IceServer(List<String> urls, String username, String credential) {
            this.urls = urls;
            this.username = username;
            this.credential = credential;
        }
    }
}

