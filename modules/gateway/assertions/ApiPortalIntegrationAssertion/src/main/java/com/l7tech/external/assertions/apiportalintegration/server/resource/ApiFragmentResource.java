package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a Api Fragment (portal managed encass) that can be represented by xml.
 */
@XmlRootElement(name = "ApiFragment", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiFragmentResource extends Resource {
    @XmlElement(name = "EncapsulatedAssertionGuid", namespace = JAXBResourceMarshaller.NAMESPACE, nillable = true)
    private String encassGuid = StringUtils.EMPTY;
    @XmlElement(name = "EncapsulatedAssertionId", namespace = JAXBResourceMarshaller.NAMESPACE, nillable = true)
    private String encassId = StringUtils.EMPTY;
    @XmlElement(name = "FragmentDetails", namespace = JAXBResourceMarshaller.NAMESPACE, nillable = true)
    private FragmentDetails fragmentDetails = new FragmentDetails();

    public ApiFragmentResource() {
    }

    public ApiFragmentResource(final String encassGuid, final String encassId, final String hasRouting, final String parsedPolicyDetails) {
        setEncassGuid(encassGuid);
        setEncassId(encassId);
        fragmentDetails.setHasRouting(hasRouting);
        fragmentDetails.setParsedPolicyDetails(parsedPolicyDetails);
    }

    public String getEncassGuid() {
        return encassGuid;
    }

    public void setEncassGuid(final String encassGuid) {
        if (encassGuid != null) {
            this.encassGuid = encassGuid;
        } else {
            this.encassGuid = StringUtils.EMPTY;
        }
    }

    public String getEncassId() {
        return encassId;
    }

    public void setEncassId(final String encassId) {
        if (encassId != null) {
            this.encassId = encassId;
        } else {
            this.encassId = StringUtils.EMPTY;
        }
    }

    public FragmentDetails getFragmentDetails() {
        return fragmentDetails;
    }

    public void setFragmentDetails(final FragmentDetails fragmentDetails) {
        this.fragmentDetails = fragmentDetails;
    }
}
