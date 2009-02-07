/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;

import org.hibernate.annotations.Proxy;

/**
 * @author alex
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="fed_user")
public class FederatedUser extends PersistentUser {
    public FederatedUser() {
        this(IdentityProviderConfig.DEFAULT_OID, null);
    }

    public FederatedUser(long providerOid, String login) {
        super(providerOid, login);
    }

    @Override
    @Column(name="provider_oid")
    public long getProviderId() {
        return super.getProviderId();
    }

    @Override
    @Column(name="subject_dn", length=255)
    public String getSubjectDn() {
        return super.getSubjectDn();
    }

    @Override
    public void copyFrom( User objToCopy ) {
        FederatedUser imp = (FederatedUser)objToCopy;
        setOid(imp.getOid());
        setName(imp.getName());
        setSubjectDn(imp.getSubjectDn());
        setProviderId(imp.getProviderId());
        setLogin(imp.getLogin());
        setDepartment(imp.getDepartment());
        setEmail(imp.getEmail());
        setFirstName(imp.getFirstName());
        setLastName(imp.getLastName());
    }
}
