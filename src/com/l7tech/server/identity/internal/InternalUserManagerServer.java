package com.l7tech.server.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.identity.PersistentUserManager;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * SSG-side implementation of the UserManager for the internal identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003<br/>
 *
 * @version $Revision$
 *
 */
public class InternalUserManagerServer extends PersistentUserManager {
    protected void preSave( User user ) throws SaveException {
        // check to see if an existing user with same login exist
        User existingDude = null;
        try {
            existingDude = findByLogin(user.getLogin());
        } catch (FindException e) {
            existingDude = null;
        }
        if (existingDude != null) {
            throw new SaveException("Cannot save this user. Existing user with login \'"
                                    + user.getLogin() + "\' present.");
        }
    }

    public static final String IMPCLASSNAME = InternalUser.class.getName();

    public InternalUserManagerServer(IdentityProvider provider) {
        this.provider = provider;
    }

    protected void preDelete( PersistentUser user ) throws FindException, DeleteException {
        if (isLastAdmin(user)) {
            String msg = "An attempt was made to nuke the last standing adminstrator";
            logger.severe(msg);
            throw new DeleteException(msg);
        }
    }

    protected PersistentUser cast( User user ) {
        InternalUser imp;
        if ( user instanceof UserBean ) {
            imp = new InternalUser( (UserBean)user );
        } else {
            imp = (InternalUser)user;
        }
        return imp;
    }

    public String getTableName() {
        return "internal_user";
    }

    public Class getImpClass() {
        return InternalUser.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }

    /**
     * check whether this user is the last standing administrator
     * this is used at update time to prevent the last administrator to nuke his admin accout membership
     * @param currentUser
     * @return true is this user is an administrator and no other users are
     */
    private boolean isLastAdmin(User currentUser) throws FindException {
        InternalUser imp = (InternalUser)currentUser;
        GroupManager gman = provider.getGroupManager();
        Iterator i = gman.getGroupHeaders(imp).iterator();
        while (i.hasNext()) {
            EntityHeader grp = (EntityHeader)i.next();
            // is he an administrator?
            if (Group.ADMIN_GROUP_NAME.equals(grp.getName())) {
                // is he the last one ?
                Set adminUserHeaders = gman.getUserHeaders( grp.getStrId() );
                if ( adminUserHeaders.size() <= 1 ) return true;
                return false;
            }
        }
        return false;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
