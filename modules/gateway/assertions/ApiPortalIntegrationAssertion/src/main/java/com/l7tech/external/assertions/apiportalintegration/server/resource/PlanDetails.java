package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class PlanDetails {
    @XmlElement(name = "ThroughputQuota", namespace = JAXBResourceMarshaller.NAMESPACE)
    private ThroughputQuotaDetails throughputQuota = new ThroughputQuotaDetails();
    @XmlElement(name = "RateLimit", namespace = JAXBResourceMarshaller.NAMESPACE)
    private RateLimitDetails rateLimit = new RateLimitDetails();

    public PlanDetails() {}

    public PlanDetails(final ThroughputQuotaDetails throughputQuota, final RateLimitDetails rateLimit) {
        setThroughputQuota(throughputQuota);
        setRateLimit(rateLimit);
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

    public RateLimitDetails getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitDetails rateLimit) {
        if (rateLimit == null) {
            this.rateLimit = new RateLimitDetails();
        } else {
            this.rateLimit = rateLimit;
        }
    }
}
