/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;

/**
 * @author alex
 */
public class GroupManagerImp extends HibernateEntityManager implements GroupManager {
    public GroupManagerImp() {
        super();
    }

    public Group findByPrimaryKey(String oid) throws FindException {
        try {
            return (Group)_manager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid) );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(Group group) throws DeleteException {
        try {
            _manager.delete( getContext(), group );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(Group group) throws SaveException {
        try {
            return _manager.save( getContext(), group );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( Group group ) throws UpdateException {
        try {
            _manager.update( getContext(), group );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public String getTableName() {
        return "internal_group";
    }

    public Class getImpClass() {
        return Group.class;
    }

    public Class getInterfaceClass() {
        return Group.class;
    }

}
