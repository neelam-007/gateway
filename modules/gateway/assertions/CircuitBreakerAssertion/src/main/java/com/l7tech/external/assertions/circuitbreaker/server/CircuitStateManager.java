package com.l7tech.external.assertions.circuitbreaker.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a history of state for Gateway circuits.
 * @author Ekta Khandelwal - khaek01@ca.com
 */
public class CircuitStateManager {
    private final ConcurrentHashMap<CircuitConfig, Long> circuitConfigState;

    public CircuitStateManager() {
        circuitConfigState = new ConcurrentHashMap();
    }

    public void addState(final CircuitConfig config, final long timeStamp) {
        circuitConfigState.put(config, timeStamp);
    }

    public Long getState(final CircuitConfig config) {
        return circuitConfigState.get(config);
    }
}
