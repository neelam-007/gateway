package com.l7tech.identity.internal;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.PersistentGroup;

public class InternalGroup extends PersistentGroup {
    public InternalGroup() {
        super();
    }

    public InternalGroup( GroupBean bean ) {
        super(bean);
    }

    public String toString() {
        return "com.l7tech.identity.internal.InternalGroup." +
                "\n\tname=" + _name +
                "\n\tproviderId=" + bean.getProviderId();
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( Group objToCopy) {
        InternalGroup imp = (InternalGroup)objToCopy;
        setOid(imp.getOid());
        setDescription(imp.getDescription());
        setName(imp.getName());
        setProviderId(imp.getProviderId());
    }
}
