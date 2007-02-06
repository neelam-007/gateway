package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.ExpandVariables;

import java.util.Map;
import java.util.HashMap;

/**
 * Adds rate limiting to a policy.
 * See http://sarek.l7tech.com/mediawiki/index.php?title=POLM_1410_Rate_Limiting 
 */
public class RateLimitAssertion extends Assertion implements UsesVariables {
    private String counterName = "RateLimit-${request.clientid}";
    private int maxRequestsPerSecond = 100;
    private boolean shapeRequests = false;
    private int maxConcurrency = 0;
    private boolean hardLimit = false;

    public String[] getVariablesUsed() {
        return ExpandVariables.getReferencedNames(getCounterName());
    }

    /**
     * @return the counter name.  Never null.
     */
    public String getCounterName() {
        return counterName;
    }

    /**
     * @param counterName the new counter name.  Must not be null.
     */
    public void setCounterName(String counterName) {
        if (counterName == null) throw new IllegalArgumentException();
        this.counterName = counterName;
    }

    public int getMaxRequestsPerSecond() {
        return maxRequestsPerSecond;
    }

    /** @param maxRequestsPerSecond the rate limit to enforce.  Must be positive. */
    public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
        if (maxRequestsPerSecond < 1) throw new IllegalArgumentException();
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    public boolean isShapeRequests() {
        return shapeRequests;
    }

    /** @param shapeRequests if true, assertion should attempt to delay requests to keep them from exceeding the limit. */
    public void setShapeRequests(boolean shapeRequests) {
        this.shapeRequests = shapeRequests;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * @param maxConcurrency maximum number of requests that may be in progress at once with this counter,
     *                       or zero for no special limit.  Must be nonnegative.
     */
    public void setMaxConcurrency(int maxConcurrency) {
        if (maxConcurrency < 0) throw new IllegalArgumentException();
        this.maxConcurrency = maxConcurrency;
    }

    public boolean isHardLimit() {
        return hardLimit;
    }

    /**
     * Set whether the limit is hard or soft.
     * <p/>
     * A hard limit will reject (or delay) a second message that arrives
     * too soon after a first message, even if the counter had been sitting idle for a long time before them.
     * Thus, a hard limit prevents burst rate from exceeding the limit.
     * <p/>
     * A soft limit will allow an idle counter to accumulate up to one seconds worth of "buffer", and will then
     * allow burst traffic at full speed until this buffer is exhausted.
     * Thus, a soft limit puts a lid of requests over time, but may allow burst traffic to exceed the limit temporarily
     * (as long as the counter was idle for a period of time before the burst).
     *
     * @param hardLimit if true, no burst traffic above the limit will be permitted.
     *                  if false, up to one second's worth of traffic may be bursted before rate limiting is imposed
     */
    public void setHardLimit(boolean hardLimit) {
        this.hardLimit = hardLimit;
    }

    // Metadata

    public static final String PARAM_MAX_QUEUED_THREADS = "ratelimitMaxQueuedThreads";
    public static final String PARAM_CLEANER_PERIOD = "ratelimitCleanerPeriod";
    public static final String PARAM_MAX_NAP_TIME = "ratelimitMaxNapTime";
    public static final String PARAM_MAX_TOTAL_SLEEP_TIME = "ratelimitMaxTotalSleepTime";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put("ratelimit.maxQueuedThreads", new String[] {
                "Maximum number of requests that can be delayed for traffic shaping purposes on a single node.  When this limit is reached, rate limiters will start failing requests that hit the limit",
                "70"
        });
        props.put("ratelimit.cleanerPeriod", new String[] {
                "Time interval for removing rate limit counters that have not been used recently (Milliseconds)",
                "13613"
        });
        props.put("ratelimit.maxNapTime", new String[] {
                "Maximum time a request subject to traffic shaping will wait before awaking to check its status (Milliseconds)",
                "4703"
        });
        props.put("ratelimit.maxTotalSleepTime", new String[] {
                "Maximum total time a request subject to traffic shaping will wait before giving up and failing (Milliseconds)",
                "18371"
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        return meta;
    }
}
