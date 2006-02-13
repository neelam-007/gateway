/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.GroupBean;
import com.l7tech.identity.PersistentGroup;

/**
 * A "physical" federated group.
 *
 * Physical groups only exist on the trusting SSG; their membership is maintained manually
 * by the administrator. By contrast, the membership of a given {@link FederatedUser} in a {@link VirtualGroup}
 * can change based on the user's and group's particular attributes.
 *
 * @author alex
 * @see VirtualGroup
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
}