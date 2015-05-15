package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class RateLimitDetails {
    @XmlAttribute(name = "enabled", namespace = JAXBResourceMarshaller.NAMESPACE)
    private boolean enabled;
    @XmlElement(name = "MaxRequestRate", namespace = JAXBResourceMarshaller.NAMESPACE)
    private int maxRequestRate;
    @XmlElement(name = "WindowSizeInSeconds", namespace = JAXBResourceMarshaller.NAMESPACE)
    private int windowSizeInSeconds;
    @XmlElement(name = "HardLimit", namespace = JAXBResourceMarshaller.NAMESPACE)
    private boolean hardLimit;

    public RateLimitDetails() {}

    public RateLimitDetails(final boolean enabled, final int maxRequestRate, final int windowSizeInSeconds, final boolean hardLimit) {
        this.enabled = enabled;
        this.maxRequestRate = maxRequestRate;
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.hardLimit = hardLimit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRequestRate() {
        return maxRequestRate;
    }

    public void setMaxRequestRate(int maxRequestRate) {
        this.maxRequestRate = maxRequestRate;
    }

    public int getWindowSizeInSeconds() {
        return windowSizeInSeconds;
    }

    public void setWindowSizeInSeconds(int windowSizeInSeconds) {
        this.windowSizeInSeconds = windowSizeInSeconds;
    }

    public boolean isHardLimit() {
        return hardLimit;
    }

    public void setHardLimit(boolean hardLimit) {
        this.hardLimit = hardLimit;
    }
}