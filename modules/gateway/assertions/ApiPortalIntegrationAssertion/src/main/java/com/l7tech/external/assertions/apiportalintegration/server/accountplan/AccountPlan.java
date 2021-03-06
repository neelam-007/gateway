package com.l7tech.external.assertions.apiportalintegration.server.accountplan;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;

import java.util.*;

/**
 * Represents a portal Account plan.
 * <p/>
 * Name = plan id.
 * <p/>
 * Description = plan name.
 */
public class AccountPlan extends AbstractPortalGenericEntity {
    private Date lastUpdate;
    private boolean defaultPlan;
    private boolean throughputQuotaEnabled;
    private int quota;
    private int timeUnit;
    private int counterStrategy;
    private String ids;
    private String policyXml;
    private boolean rateLimitEnabled;
    private int maxRequestRate;
    private int windowSizeInSeconds;
    private boolean hardLimit;

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        checkLocked();
        this.lastUpdate = lastUpdate;
    }

    public boolean isDefaultPlan() {
        return defaultPlan;
    }

    public void setDefaultPlan(boolean defaultPlan) {
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

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        checkLocked();
        this.ids = ids;
    }

    public String getPolicyXml() {
        return policyXml;
    }

    public void setPolicyXml(String policyXml) {
        checkLocked();
        this.policyXml = policyXml;
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
    public AccountPlan getReadOnlyCopy() {
        final AccountPlan readOnly = new AccountPlan();
        copyBaseFields(this, readOnly);
        readOnly.setName(this.getName());
        readOnly.setDescription(this.getDescription());
        readOnly.setLastUpdate(this.getLastUpdate());
        readOnly.setDefaultPlan(this.isDefaultPlan());
        readOnly.setThroughputQuotaEnabled(this.isThroughputQuotaEnabled());
        readOnly.setQuota(this.getQuota());
        readOnly.setTimeUnit(this.getTimeUnit());
        readOnly.setCounterStrategy(this.getCounterStrategy());
        readOnly.setIds(this.getIds());
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

        AccountPlan accountPlan = (AccountPlan) o;

        if (_name != null ? !_name.equals(accountPlan._name) : accountPlan._name != null) return false;
        if (getDescription() != null ? !getDescription().equals(accountPlan.getDescription()) : accountPlan.getDescription() != null)
            return false;
        if (defaultPlan != accountPlan.defaultPlan) return false;
        if (throughputQuotaEnabled != accountPlan.isThroughputQuotaEnabled()) return false;
        if (rateLimitEnabled != accountPlan.isRateLimitEnabled()) return false;
        if (quota != accountPlan.getQuota()) return false;
        if (timeUnit != accountPlan.getTimeUnit()) return false;
        if (counterStrategy != accountPlan.getCounterStrategy()) return false;
        if (maxRequestRate != accountPlan.getMaxRequestRate()) return false;
        if (windowSizeInSeconds != accountPlan.getWindowSizeInSeconds()) return false;
        if (hardLimit != accountPlan.isHardLimit()) return false;
        if (policyXml != null ? !policyXml.equals(accountPlan.getPolicyXml()) : accountPlan.getPolicyXml() != null) return false;
        if (ids != null && accountPlan.getIds() != null) {
            List idList = Arrays.asList(ids.split(","));
            List otherIdList = Arrays.asList(accountPlan.getIds().split(","));
            if(idList.size() != otherIdList.size()) return false;
            Collections.sort(idList);
            Collections.sort(otherIdList);
            if(!idList.equals(otherIdList)) return false;
        } else if (ids != null ?
                accountPlan.getIds() == null : accountPlan.getIds() != null){
            return false;
        }

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
        result = 31 * result + (ids != null ? ids.hashCode() : 0);
        return result;
    }
}
