package com.l7tech.identity.ldap;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;

import java.io.Serializable;

public class LdapGroup extends LdapIdentityBase implements Group, Serializable, LdapIdentity {
    public static final int OU_GROUP = 0;
    public static final int NORMAL_GROUP = 1;

    private GroupBean groupBean;

    public LdapGroup( GroupBean bean ) {
        groupBean = bean;
    }

    public LdapGroup() {
        groupBean = new GroupBean();
    }

    public String getDescription() {
        return groupBean.getDescription();
    }

    public void setDescription(String description) {
        groupBean.setDescription( description );
    }

    public synchronized void setDn(String dn) {
        super.setDn(dn);
        groupBean.setUniqueIdentifier(dn);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        groupBean.setName(name);
    }

    @Override
    public void setCn(String cn) {
        super.setCn(cn);
        groupBean.setName(cn);
    }

    @Override
    public void setProviderId(long providerOid) {
        super.setProviderId(providerOid);
        groupBean.setProviderId(providerOid);
    }

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + getName() +
                "\n\tDN=" + getDn() +
                "\n\tproviderId=" + providerId;
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( Group objToCopy) {
        LdapGroup imp = (LdapGroup)objToCopy;
        setDn(imp.getDn());
        setCn(imp.getCn());
        setDescription(imp.getDescription());
        setProviderId(imp.getProviderId());
        setAttributes(imp.getAttributes());
    }

    public GroupBean getGroupBean() {
        return groupBean;
    }

}
