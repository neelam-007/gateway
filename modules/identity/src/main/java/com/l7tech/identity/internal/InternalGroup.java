package com.l7tech.identity.internal;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.PersistentGroup;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InternalGroup extends PersistentGroup {
    public InternalGroup() {
        this(null);
    }

    public InternalGroup(String name) {
        super(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, name);
    }

    public String toString() {
        return "com.l7tech.identity.internal.InternalGroup." +
                "\n\tname=" + _name +
                "\n\tproviderId=" + getProviderId();
    }
}
