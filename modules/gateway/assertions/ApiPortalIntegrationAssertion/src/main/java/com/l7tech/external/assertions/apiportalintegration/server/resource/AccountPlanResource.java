package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.*;
import java.util.Date;

/**
 * A portal API key which has an xml representation.
 */
@XmlRootElement(name = "AccountPlan", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class AccountPlanResource extends Resource {
    @XmlElement(name = "PlanId", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String planId = StringUtils.EMPTY;
    @XmlElement(name = "PlanName", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String planName = StringUtils.EMPTY;
    @XmlElement(name = "LastUpdated", namespace = JAXBResourceMarshaller.NAMESPACE)
    @XmlSchemaType(name = "timestamp")
    private Date lastUpdate;
    @XmlElement(name = "DefaultPlan", namespace = JAXBResourceMarshaller.NAMESPACE)
    private boolean defaultPlan;
    @XmlElement(name = "PlanDetails", namespace = JAXBResourceMarshaller.NAMESPACE)
    private PlanDetails planDetails = new PlanDetails();
    @XmlElement(name = "PlanMapping", namespace = JAXBResourceMarshaller.NAMESPACE)
    private AccountPlanMapping planMapping = new AccountPlanMapping();
    @XmlElement(name = "PlanPolicy", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String policyXml = StringUtils.EMPTY;

    public AccountPlanResource() {}

    public AccountPlanResource(final String planId, final String planName, final Date lastUpdate,
                               final boolean defaultPlan, final PlanDetails planDetails,
                               final AccountPlanMapping planMapping, final String policyXml) {
        setPlanId(planId);
        setPlanName(planName);
        setLastUpdate(lastUpdate);
        setDefaultPlan(defaultPlan);
        setPlanDetails(planDetails);
        setPlanMapping(planMapping);
        setPolicyXml(policyXml);
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

    public void setPlanName(String planName) {
        if(planName == null) {
            this.planName = StringUtils.EMPTY;
        } else {
            this.planName = planName;
        }
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isDefaultPlan() {
        return defaultPlan;
    }

    public void setDefaultPlan(boolean defaultPlan) {
        this.defaultPlan = defaultPlan;
    }

    public PlanDetails getPlanDetails() {
        return planDetails;
    }

    public void setPlanDetails(PlanDetails planDetails) {
        if(planDetails == null) {
            this.planDetails = new PlanDetails();
        } else {
            this.planDetails = planDetails;
        }
    }

    public AccountPlanMapping getPlanMapping() {
        return planMapping;
    }

    public void setPlanMapping(AccountPlanMapping planMapping) {
        if(planMapping == null) {
            this.planMapping = new AccountPlanMapping();
        } else {
            this.planMapping = planMapping;
        }
    }

    public String getPolicyXml() {
        return policyXml;
    }

    public void setPolicyXml(String policyXml) {
        if(policyXml == null) {
            this.policyXml = StringUtils.EMPTY;
        } else {
            this.policyXml = policyXml;
        }
    }
}
