/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedIdentityProviderConfig extends IdentityProviderConfig {
    public FederatedIdentityProviderConfig() {
        super(IdentityProviderType.FEDERATED);
    }

    public boolean isSamlSupported() {
        return getBooleanProperty(PROP_SAML_SUPPORTED, false);
    }

    public void setSamlSupported(boolean saml) {
        props.put(PROP_SAML_SUPPORTED,Boolean.valueOf(saml));
    }

    public boolean isX509Supported() {
        return getBooleanProperty(PROP_X509_SUPPORTED, true);
    }

    public void setX509Supported(boolean x509) {
        props.put(PROP_X509_SUPPORTED,Boolean.valueOf(x509));
    }

    public SamlConfig getSamlConfig() {
        SamlConfig config = (SamlConfig)props.get(PROP_SAML_CONFIG);
        if ( config == null ) {
            config = new SamlConfig();
            props.put(PROP_SAML_CONFIG,config);
        }
        return config;
    }

    public X509Config getX509Config() {
        X509Config config = (X509Config)props.get(PROP_X509_CONFIG);
        if ( config == null ) {
            config = new X509Config();
            props.put(PROP_X509_CONFIG, config);
        }
        return config;
    }

    public long[] getTrustedCertOids() {
        long[] oids = (long[])props.get(PROP_CERT_OIDS);
        if ( oids == null ) {
            oids = new long[0];
            props.put(PROP_CERT_OIDS, oids);
        }
        return oids;
    }

    public void setTrustedCertOids(long[] oids) {
        props.put(PROP_CERT_OIDS, oids);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<FederatedIdentityProviderConfig ");
        sb.append("oid=\"").append(_oid).append("\" ");
        sb.append("name=\"").append(_name).append("\" ");
        sb.append("samlSupported=\"").append(isSamlSupported()).append("\" ");
        sb.append("x509Supported=\"").append(isX509Supported()).append("\">\n  ");
        sb.append(getSamlConfig().toString()).append("\n  ");
        sb.append(getX509Config().toString());
        sb.append("</FederatedIdentityProviderConfig>");
        return sb.toString();
    }

    private static final String PROP_SAML_SUPPORTED = "samlSupported";
    private static final String PROP_X509_SUPPORTED = "x509Supported";
    private static final String PROP_SAML_CONFIG = "samlConfig";
    private static final String PROP_X509_CONFIG = "x509Config";
    private static final String PROP_CERT_OIDS = "trustedCertOids";
}
