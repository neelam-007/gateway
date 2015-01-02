package com.l7tech.external.assertions.radius.server;

import net.jradius.exception.RadiusException;
import net.jradius.packet.attribute.AttributeList;

public class RadiusExceptionWithAttributes extends RadiusException {

    private AttributeList listOfAttributes;

    private String authenticator;

    public RadiusExceptionWithAttributes(String message) {
        super(message);
    }

    public RadiusExceptionWithAttributes(String message, AttributeList al, String encodedAuthenticator) {
        super(message);
        setListOfAttributes(al);
        setAuthenticator(encodedAuthenticator);
    }

    public AttributeList getListOfAttributes() {
        return listOfAttributes;
    }

    public void setListOfAttributes(AttributeList listOfAttributes) {
        this.listOfAttributes = listOfAttributes;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }
}
