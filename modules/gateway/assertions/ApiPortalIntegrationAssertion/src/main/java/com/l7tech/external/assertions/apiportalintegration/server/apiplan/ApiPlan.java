package com.l7tech.external.assertions.apiportalintegration.server.apiplan;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;

import javax.persistence.Transient;
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

    @Transient
    public ApiPlan getReadOnlyCopy() {
        final ApiPlan readOnly = new ApiPlan();
        copyBaseFields(this, readOnly);
        readOnly.setLastUpdate(this.getLastUpdate());
        readOnly.setPolicyXml(this.getPolicyXml());
        readOnly.setDefaultPlan(this.isDefaultPlan());
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

        return true;
    }

    @Override
    public int hashCode() {
        int result = 31 + (_name != null ? _name.hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (policyXml != null ? policyXml.hashCode() : 0);
        result = 31 * result + (defaultPlan ? 1 : 0);
        return result;
    }
}
