package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a portal API that can be represented by xml.
 */
@XmlRootElement(name = "Api", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiResource extends Resource {
    @XmlElement(name = "ApiId", namespace = JAXBResourceMarshaller.NAMESPACE, nillable = true)
    private String apiId = StringUtils.EMPTY;
    @XmlElement(name = "ApiGroup", namespace = JAXBResourceMarshaller.NAMESPACE, nillable = true)
    private String apiGroup = StringUtils.EMPTY;
    @XmlElement(name = "ServiceOID", namespace = JAXBResourceMarshaller.NAMESPACE, nillable = true)
    private String serviceOid = StringUtils.EMPTY;

    public ApiResource() {
    }

    public ApiResource(final String apiId, final String apiGroup, final String serviceOid) {
        setApiId(apiId);
        setApiGroup(apiGroup);
        setServiceOid(serviceOid);
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

    public String getApiGroup() {
        return apiGroup;
    }

    public void setApiGroup(final String apiGroup) {
        if (apiGroup != null) {
            this.apiGroup = apiGroup;
        } else {
            this.apiGroup = StringUtils.EMPTY;
        }
    }

    public String getServiceOid() {
        return serviceOid;
    }

    public void setServiceOid(final String serviceOid) {
        if (serviceOid != null) {
            this.serviceOid = serviceOid;
        } else {
            this.serviceOid = StringUtils.EMPTY;
        }
    }
}
