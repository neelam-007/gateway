package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class GatewayStatResource {
    @XmlElement(name = "Count", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String count = StringUtils.EMPTY;
    @XmlElement(name = "CacheItems", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String cacheItems = StringUtils.EMPTY;

    public GatewayStatResource() {

    }

    public GatewayStatResource(final String count, final String cacheItems) {
        setCount(count);
        setCacheItems(cacheItems);
    }

    public String getCount() {
        return count;
    }

    public void setCount(final String count) {
        if (count != null) {
            this.count = count;
        } else {
            this.count = StringUtils.EMPTY;
        }
    }

    public String getCacheItems() {
        return cacheItems;
    }

    public void setCacheItems(final String cacheItems) {
        if (cacheItems != null) {
            this.cacheItems = cacheItems;
        } else {
            this.cacheItems = StringUtils.EMPTY;
        }
    }
}