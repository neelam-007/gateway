/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author alex
 */
@XmlRootElement
public class FederatedUser extends PersistentUser {
    public FederatedUser() {
        this(IdentityProviderConfig.DEFAULT_OID, null);
    }

    public FederatedUser(long providerOid, String login) {
        super(providerOid, login);
    }

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
