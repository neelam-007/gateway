/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.GroupBean;

/**
 * A "virtual" federated group.
 *
 * Physical groups only exist on the trusting SSG; their membership is maintained manually
 * by the administrator. By contrast, the membership of a given {@link FederatedUser} in a
 * {@link VirtualGroup} can change based on the user's and group's particular attributes.
 *
 * @see FederatedGroup
 * @author alex
 * @version $Revision$
 */
public class VirtualGroup extends FederatedGroup {
    public VirtualGroup() {
        super();
    }

    public VirtualGroup(GroupBean bean) {
        super(bean);
    }

    public String getSamlEmailPattern() {
        return (String)bean.getProperties().get(PROP_SAML_EMAIL_PATTERN);
    }

    public void setSamlEmailPattern( String samlEmailPattern ) {
        bean.getProperties().put(PROP_SAML_EMAIL_PATTERN, samlEmailPattern);
    }

    public String getX509SubjectDnPattern() {
        return (String)bean.getProperties().get(PROP_X509_DN_PATTERN);
    }

    public void setX509SubjectDnPattern( String x509SubjectDnPattern ) {
        bean.getProperties().put(PROP_X509_DN_PATTERN, x509SubjectDnPattern);
    }

    private static final String PROP_SAML_EMAIL_PATTERN = "samlEmailPattern";
    private static final String PROP_X509_DN_PATTERN = "x509SubjectDnPattern";
}
