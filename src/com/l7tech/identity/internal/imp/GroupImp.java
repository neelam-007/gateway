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
    public String getDescription() {
        return _description;
    }

    public Collection getMembers() {
        if (_members == null) _members = new new java.util.ArrayList();
        return _members;
    }

    public Collection getMemberHeaders() {
        if (_memberHeaders == null) _memberHeaders = new new java.util.ArrayList();
        return _memberHeaders;
    }

    public void setDescription(String description) {
        _description = description;
    }

    private String _description;
    private Collection _members;
    private Collection _memberHeaders;
}
