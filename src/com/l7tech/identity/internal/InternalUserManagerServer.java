package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;

import java.sql.SQLException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class InternalUserManagerServer extends HibernateEntityManager implements UserManager {

    public InternalUserManagerServer() {
        super();
    }

    public User findByPrimaryKey(String oid) throws FindException {
        try {
            return (User)_manager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid));
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( NumberFormatException nfe ) {
            throw new FindException( nfe.toString(), nfe );
        }
    }

    public void delete(User user) throws DeleteException {
        try {
            _manager.delete( getContext(), user );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(User user) throws SaveException {
        try {
            return _manager.save( getContext(), user );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( User user ) throws UpdateException {
        try {
            _manager.update( getContext(), user );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public String getTableName() {
        return "internal_user";
    }

    public Class getImpClass() {
        return User.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }
}
