package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for a list of ApiFragments required because jaxb does not support marshalling of Lists.
 */
@XmlRootElement(name = "ApiFragments", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiFragmentListResource extends Resource {
    @XmlElement(name = "ApiFragment", namespace = JAXBResourceMarshaller.NAMESPACE)
    private List<ApiFragmentResource> apiFragments = new ArrayList<ApiFragmentResource>();

    public ApiFragmentListResource() {
    }

    public ApiFragmentListResource(final List<ApiFragmentResource> apiFragments) {
        this.apiFragments = apiFragments;
    }

    public List<ApiFragmentResource> getApiFragments() {
        return apiFragments;
    }

    public void setApiFragments(final List<ApiFragmentResource> apiFragments) {
        this.apiFragments = apiFragments;
    }

}