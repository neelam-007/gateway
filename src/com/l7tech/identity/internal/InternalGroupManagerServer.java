package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.Group;

import java.sql.SQLException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class InternalGroupManagerServer extends HibernateEntityManager implements GroupManager {
    public InternalGroupManagerServer() {
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
