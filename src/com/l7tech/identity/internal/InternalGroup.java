package com.l7tech.identity.internal;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.objectmodel.imp.NamedEntityImp;

public class InternalGroup extends NamedEntityImp implements Group {
    public InternalGroup() {
        _groupBean = new GroupBean();
    }

    public InternalGroup( GroupBean bean ) {
        _groupBean = bean;
    }

    public void setOid( long oid ) {
        super.setOid(oid);
        _groupBean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        String uniqueId = _groupBean.getUniqueIdentifier();
        if ( uniqueId == null || uniqueId.length() == 0 ) {
            return -1L;
        } else {
            return new Long( _groupBean.getUniqueIdentifier() ).longValue();
        }
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

    public void setName( String name ) {
        _groupBean.setName( name );
    }

    public String getUniqueIdentifier() {
        return new Long( _oid ).toString();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return _groupBean.getProviderId();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId ) {
        _groupBean.setProviderId( providerId );
    }

    public int getVersion() {
        return _groupBean.getVersion();
    }

    public void setVersion(int version) {
        _groupBean.setVersion(version);
    }

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + _name +
                "\n\tproviderId=" + _groupBean.getProviderId();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalGroup)) return false;
        final InternalGroup groupImp = (InternalGroup) o;
        if (_oid != DEFAULT_OID ? !(_oid == groupImp._oid) : groupImp._oid != DEFAULT_OID ) return false;
        return true;
    }

    public int hashCode() {
        if ( _oid != DEFAULT_OID ) return (int)_oid;
        if ( _name == null ) return System.identityHashCode(this);

        int hash = _name.hashCode();
        hash += 29 * (int)_groupBean.getProviderId();
        return hash;
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

    public GroupBean getGroupBean() {
        return _groupBean;
    }

    private GroupBean _groupBean;
}
