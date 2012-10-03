package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class OAuthDetails {
    @XmlElement(name = "CallbackUrl", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String callbackUrl = StringUtils.EMPTY;
    @XmlElement(name = "Scope", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String scope = StringUtils.EMPTY;
    @XmlElement(name = "Type", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String type = StringUtils.EMPTY;

    public OAuthDetails() {

    }

    public OAuthDetails(final String callbackUrl, final String scope, final String type) {
        setCallbackUrl(callbackUrl);
        setScope(scope);
        setType(type);
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(final String callbackUrl) {
        if (callbackUrl != null) {
            this.callbackUrl = callbackUrl;
        } else {
            this.callbackUrl = StringUtils.EMPTY;
        }
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        if (scope != null) {
            this.scope = scope;
        } else {
            this.scope = StringUtils.EMPTY;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        if (type != null) {
            this.type = type;
        } else {
            this.type = StringUtils.EMPTY;
        }
    }
}
