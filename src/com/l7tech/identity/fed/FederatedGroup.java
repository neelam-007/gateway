/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import java.util.Map;

import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.IdentityProviderConfig;

import javax.xml.bind.annotation.XmlRootElement;

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
@XmlRootElement
public class FederatedGroup extends PersistentGroup {
    public FederatedGroup() {
        this(IdentityProviderConfig.DEFAULT_OID, null);
    }

    public FederatedGroup(long providerOid, String name) {
        super(providerOid, name);
    }

    public FederatedGroup(long providerOid, String name, Map<String, String> properties) {
        super(providerOid, name, properties);
    }

    public String toString() {
        return "com.l7tech.identity.fed.FederatedGroup." +
                "\n\tname=" + _name +
                "\n\tproviderId=" + getProviderId();
    }
}