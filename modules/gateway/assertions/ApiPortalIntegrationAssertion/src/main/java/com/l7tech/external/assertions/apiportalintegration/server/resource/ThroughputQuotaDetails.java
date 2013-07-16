package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class ThroughputQuotaDetails {
    @XmlAttribute(name = "enabled", namespace = JAXBResourceMarshaller.NAMESPACE)
    private boolean enabled;
    @XmlElement(name = "Quota", namespace = JAXBResourceMarshaller.NAMESPACE)
    private int quota;
    @XmlElement(name = "TimeUnit", namespace = JAXBResourceMarshaller.NAMESPACE)
    private int timeUnit;
    @XmlElement(name = "CounterStrategy", namespace = JAXBResourceMarshaller.NAMESPACE)
    private int counterStrategy;

    public ThroughputQuotaDetails() {}

    public ThroughputQuotaDetails(final boolean enabled, final int quota, final int timeUnit, final int counterStrategy) {
        setEnabled(enabled);
        setQuota(quota);
        setTimeUnit(timeUnit);
        setCounterStrategy(counterStrategy);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        this.quota = quota;
    }

    public int getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(int timeUnit) {
        this.timeUnit = timeUnit;
    }

    public int getCounterStrategy() {
        return counterStrategy;
    }

    public void setCounterStrategy(int counterStrategy) {
        this.counterStrategy = counterStrategy;
    }
}
