/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.User;

/**
 * @author alex
 */
public class FederatedUser extends PersistentUser {
    public FederatedUser(UserBean bean) {
        super(bean);
    }

    public FederatedUser() {
        super();
    }

    public String getSubjectDn() {
        return bean.getSubjectDn();
    }

    public void setSubjectDn(String dn) {
        bean.setSubjectDn(dn);
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
