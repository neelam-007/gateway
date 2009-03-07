/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.api.monitoring.MonitorableEvent;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class EventState extends MonitorState<MonitorableEvent> {
    // TODO is there some kind of payload for the event...?
    private final NavigableSet<Long> eventHistory;

    protected EventState(MonitorableEvent monitorable, String componentId) {
        super(monitorable, componentId);
        this.eventHistory = new ConcurrentSkipListSet<Long>();
    }

    public Set<Long> getEvents(Long from, Long to) {
        return Collections.unmodifiableSet(eventHistory.subSet(from, to));
    }

    void addEvent(Long timestamp) {
        eventHistory.add(timestamp);
    }

    @Override
    void expireHistory(Long retainNewerThan) {
        eventHistory.headSet(retainNewerThan).clear();
    }

    @Override
    public void close() { }
}
