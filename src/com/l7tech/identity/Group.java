package com.l7tech.identity;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.Set;
import java.security.Principal;

public class Group extends NamedEntityImp implements Principal {

    public String getDescription() {
        return _description;
    }

    public Set getMembers() {
        return _members;
    }

    public Set getMemberHeaders() {
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

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + _name;
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

    private String _description;
    private Set _members;
    private Set _memberHeaders;
}
