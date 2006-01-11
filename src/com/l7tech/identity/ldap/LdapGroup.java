package com.l7tech.identity.ldap;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;

import javax.naming.directory.Attributes;
import java.io.Serializable;

public class LdapGroup implements Group, Serializable, LdapIdentity {
    public static final int OU_GROUP = 0;
    public static final int NORMAL_GROUP = 1;

    private String dn;
    private GroupBean groupBean;
    private long providerId;
    private Attributes attributes;

    public LdapGroup( GroupBean bean ) {
        groupBean = bean;
    }

    public LdapGroup() {
        groupBean = new GroupBean();
    }

    public String getDescription() {
        return groupBean.getDescription();
    }

    public String getName() {
        return groupBean.getName();
    }

    public void setDescription(String description) {
        groupBean.setDescription( description );
    }

    public String getUniqueIdentifier() {
        return dn;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return providerId;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId ) {
        this.providerId = providerId;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        if (dn == null) throw new NullPointerException();
        this.dn = dn;
        groupBean.setUniqueIdentifier(dn);
    }

    public String getCn() {
        return groupBean.getName();
    }

    public Attributes getAttributes() {
        return attributes;
    }


    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public void setCn(String cn) {
        groupBean.setName( cn );
    }

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + getName() +
                "\n\tDN=" + getDn() +
                "\n\tproviderId=" + providerId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LdapGroup)) return false;
        final LdapGroup groupImp = (LdapGroup) o;
        if ( providerId != groupImp.providerId ) return false;
        if ( !dn.equals(groupImp.dn) ) return false;
        return true;
    }

    public int hashCode() {
        if ( dn == null ) return System.identityHashCode( this );

        int hash = dn.hashCode();
        hash += 29 * (int)providerId;

        return hash;
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
