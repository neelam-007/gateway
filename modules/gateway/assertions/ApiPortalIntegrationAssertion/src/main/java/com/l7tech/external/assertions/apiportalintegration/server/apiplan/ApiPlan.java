package com.l7tech.external.assertions.apiportalintegration.server.apiplan;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;

import java.util.Date;

/**
 * Represents a portal API plan.
 * <p/>
 * Name = plan id.
 * <p/>
 * Description = plan name.
 */
public class ApiPlan extends AbstractPortalGenericEntity {
    private Date lastUpdate;
    private String policyXml;
    private boolean defaultPlan;

    private boolean throughputQuotaEnabled;
    private int quota;

    /**
    * TimeUnit type in integer.
    * <p/>
    * TimeUnit type in integer as input to {@link ThroughputQuotaAssertion}.
    * Types are defined in the {@link ThroughputQuotaAssertion} as
    * PER_SECOND = 1
    * PER_MINUTE = 2
    * PER_HOUR = 3
    * PER_DAY = 4
    * PER_MONTH = 5
    * <p/>
    */
    private int timeUnit;

    /**
    * Counter strategy type in integer.
    * <p/>
    * Counter strategy type in integer as input to {@link ThroughputQuotaAssertion}.
    * Types are defined in the {@link ThroughputQuotaAssertion} as
    * ALWAYS_INCREMENT = 1
    * INCREMENT_ON_SUCCESS = 2
    * DECREMENT = 3
    * RESET = 4
    * <p/>
    */
    private int counterStrategy;

    private boolean rateLimitEnabled;
    private int maxRequestRate;
    private int windowSizeInSeconds;
    private boolean hardLimit;


    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final Date lastUpdate) {
        checkLocked();
        this.lastUpdate = lastUpdate;
    }

    public String getPolicyXml() {
        return policyXml;
    }

    public void setPolicyXml(final String policyXml) {
        checkLocked();
        this.policyXml = policyXml;
    }

    public boolean isDefaultPlan() {
        return defaultPlan;
    }

    public void setDefaultPlan(final boolean defaultPlan) {
        checkLocked();
        this.defaultPlan = defaultPlan;
    }

    public boolean isThroughputQuotaEnabled() {
        return throughputQuotaEnabled;
    }

    public void setThroughputQuotaEnabled(boolean throughputQuotaEnabled) {
        checkLocked();
        this.throughputQuotaEnabled = throughputQuotaEnabled;
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        checkLocked();
        this.quota = quota;
    }

    public int getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(int timeUnit) {
        checkLocked();
        this.timeUnit = timeUnit;
    }

    public int getCounterStrategy() {
        return counterStrategy;
    }

    public void setCounterStrategy(int counterStrategy) {
        checkLocked();
        this.counterStrategy = counterStrategy;
    }

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        checkLocked();
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public int getMaxRequestRate() {
        return maxRequestRate;
    }

    public void setMaxRequestRate(int maxRequestRate) {
        checkLocked();
        this.maxRequestRate = maxRequestRate;
    }

    public int getWindowSizeInSeconds() {
        return windowSizeInSeconds;
    }

    public void setWindowSizeInSeconds(int windowSizeInSeconds) {
        checkLocked();
        this.windowSizeInSeconds = windowSizeInSeconds;
    }

    public boolean isHardLimit() {
        return hardLimit;
    }

    public void setHardLimit(boolean hardLimit) {
        checkLocked();
        this.hardLimit = hardLimit;
    }


    @Override
    public ApiPlan getReadOnlyCopy() {
        final ApiPlan readOnly = new ApiPlan();
        copyBaseFields(this, readOnly);
        readOnly.setName(this.getName());
        readOnly.setDescription(this.getDescription());
        readOnly.setLastUpdate(this.getLastUpdate());
        readOnly.setPolicyXml(this.getPolicyXml());
        readOnly.setDefaultPlan(this.isDefaultPlan());
        readOnly.setThroughputQuotaEnabled(this.isThroughputQuotaEnabled());
        readOnly.setQuota(this.getQuota());
        readOnly.setTimeUnit(this.getTimeUnit());
        readOnly.setCounterStrategy(this.getCounterStrategy());
        readOnly.setPolicyXml(this.getPolicyXml());
        readOnly.setRateLimitEnabled(this.isRateLimitEnabled());
        readOnly.setHardLimit(this.isHardLimit());
        readOnly.setWindowSizeInSeconds(this.getWindowSizeInSeconds());
        readOnly.setMaxRequestRate(this.getMaxRequestRate());
        readOnly.lock();
        return readOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiPlan apiPlan = (ApiPlan) o;

        if (_name != null ? !_name.equals(apiPlan._name) : apiPlan._name != null) return false;
        if (getDescription() != null ? !getDescription().equals(apiPlan.getDescription()) : apiPlan.getDescription() != null)
            return false;
        if (policyXml != null ? !policyXml.equals(apiPlan.policyXml) : apiPlan.policyXml != null) return false;
        if (defaultPlan != apiPlan.defaultPlan) return false;
        if (!(throughputQuotaEnabled == apiPlan.throughputQuotaEnabled && rateLimitEnabled == apiPlan.rateLimitEnabled 
            && quota == apiPlan.quota && timeUnit == apiPlan.timeUnit && counterStrategy == apiPlan.counterStrategy
            && maxRequestRate == apiPlan.maxRequestRate && windowSizeInSeconds == apiPlan.windowSizeInSeconds
            && hardLimit == apiPlan.hardLimit)) 
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 31 + (_name != null ? _name.hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (policyXml != null ? policyXml.hashCode() : 0);
        result = 31 * result + (defaultPlan ? 1 : 0);
        result = 31 * result + (throughputQuotaEnabled ? 1: 0);
        result = 31 * result + (rateLimitEnabled ? 1: 0);
        result = 31 * result + quota;
        result = 31 * result + timeUnit;
        result = 31 * result + maxRequestRate;
        result = 31 * result + windowSizeInSeconds;
        result = 31 * result + (hardLimit ? 1: 0);

        return result;
    }
}
