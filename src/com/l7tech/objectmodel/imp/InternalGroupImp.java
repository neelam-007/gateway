/*
 * Created on 7-May-2003
 */
package com.l7tech.ssg.objectmodel.imp;

import java.util.Collection;

import com.l7tech.ssg.objectmodel.InternalGroup;

/**
 * @author alex
 */
public class InternalGroupImp extends NamedEntityImp implements InternalGroup {
    public String getDescription() {
        return _description;
    }

    public Collection getMembers() {
        // TODO: Implement!
        return null;
    }

    public void setDescription(String description) {
        _description = description;
    }

    private String _description;
}
