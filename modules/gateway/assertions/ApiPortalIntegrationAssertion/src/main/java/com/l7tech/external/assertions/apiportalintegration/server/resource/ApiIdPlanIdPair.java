package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents a service id - planId pair - required for xml marshalling/unmarshalling.
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ApiIdPlanIdPair {
    @XmlAttribute(name = "apiId")
    private String apiId = StringUtils.EMPTY;
    @XmlAttribute(name = "planId")
    private String planId = StringUtils.EMPTY;

    public ApiIdPlanIdPair(final String apiId, final String planId) {
        setApiId(apiId);
        setPlanId(planId);
    }

    public ApiIdPlanIdPair() {

    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(final String apiId) {
        if (apiId != null) {
            this.apiId = apiId;
        } else {
            this.apiId = StringUtils.EMPTY;
        }
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

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
