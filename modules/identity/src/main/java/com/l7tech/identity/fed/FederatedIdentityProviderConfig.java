/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Proxy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.HashMap;
import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@DiscriminatorValue("3")
public class FederatedIdentityProviderConfig extends IdentityProviderConfig {

    public FederatedIdentityProviderConfig(IdentityProviderConfig other) {
        super(IdentityProviderType.FEDERATED);
        this._version = other.getVersion();
	    this._oid = other.getOid();
        copyFrom(other);
    }

    public FederatedIdentityProviderConfig() {
        super(IdentityProviderType.FEDERATED);
    }

    @Override
    @Transient
    public boolean isWritable() {
        return true;
    }

    @Override
    @Transient
    public boolean canIssueCertificates() {
        return false;
    }

    @Transient
    public boolean isSamlSupported() {
        return getBooleanProperty(PROP_SAML_SUPPORTED, false);
    }

    public void setSamlSupported(boolean saml) {
        setProperty(PROP_SAML_SUPPORTED, saml);
    }

    @Transient
    public boolean isX509Supported() {
        return getBooleanProperty(PROP_X509_SUPPORTED, true);
    }

    public void setX509Supported(boolean x509) {
        setProperty(PROP_X509_SUPPORTED, x509);
    }

    @Transient
    public X509Config getX509Config() {
        X509Config config = (X509Config)getProperty(PROP_X509_CONFIG);
        if ( config == null ) {
            config = new X509Config();
            setProperty(PROP_X509_CONFIG, config);
        }
        return config;
    }

    @Transient
    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.TRUSTED_CERT)
    public Goid[] getTrustedCertGoids() {
        Goid[] goids = (Goid[])getProperty(PROP_CERT_GOIDS);
        if ( goids == null ) {
            goids = new Goid[0];
            setProperty(PROP_CERT_GOIDS, goids);
        }
        return goids;
    }

    public void setTrustedCertGoids(Goid[] goids) {
        setProperty(PROP_CERT_GOIDS, goids);
    }

    @Override
    @Transient
    protected String[] getUnexportablePropKeys() {
        return new String[] {PROP_CERT_OIDS, PROP_CERT_GOIDS};
    }

    @Transient
    public HashMap<String, FederatedGroup> getImportedGroups() {
        return importedGroups;
    }

    public void setImportedGroups(HashMap<String, FederatedGroup> importedGroups) {
        this.importedGroups = importedGroups;
    }

    @Transient
    public HashMap<String, FederatedUser> getImportedUsers() {
        return importedUsers;
    }

    public void setImportedUsers(HashMap<String, FederatedUser> importedUsers) {
        this.importedUsers = importedUsers;
    }

    @XmlTransient
    @Transient
    public HashMap<String, Set<String>> getImportedGroupMembership() {
        return importedGroupMembership;
    }

    public void setImportedGroupMembership(HashMap<String, Set<String>> importedGroupMembership) {
        this.importedGroupMembership = importedGroupMembership;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<FederatedIdentityProviderConfig ");
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
    public static final String PROP_CERT_GOIDS = "trustedCertGoids";

    // Should not be used any more -- should be present only in upgraded databases
    private static final String PROP_CERT_OIDS = "trustedCertOids";

    private HashMap<String, FederatedUser> importedUsers;
    private HashMap<String, FederatedGroup> importedGroups;
    private HashMap<String, Set<String>> importedGroupMembership;
}
