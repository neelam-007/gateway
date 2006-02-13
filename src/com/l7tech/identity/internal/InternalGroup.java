package com.l7tech.identity.internal;

import com.l7tech.identity.GroupBean;
import com.l7tech.identity.PersistentGroup;

public class InternalGroup extends PersistentGroup {
    public InternalGroup() {
        super();
    }

    public InternalGroup(GroupBean bean) {
        super(bean);
    }

    public String toString() {
        return "com.l7tech.identity.internal.InternalGroup." +
                "\n\tname=" + _name +
                "\n\tproviderId=" + bean.getProviderId();
    }
}
