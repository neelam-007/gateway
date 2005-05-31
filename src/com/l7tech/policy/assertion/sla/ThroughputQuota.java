/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.policy.assertion.sla;

import com.l7tech.policy.assertion.Assertion;

/**
 * An assertion that enforce the number of requests that can be made to a particular resource per time unit.
 * A quota can be defined as global or per requestor.
 *
 * For each runtime request if two quota assertions are invoked and use the same counter name, only the first will
 * increment the counter but both quota assertions will assert their quota based on the current value of the counter.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuota extends Assertion {
    public static final int TIME_UNIT_UNDEFINED = 0;
    public static final int PER_SECOND = 1;
    public static final int PER_HOUR = 2;
    public static final int PER_DAY = 3;
    public static final int PER_MONTH = 4;
    private long quota = 200;
    private boolean global = false;
    private int timeUnit = PER_MONTH;
    private String counterName = "";
    public static final int ALWAYS_INCREMENT = 1;
    public static final int INCREMENT_ON_SUCCESS = 2;
    public static final int DECREMENT = 3;
    private int counterStrategy = INCREMENT_ON_SUCCESS;

    public ThroughputQuota() {}

    /**
     * The quota property represents the maximum number of requests that are allowed at run time per timeUnit.
     * @return the quota
     */
    public long getQuota() {
        return quota;
    }

    /**
     * The quota property represents the maximum number of requests that are allowed at run time per timeUnit.
     * @param quota the quota
     */
    public void setQuota(long quota) {
        this.quota = quota;
    }

    /**
     * Whether or not this quota is enforce on a per requestor basis or for all requestors together.
     * @return true means quota applies to all requestors together, false means it's applied to each
     * requestor individually
     */
    public boolean isGlobal() {
        return global;
    }

    /**
     * Set whether or not this quota is enforce on a per requestor basis or for all requestors together.
     * @param global true means quota applies to all requestors together, false means it's applied
     * to each requestor individually
     */
    public void setGlobal(boolean global) {
        this.global = global;
    }

    /**
     * The time unit applicable to the quota.
     * @return TIME_UNIT_UNDEFINED, PER_SECOND, PER_HOUR, PER_DAY or PER_MONTH
     */
    public int getTimeUnit() {
        return timeUnit;
    }

    /**
     * The time unit applicable to the quota.
     * @param timeUnit TIME_UNIT_UNDEFINED, PER_SECOND, PER_HOUR, PER_DAY or PER_MONTH
     */
    public void setTimeUnit(int timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * For each runtime request if two quota assertions are invoked and use the same counter name, only the first
     * will increment the counter but both quota assertions will assert their quota based on the current value
     * of the counter.
     *
     * @return the name for this counter
     */
    public String getCounterName() {
        return counterName;
    }

    /**
     * For each runtime request if two quota assertions are invoked and use the same counter name, only the first
     * will increment the counter but both quota assertions will assert their quota based on the current value
     * of the counter.
     *
     * @param counterName the name for this counter
     */
    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public int getCounterStrategy() {
        return counterStrategy;
    }

    public void setCounterStrategy(int counterStrategy) {
        this.counterStrategy = counterStrategy;
    }
}
