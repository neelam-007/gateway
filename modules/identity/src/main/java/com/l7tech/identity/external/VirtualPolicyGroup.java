package com.l7tech.identity.external;

import com.l7tech.identity.GroupBean;
import com.l7tech.objectmodel.Goid;

/**
 * Represents a group in the PolicyBackedIdentityProvider.
 * <p/>
 * Note that there is currently no way to define groups for this provider type.
 */
public class VirtualPolicyGroup extends GroupBean {
    public VirtualPolicyGroup(Goid providerId, String groupUniqueId, String name) {
        super(providerId, name);
        setUniqueIdentifier(groupUniqueId);
    }

    public VirtualPolicyGroup() {
    }
}
