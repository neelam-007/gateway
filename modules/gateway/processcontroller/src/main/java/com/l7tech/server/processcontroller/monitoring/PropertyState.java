/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.util.Pair;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Runtime state for a MonitorableProperty.
 *
 * @param <V> the type of the sampled property values
 */
public class PropertyState<V extends Serializable & Comparable> extends MonitorState<MonitorableProperty> {
    private final NavigableMap<Long, Pair<V, PropertySamplingException>> lastSamples;
    private final TimerTask task;
    private final long samplingInterval;
    private final PropertySampler<V> sampler;

    private volatile Pair<Long, V> lastSample;
    private volatile Pair<Long, PropertySamplingException> lastFailure;

    /**
     * Create a new PropertyState with no history
     */
    protected PropertyState(MonitorableProperty property, String componentId, long samplingInterval, final PropertySampler<V> sampler) {
        super(property, componentId);
        this.lastSamples = new ConcurrentSkipListMap<Long,Pair<V, PropertySamplingException>>();
        this.samplingInterval = samplingInterval;
        this.sampler = sampler;
        this.task = new TimerTask() {
            @Override
            public void run() {
                final long now = System.currentTimeMillis();
                try {
                    addSample(sampler.sample(), now);
                } catch (PropertySamplingException e) {
                    addFailure(e, now);
                }
            }
        };
    }

    protected PropertyState(PropertyState<V> oldState, long newSamplingInterval) {
        // TODO can the sampler itself ever change its configuration?
        this(oldState.getMonitorable(), oldState.getComponentId(), Math.min(newSamplingInterval, oldState.getSamplingInterval()), oldState.getSampler());
        lastSamples.putAll(oldState.lastSamples);
    }


    protected void addSample(V value, Long when) {
        if (when == null) throw new IllegalArgumentException();
        lastSample = new Pair<Long,V>(when,  value);
        lastSamples.put(when, new Pair<V, PropertySamplingException>(value, null));
    }

    protected void addFailure(PropertySamplingException throwable, Long when) {
        if (when == null) throw new IllegalArgumentException();
        lastFailure = new Pair<Long, PropertySamplingException>(when, throwable);
        lastSamples.put(when, new Pair<V, PropertySamplingException>(null, throwable));
    }

    /**
     * Gets the samples that are available in the given time range.
     *
     * @param onOrAfter the earliest time for which to retrieve samples (inclusive)
     * @param before the time before which to retrieve samples (exclusive)
     */
    Map<Long, Pair<V, PropertySamplingException>> getSamples(Long onOrAfter, Long before) {
        return Collections.unmodifiableMap(lastSamples.subMap(onOrAfter, before));
    }

    @Override
    void expireHistory(Long retainNewerThan) {
        lastSamples.headMap(retainNewerThan).clear();
    }

    public long getSamplingInterval() {
        return samplingInterval;
    }

    public PropertySampler<V> getSampler() {
        return sampler;
    }

    public Pair<Long, V> getLastSample() {
        return lastSample;
    }

    public Pair<Long, PropertySamplingException> getLastFailure() {
        return lastFailure;
    }

    public TimerTask getTask() {
        return task;
    }
}
