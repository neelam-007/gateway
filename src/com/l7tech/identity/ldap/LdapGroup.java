package com.l7tech.identity.ldap;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;

import java.io.Serializable;

public class LdapGroup implements Group, Serializable {
    public static final int OU_GROUP = 0;
    public static final int NORMAL_GROUP = 1;

    public LdapGroup( GroupBean bean ) {
        _groupBean = bean;
    }

    public LdapGroup() {
        _groupBean = new GroupBean();
    }

    public String getDescription() {
        return _groupBean.getDescription();
    }

    public String getName() {
        return _groupBean.getName();
    }

    public void setDescription(String description) {
        _groupBean.setDescription( description );
    }

    public String getUniqueIdentifier() {
        return _dn;
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
        return _dn;
    }

    public void setDn(String dn) {
        _dn = dn;
        _groupBean.setUniqueIdentifier(dn);
    }

    public String getCn() {
        return _groupBean.getName();
    }

    public void setCn(String cn) {
        _groupBean.setName( cn );
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
        if ( !_dn.equals(groupImp._dn) ) return false;
        return true;
    }

    public int hashCode() {
        if ( _dn == null ) return System.identityHashCode( this );

        int hash = _dn.hashCode();
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
    }

    public GroupBean getGroupBean() {
        return _groupBean;
    }

    private String _dn;

    private GroupBean _groupBean;

    private long providerId;
}
