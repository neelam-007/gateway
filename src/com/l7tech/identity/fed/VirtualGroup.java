/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

/**
 * A "virtual" federated group.
 *
 * Physical groups only exist on the trusting SSG; their membership is maintained manually
 * by the administrator. By contrast, the membership of a given {@link FederatedUser} in a {@link VirtualGroup}
 * can change based on the user's and group's particular attributes.
 *
 * @see FederatedGroup
 * @author alex
 * @version $Revision$
 */
public class VirtualGroup extends FederatedGroup {
    public String getSamlEmailPattern() {
        return samlEmailPattern;
    }

    public void setSamlEmailPattern( String samlEmailPattern ) {
        this.samlEmailPattern = samlEmailPattern;
    }

    public String getX509SubjectDnPattern() {
        return x509SubjectDnPattern;
    }

    public void setX509SubjectDnPattern( String x509SubjectDnPattern ) {
        this.x509SubjectDnPattern = x509SubjectDnPattern;
    }

    public String getXmlProperties() {
        return xmlProperties;
    }

    public void setXmlProperties( String xmlProperties ) {
        this.xmlProperties = xmlProperties;
    }

    private String samlEmailPattern;
    private String x509SubjectDnPattern;
    private String xmlProperties;
}
