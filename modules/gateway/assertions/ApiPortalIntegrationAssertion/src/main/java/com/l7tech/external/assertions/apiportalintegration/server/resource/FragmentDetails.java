package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class FragmentDetails {
    @XmlElement(name = "HasRouting", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String hasRouting = StringUtils.EMPTY;
    @XmlElement(name = "ParsedPolicyDetails", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String parsedPolicyDetails = StringUtils.EMPTY;

    public FragmentDetails() {

    }

    public FragmentDetails(final String callbackUrl, final String scope) {
        setHasRouting(callbackUrl);
        setParsedPolicyDetails(scope);
    }

    public String getHasRouting() {
        return hasRouting;
    }

    public void setHasRouting(final String hasRouting) {
        if (hasRouting != null) {
            this.hasRouting = hasRouting;
        } else {
            this.hasRouting = StringUtils.EMPTY;
        }
    }

    public String getParsedPolicyDetails() {
        return parsedPolicyDetails;
    }

    public void setParsedPolicyDetails(final String parsedPolicyDetails) {
        if (parsedPolicyDetails != null) {
            this.parsedPolicyDetails = parsedPolicyDetails;
        } else {
            this.parsedPolicyDetails = StringUtils.EMPTY;
        }
    }
}
