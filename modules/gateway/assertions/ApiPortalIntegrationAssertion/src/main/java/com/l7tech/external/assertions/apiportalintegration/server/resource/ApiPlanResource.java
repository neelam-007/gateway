package com.l7tech.external.assertions.apiportalintegration.server.resource;


import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Represents a portal API plan that can be represented by xml.
 */
@XmlRootElement(name = "ApiPlan", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiPlanResource extends Resource {
    @XmlElement(name = "PlanId", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String planId = StringUtils.EMPTY;
    @XmlElement(name = "PlanName", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String planName = StringUtils.EMPTY;
    @XmlElement(name = "LastUpdate", namespace = JAXBResourceMarshaller.NAMESPACE)
    @XmlSchemaType(name = "timestamp")
    private Date lastUpdate;
    @XmlElement(name = "PlanPolicy", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String policyXml = StringUtils.EMPTY;
    @XmlElement(name = "DefaultPlan", namespace = JAXBResourceMarshaller.NAMESPACE)
    private boolean defaultPlan;

    public ApiPlanResource(final String planId, final String planName, final Date lastUpdate, final String policyXml, final boolean defaultPlan) {
        setPlanId(planId);
        setPlanName(planName);
        setLastUpdate(lastUpdate);
        setPolicyXml(policyXml);
        setDefaultPlan(defaultPlan);
    }

    public ApiPlanResource() {

    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(final String planId) {
        if (planId != null) {
            this.planId = planId;
        } else {
            this.planId = StringUtils.EMPTY;
        }
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(final String planName) {
        if (planName != null) {
            this.planName = planName;
        } else {
            this.planName = StringUtils.EMPTY;
        }
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getPolicyXml() {
        return policyXml;
    }

    public void setPolicyXml(final String policyXml) {
        if (policyXml != null) {
            this.policyXml = policyXml;
        } else {
            this.policyXml = StringUtils.EMPTY;
        }
    }

    public boolean isDefaultPlan() {
        return defaultPlan;
    }

    public void setDefaultPlan(final boolean defaultPlan) {
        this.defaultPlan = defaultPlan;
    }
}
