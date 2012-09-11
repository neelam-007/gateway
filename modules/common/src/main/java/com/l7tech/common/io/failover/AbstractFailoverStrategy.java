package com.l7tech.common.io.failover;


import com.l7tech.util.ConfigFactory;
import com.l7tech.util.TimeSource;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Superclass for implementations of {@link FailoverStrategy}.
 */
public abstract class AbstractFailoverStrategy<ST> implements FailoverStrategy<ST>, Serializable {
    protected final transient ST[] servers;
    @NotNull
    protected transient TimeSource timeSource;

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers  servers to use.  Must not be null or empty.
     */
    protected AbstractFailoverStrategy(ST[] servers) {
        if (servers == null) throw new NullPointerException();
        if (servers.length < 1) throw new IllegalArgumentException("Must be at least one server");
        this.servers = servers;
        this.timeSource = new TimeSource();
    }

    /**
     * Get the configured interval between probes for failing servers.
     *
     * @param defaultProbeMillis The default value to use.
     * @return The probe interval in milliseconds.
     */
    protected long getProbeInterval( final long defaultProbeMillis ) {
        // The system property used to be specific to the round-robin strategy
        // but is now used by other strategies, the name is preserved for
        // backwards compatibility.
        final long interval = ConfigFactory.getLongProperty( "com.l7tech.common.io.failover.robin.retryMillis", defaultProbeMillis );
        return interval == 0 ? defaultProbeMillis : interval;
    }

    public void setTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    abstract public ST selectService();

    @Override
    abstract public void reportFailure(ST service);

    @Override
    public void reportContent(Object content, Feedback feedback) {
    }

    @Override
    abstract public void reportSuccess(ST service);

    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Synchronizes an existing unsynchronized {@link FailoverStrategy}.
     *
     * @param strat the strategy that should be synchronized.  Must not be null.
     * @return strat wrapped in a layer that synchronizes access.  Never null.
     */
    public static <ST> FailoverStrategy<ST> makeSynchronized(final FailoverStrategy<ST> strat) {
        return new FailoverStrategy<ST>() {
            @Override
            public synchronized ST selectService() {
                return strat.selectService();
            }

            @Override
            public synchronized void reportFailure(ST service) {
                strat.reportFailure(service);
            }

            @Override
            public synchronized void reportSuccess(ST service) {
                strat.reportSuccess(service);
            }

            @Override
            public synchronized String getName() {
                return strat.getName();
            }

            @Override
            public synchronized String getDescription() {
                return strat.getDescription();
            }

            @Override
            public synchronized void reportContent(Object content, Feedback feedback) {
                strat.reportContent(content, feedback);
            }
        };
    }
}
