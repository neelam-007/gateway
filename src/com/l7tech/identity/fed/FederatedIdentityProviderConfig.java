/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedIdentityProviderConfig extends IdentityProviderConfig {
    public FederatedIdentityProviderConfig() {
        super(IdentityProviderType.FEDERATED);
    }

    public boolean isSamlSupported() {
        return getBooleanProperty(PROP_SAML_SUPPORTED);
    }

    public boolean isX509Supported() {
        return getBooleanProperty(PROP_X509_SUPPORTED);
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

    public Set getTrustedCerts() {
        return trustedCerts;
    }

    public void setTrustedCerts( Set trustedCerts ) {
        this.trustedCerts = trustedCerts;
    }

    private Set trustedCerts;
    
    private static final String PROP_SAML_SUPPORTED = "samlSupported";
    private static final String PROP_X509_SUPPORTED = "x509Supported";
    private static final String PROP_SAML_CONFIG = "samlConfig";
    private static final String PROP_X509_CONFIG = "x509Config";
}
