/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.PersistentGroup;

/**
 * A "physical" federated group.
 *
 * @author alex
 * @see VirtualGroup
 * @version $Revision$
 */
public class FederatedGroup extends PersistentGroup {
    public FederatedGroup(GroupBean bean) {
        super(bean);
    }

    public FederatedGroup() {
        super();
    }

    public String toString() {
        return "com.l7tech.identity.fed.FederatedGroup." +
                "\n\tname=" + _name +
                "\n\tproviderId=" + bean.getProviderId();
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom(Group objToCopy) {
        FederatedGroup imp = (FederatedGroup)objToCopy;
        setOid(imp.getOid());
        setDescription(imp.getDescription());
        setName(imp.getName());
        setProviderId(imp.getProviderId());
    }
}
