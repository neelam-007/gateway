package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "AccountPlans", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class AccountPlanListResource extends Resource{
    @XmlElement(name = "AccountPlan", namespace = JAXBResourceMarshaller.NAMESPACE)
    private List<AccountPlanResource> accountPlans = new ArrayList<AccountPlanResource>();

    public AccountPlanListResource() {}

    public AccountPlanListResource(final List<AccountPlanResource> accountPlans) {
        setAccountPlans(accountPlans);
    }

    public List<AccountPlanResource> getAccountPlans() {
        return accountPlans;
    }

    public void setAccountPlans(List<AccountPlanResource> accountPlans) {
        this.accountPlans = accountPlans;
    }
}
