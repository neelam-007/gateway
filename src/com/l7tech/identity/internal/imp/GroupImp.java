/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.internal.imp;

import java.util.Collection;

import com.l7tech.identity.Group;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class GroupImp extends NamedEntityImp implements Group {
    public long getProviderOid() {
        return _providerOid;
    }

    public String getDescription() {
        return _description;
    }

    public Collection getMembers() {
        if (_members == null) _members = new java.util.ArrayList();
        return _members;
    }

    public Collection getMemberHeaders() {
        if (_memberHeaders == null) _memberHeaders = new java.util.ArrayList();
        return _memberHeaders;
    }

    public void setMembers( Collection members ) {
        _members = members;
    }

    public void setMemberHeaders( Collection memberHeaders ) {
        _memberHeaders = memberHeaders;
    }

    public void setProviderOid( long oid ) {
        _providerOid = oid;
    }

    public void setDescription(String description) {
        _description = description;
    }

    private long _providerOid;
    private String _description;
    private Collection _members;
    private Collection _memberHeaders;
}
