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
@XmlRootElement(name = "Apis", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiListResource extends Resource {
    @XmlElement(name = "Api", namespace = JAXBResourceMarshaller.NAMESPACE)
    private List<ApiResource> apis = new ArrayList<ApiResource>();

    public ApiListResource() {
    }

    public ApiListResource(final List<ApiResource> apis) {
        this.apis = apis;
    }

    public List<ApiResource> getApis() {
        return apis;
    }

    public void setApis(final List<ApiResource> apis) {
        this.apis = apis;
    }

}
