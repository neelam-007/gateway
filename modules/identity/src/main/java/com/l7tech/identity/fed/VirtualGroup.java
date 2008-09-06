/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.IdentityProviderConfig;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.AttributeOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.Transient;
import javax.persistence.Lob;

/**
 * A "virtual" federated group.
 *
 * Physical groups only exist on the trusting SSG; their membership is maintained manually
 * by the administrator. By contrast, the membership of a given {@link FederatedUser} in a
 * {@link VirtualGroup} can change based on the user's and group's particular attributes.
 *
 * @see FederatedGroup
 * @author alex
 */

@XmlRootElement
@Entity
@Table(name="fed_group_virtual")
public class VirtualGroup extends FederatedGroup {
    public VirtualGroup() {
        this(IdentityProviderConfig.DEFAULT_OID, null);
    }

    public VirtualGroup(long providerOid, String name) {
        super(providerOid, name);
    }

    @Column(name="saml_email_pattern",length=128)
    public String getSamlEmailPattern() {
        return getProperty(PROP_SAML_EMAIL_PATTERN);
    }

    public void setSamlEmailPattern( String samlEmailPattern ) {
        setProperty(PROP_SAML_EMAIL_PATTERN, samlEmailPattern);
    }

    @Column(name="x509_subject_dn_pattern",length=255)
    public String getX509SubjectDnPattern() {
        return getProperty(PROP_X509_DN_PATTERN);
    }

    public void setX509SubjectDnPattern( String x509SubjectDnPattern ) {
        setProperty(PROP_X509_DN_PATTERN, x509SubjectDnPattern);
    }

    @Override
    @Column(name="properties",length=Integer.MAX_VALUE)
    @Lob
    public synchronized String getXmlProperties() {
        return super.getXmlProperties();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final VirtualGroup that = (VirtualGroup)o;

        String samlEmailPattern = getSamlEmailPattern();
        String thatSamlEmailPattern = that.getSamlEmailPattern();
        if (samlEmailPattern != null ? !samlEmailPattern.equals(thatSamlEmailPattern) : thatSamlEmailPattern != null)
            return false;

        String x509DnPattern = getX509SubjectDnPattern();
        String thatX509DnPattern = that.getX509SubjectDnPattern();
        if (x509DnPattern != null ? !x509DnPattern.equals(thatX509DnPattern) : thatX509DnPattern != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        String samlEmailPattern = getSamlEmailPattern();
        result = 31 * result + (samlEmailPattern != null ? samlEmailPattern.hashCode() : 0);
        String x509DnPattern = getX509SubjectDnPattern();
        result = 31 * result + (x509DnPattern != null ? x509DnPattern.hashCode() : 0);
        return result;
    }

    private static final String PROP_SAML_EMAIL_PATTERN = "samlEmailPattern";
    private static final String PROP_X509_DN_PATTERN = "x509SubjectDnPattern";
}
