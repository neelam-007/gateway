package com.l7tech.identity;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.HashSet;
import java.util.Set;
import java.security.Principal;

public class Group extends NamedEntityImp implements Principal {

    public static final String ADMIN_GROUP_NAME = "SSGAdmin";

    public String getDescription() {
        return _description;
    }

    public Set getMembers() {
        if ( _members == null ) return new HashSet();
        return _members;
    }

    public Set getMemberHeaders() {
        if ( _memberHeaders == null ) return new HashSet();
        return _memberHeaders;
    }

    public void setMembers( Set members ) {
        _members = members;
    }

    public void setMemberHeaders( Set memberHeaders ) {
        _memberHeaders = memberHeaders;
    }

    public void setDescription(String description) {
        _description = description;
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
    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + _name +
                "\n\tproviderId=" + providerId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        final Group groupImp = (Group) o;
        if (_oid != DEFAULT_OID ? !(_oid == groupImp._oid) : groupImp._oid != DEFAULT_OID ) return false;
        return true;
    }

    public int hashCode() {
        return (int)getOid();
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom(Group objToCopy) {
        setOid(objToCopy.getOid());
        setDescription(objToCopy.getDescription());
        setMembers(objToCopy.getMembers());
        setName(objToCopy.getName());
        setProviderId(objToCopy.getProviderId());
    }

    private String _description;
    private Set _members;
    private Set _memberHeaders;
    private long providerId;
}
