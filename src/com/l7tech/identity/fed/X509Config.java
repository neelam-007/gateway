/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import java.io.Serializable;

/**
 * The X.509-related configuration for a {@link com.l7tech.server.identity.fed.FederatedIdentityProvider}.
 *
 * This gets serialized by {@link java.beans.XMLEncoder} to XML for storage along with the
 * {@link FederatedIdentityProviderConfig}.
 *
 * @author alex
 * @version $Revision$
 */
public class X509Config implements Serializable {
    public X509Config() {
    }

    public boolean isSslClientCert() {
        return sslClientCert;
    }

    public void setSslClientCert( boolean sslClientCert ) {
        this.sslClientCert = sslClientCert;
    }

    public boolean isWssBinarySecurityToken() {
        return wssBinarySecurityToken;
    }

    public void setWssBinarySecurityToken( boolean wssBinarySecurityToken ) {
        this.wssBinarySecurityToken = wssBinarySecurityToken;
    }

    /** Non-normative! */
    public String toString() {
        StringBuffer sb = new StringBuffer("<X509Config ");
        sb.append("sslClientCert=\"").append(sslClientCert).append("\" ");
        sb.append("wssBinarySecurityToken=\"").append(wssBinarySecurityToken).append("\"/>");
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof X509Config)) return false;

        final X509Config config = (X509Config)o;

        if (sslClientCert != config.sslClientCert) return false;
        if (wssBinarySecurityToken != config.wssBinarySecurityToken) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (sslClientCert ? 1 : 0);
        result = 29 * result + (wssBinarySecurityToken ? 1 : 0);
        return result;
    }

    private boolean sslClientCert = false;
    private boolean wssBinarySecurityToken = true;
}
