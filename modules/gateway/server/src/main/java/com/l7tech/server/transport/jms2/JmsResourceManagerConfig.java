package com.l7tech.server.transport.jms2;

public class JmsResourceManagerConfig {
    private final long maximumAge;
    private final long maximumIdleTime;
    private final int maximumSize;
    private final int defaultPoolSize;
    private final long defaultWait;
    private final long timeBetweenEviction;
    private final int defaultEvictionBatchSize;

    public JmsResourceManagerConfig(final long maximumAge, final long maximumIdleTime, final int maximumSize,
                                    final int defaultPoolSize, final long defaultWait, final long timeBetweenEviction, final int defaultEvictionBatchSize) {
        this.maximumAge = maximumAge;
        this.maximumIdleTime = maximumIdleTime;
        this.maximumSize = maximumSize;
        this.defaultPoolSize = defaultPoolSize;
        this.defaultWait = defaultWait;
        this.timeBetweenEviction = timeBetweenEviction;
        this.defaultEvictionBatchSize = defaultEvictionBatchSize;
    }

    public long getMaximumAge() {
        return maximumAge;
    }

    public long getMaximumIdleTime() {
        return maximumIdleTime;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public int getDefaultPoolSize() {
        return defaultPoolSize;
    }

    public long getDefaultWait() {
        return defaultWait;
    }

    public long getTimeBetweenEviction() {
        return timeBetweenEviction;
    }

    public int getDefaultEvictionBatchSize() {
        return defaultEvictionBatchSize;
    }
}
