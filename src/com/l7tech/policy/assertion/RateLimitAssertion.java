package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.ExpandVariables;

/**
 * Adds rate limiting to a policy.
 * See http://sarek.l7tech.com/mediawiki/index.php?title=POLM_1410_Rate_Limiting 
 */
public class RateLimitAssertion extends Assertion implements UsesVariables {
    private String counterName = "RateLimit-${request.clientid}";
    private int maxRequestsPerSecond = 100;
    private boolean shapeRequests = false;
    private int maxConcurrency = 0;

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
}
