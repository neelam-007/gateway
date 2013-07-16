package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class PlanDetails {
    @XmlElement(name = "ThroughputQuota", namespace = JAXBResourceMarshaller.NAMESPACE)
    private ThroughputQuotaDetails throughputQuota = new ThroughputQuotaDetails();

    public PlanDetails() {}

    public PlanDetails(final ThroughputQuotaDetails throughputQuota) {
        setThroughputQuota(throughputQuota);
    }

    public ThroughputQuotaDetails getThroughputQuota() {
        return throughputQuota;
    }

    public void setThroughputQuota(ThroughputQuotaDetails throughputQuota) {
        if(throughputQuota == null) {
            this.throughputQuota= new ThroughputQuotaDetails();
        } else {
            this.throughputQuota = throughputQuota;
        }
    }
}
