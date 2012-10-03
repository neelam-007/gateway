package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for a list of API plans because jaxb does not support marshalling of Lists.
 */
@XmlRootElement(name = "ApiPlans", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiPlanListResource extends Resource {
    @XmlElement(name = "ApiPlan", namespace = JAXBResourceMarshaller.NAMESPACE)
    private List<ApiPlanResource> apiPlans = new ArrayList<ApiPlanResource>();

    public ApiPlanListResource(final List<ApiPlanResource> apiPlans) {
        this.apiPlans = apiPlans;
    }

    public ApiPlanListResource() {

    }

    public List<ApiPlanResource> getApiPlans() {
        return apiPlans;
    }

    public void setApiPlans(final List<ApiPlanResource> apiPlans) {
        this.apiPlans = apiPlans;
    }
}
