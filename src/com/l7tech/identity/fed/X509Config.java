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
 * This gets serialized to XML for storage along with the {@link FederatedIdentityProviderConfig}.
 *
 * @author alex
 * @version $Revision$
 */
public class X509Config implements Serializable {
    public X509Config() {
        sslClientCert = false;
        wssBinarySecurityToken = true;
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

    public String toString() {
        StringBuffer sb = new StringBuffer("<X509Config ");
        sb.append("sslClientCert=\"").append(sslClientCert).append("\" ");
        sb.append("wssBinarySecurityToken=\"").append(wssBinarySecurityToken).append("\"/>");
        return sb.toString();
    }

    private boolean sslClientCert;
    private boolean wssBinarySecurityToken;
}
