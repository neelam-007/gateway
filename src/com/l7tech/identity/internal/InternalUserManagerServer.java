package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.logging.LogManager;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 * @version $Revision$
 *
 */
public class InternalUserManagerServer extends HibernateEntityManager implements UserManager {

    public InternalUserManagerServer() {
        super();
    }

    public User findByPrimaryKey(String oid) throws FindException {
        try {
            User out = (User)_manager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid));
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( NumberFormatException nfe ) {
            throw new FindException( nfe.toString(), nfe );
        }
    }

    public User findByLogin( String login ) throws FindException {
        try {
            List users = _manager.find( getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".login = ?", login, String.class );
            switch ( users.size() ) {
            case 0:
                return null;
            case 1:
                User u = (User)users.get(0);
                u.setProviderId( IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID );
                return u;
            default:
                String err = "Found more than one user with the login " + login;
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
                throw new FindException( err );
            }
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(User user) throws DeleteException {
        try {
            _manager.delete( getContext(), user );
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(User user) throws SaveException {
        try {
            return _manager.save( getContext(), user );
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( User user ) throws UpdateException {
        try {
            _manager.update( getContext(), user );
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new UpdateException( se.toString(), se );
        }
    }

    public EntityHeader userToHeader(User user) {
        return new EntityHeader(user.getOid(), EntityType.USER, user.getName(), null);
    }

    public User headerToUser(EntityHeader header) {
        try {
            return findByPrimaryKey(header.getStrId());
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
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
