/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.HashMap;
import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
@XmlRootElement
public class FederatedIdentityProviderConfig extends IdentityProviderConfig {
    public FederatedIdentityProviderConfig() {
        super(IdentityProviderType.FEDERATED);
    }

    public boolean isWritable() {
        return true;
    }

    public boolean isSamlSupported() {
        return getBooleanProperty(PROP_SAML_SUPPORTED, false);
    }

    public void setSamlSupported(boolean saml) {
        setProperty(PROP_SAML_SUPPORTED,Boolean.valueOf(saml));
    }

    public boolean isX509Supported() {
        return getBooleanProperty(PROP_X509_SUPPORTED, true);
    }

    public void setX509Supported(boolean x509) {
        setProperty(PROP_X509_SUPPORTED,Boolean.valueOf(x509));
    }

    public X509Config getX509Config() {
        X509Config config = (X509Config)getProperty(PROP_X509_CONFIG);
        if ( config == null ) {
            config = new X509Config();
            setProperty(PROP_X509_CONFIG, config);
        }
        return config;
    }

    public long[] getTrustedCertOids() {
        long[] oids = (long[])getProperty(PROP_CERT_OIDS);
        if ( oids == null ) {
            oids = new long[0];
            setProperty(PROP_CERT_OIDS, oids);
        }
        return oids;
    }

    public void setTrustedCertOids(long[] oids) {
        setProperty(PROP_CERT_OIDS, oids);
    }

    public HashMap<String, FederatedGroup> getImportedGroups() {
        return importedGroups;
    }

    public void setImportedGroups(HashMap<String, FederatedGroup> importedGroups) {
        this.importedGroups = importedGroups;
    }

    public HashMap<String, FederatedUser> getImportedUsers() {
        return importedUsers;
    }

    public void setImportedUsers(HashMap<String, FederatedUser> importedUsers) {
        this.importedUsers = importedUsers;
    }

    @XmlTransient
    public HashMap<String, Set<String>> getImportedGroupMembership() {
        return importedGroupMembership;
    }

    public void setImportedGroupMembership(HashMap<String, Set<String>> importedGroupMembership) {
        this.importedGroupMembership = importedGroupMembership;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<FederatedIdentityProviderConfig ");
        sb.append("oid=\"").append(_oid).append("\" ");
        sb.append("name=\"").append(_name).append("\" ");
        sb.append("samlSupported=\"").append(isSamlSupported()).append("\" ");
        sb.append("x509Supported=\"").append(isX509Supported()).append("\">\n  ");
        sb.append(getX509Config().toString());
        sb.append("</FederatedIdentityProviderConfig>");
        return sb.toString();
    }

    private static final String PROP_SAML_SUPPORTED = "samlSupported";
    private static final String PROP_X509_SUPPORTED = "x509Supported";
    private static final String PROP_X509_CONFIG = "x509Config";
    private static final String PROP_CERT_OIDS = "trustedCertOids";

    private HashMap<String, FederatedUser> importedUsers;
    private HashMap<String, FederatedGroup> importedGroups;
    private HashMap<String, Set<String>> importedGroupMembership;
}
