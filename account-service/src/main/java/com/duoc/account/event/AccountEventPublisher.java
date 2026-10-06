package com.duoc.account.event;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class AccountEventPublisher {
    private final StreamBridge bridge;

    public AccountEventPublisher(StreamBridge bridge) {
        this.bridge = bridge;
    }

    public void publish(String event) {
        bridge.send("accountEvents-out-0", event);
    }
}
