/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.internal.imp;

import java.util.Set;

import com.l7tech.identity.Group;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class GroupImp extends NamedEntityImp /*implements Group*/ {
    public long getProviderOid() {
        return _providerOid;
    }

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

    public void setProviderOid( long oid ) {
        _providerOid = oid;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupImp)) return false;

        final GroupImp groupImp = (GroupImp) o;

        if (_oid != DEFAULT_OID ? !(_oid == groupImp._oid) : groupImp._oid != DEFAULT_OID ) return false;
        if (_providerOid != groupImp._providerOid) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (_providerOid ^ (_providerOid >>> 32));

        result = 29 * result + (int)_oid;
        return result;
    }

    private long _providerOid;
    private String _description;
    private Set _members;
    private Set _memberHeaders;
}
