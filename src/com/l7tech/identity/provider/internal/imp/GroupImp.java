/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.provider.internal.imp;

import java.util.Collection;

import com.l7tech.identity.provider.internal.Group;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class GroupImp extends NamedEntityImp implements Group {
    public String getDescription() {
        return _description;
    }

    public Collection getMembers() {
        return _members;
    }

    public void setDescription(String description) {
        _description = description;
    }

    private String _description;
    private Collection _members;
}
