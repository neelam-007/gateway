package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for a list of APIs required because jaxb does not support marshalling of Lists.
 */
@XmlRootElement(name = "ApiKeys", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiKeyListResource extends Resource {
    @XmlElement(name = "ApiKey", namespace = JAXBResourceMarshaller.NAMESPACE)
    private List<ApiKeyResource> apis = new ArrayList<ApiKeyResource>();

    public ApiKeyListResource() {
    }

    public ApiKeyListResource(final List<ApiKeyResource> apis) {
        this.apis = apis;
    }

    public List<ApiKeyResource> getApis() {
        return apis;
    }

    public void setApis(final List<ApiKeyResource> apis) {
        this.apis = apis;
    }

}
