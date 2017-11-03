package com.l7tech.server.transport.jms2;

public class JmsResourceManagerConfig {
    private final long maximumAge;
    private final long maximumIdleTime;
    private final long idleTime;
    private final int maximumSize;
    private final int connectionPoolSize;
    private final int connectionMinIdle;
    private final long connectionMaxWait;
    private final long timeBetweenEviction;
    private final int evictionBatchSize;
    private final int sessionPoolSize;
    private final int sessionMaxIdle;
    private final long sessionMaxWait;

    public JmsResourceManagerConfig(final long maximumAge, final long maximumIdleTime, final long idleTime, final int maximumSize,
                                    final int connectionPoolSize, final int connectionMinIdle,
                                    final long connectionMaxWait, final long timeBetweenEviction, final int evictionBatchSize,
                                    final int sessionPoolSize, final int sessionMaxIdle, final long sessionMaxWait) {
        this.maximumAge = maximumAge;
        this.maximumIdleTime = maximumIdleTime;
        this.idleTime = idleTime;
        this.maximumSize = maximumSize;
        this.connectionPoolSize = connectionPoolSize;
        this.connectionMinIdle = connectionMinIdle;
        this.connectionMaxWait = connectionMaxWait;
        this.timeBetweenEviction = timeBetweenEviction;
        this.evictionBatchSize = evictionBatchSize;
        this.sessionPoolSize = sessionPoolSize;
        this.sessionMaxIdle = sessionMaxIdle;
        this.sessionMaxWait = sessionMaxWait;
    }

    public JmsResourceManagerConfig(final long maximumAge, final long maximumIdleTime, final int maximumSize) {
        this(maximumAge, maximumIdleTime, 0, maximumSize, 0, 0, 0L, 0L, 0, 0, 0, 0L);
    }

    public long getMaximumAge() {
        return maximumAge;
    }

    public long getMaximumIdleTime() {
        return maximumIdleTime;
    }

    public long getIdleTime() {
        return idleTime;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public long getConnectionMaxWait() {
        return connectionMaxWait;
    }

    public long getTimeBetweenEviction() {
        return timeBetweenEviction;
    }

    public int getEvictionBatchSize() {
        return evictionBatchSize;
    }

    public int getSessionPoolSize() {
        return sessionPoolSize;
    }

    public int getSessionMaxIdle() {
        return sessionMaxIdle;
    }

    public long getSessionMaxWait() {
        return sessionMaxWait;
    }

    public int getConnectionMinIdle() {
        return connectionMinIdle;
    }
}
