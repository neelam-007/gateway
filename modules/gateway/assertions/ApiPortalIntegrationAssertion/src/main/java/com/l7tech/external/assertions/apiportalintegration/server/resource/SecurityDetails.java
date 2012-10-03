package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class SecurityDetails {
    @XmlElement(name = "OAuth", namespace = JAXBResourceMarshaller.NAMESPACE)
    private OAuthDetails oauth;

    public SecurityDetails(final OAuthDetails oauth) {
        setOauth(oauth);
    }

    public SecurityDetails() {

    }

    public OAuthDetails getOauth() {
        return oauth;
    }

    public void setOauth(final OAuthDetails oauth) {
        this.oauth = oauth;
    }
}
