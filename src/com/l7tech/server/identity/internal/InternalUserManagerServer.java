package com.l7tech.server.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
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
    public InternalUserManagerServer(IdentityProvider provider) {
        this.provider = provider;
    }

    public String getTableName() {
        return "internal_user";
    }

    public EntityHeader userToHeader( User user ) {
        InternalUser imp = (InternalUser)cast(user);
        return new EntityHeader(imp.getUniqueIdentifier(), EntityType.USER, imp.getLogin(), null);
    }

    public Class getImpClass() {
        return InternalUser.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }

    protected void preSave(PersistentUser user) throws SaveException {
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

    protected void checkUpdate( PersistentUser originalUser,
                                PersistentUser updatedUser ) throws ObjectModelException {
        // checks whether the updatedUser changed his password
        String originalPasswd = originalUser.getPassword();
        String newPasswd = updatedUser.getPassword();

        // if password has changed, any cert should be revoked
        if (!originalPasswd.equals(newPasswd)) {
            logger.info("Revoking cert for updatedUser " + originalUser.getLogin() + " because he is changing his passwd.");
            // must revoke the cert

            revokeCert( originalUser );
        }
    }

    /**
     * Checks whether this user is the last member of the "Gateway Administrators" group.
     * Prevents the administrator from removing his own admin accout membership
     * @throws DeleteException if the proposed deletion would remove the last member of the "Gateway Administrators" group.
     */
    protected void preDelete( PersistentUser user ) throws DeleteException {
        try {
            InternalUser imp = (InternalUser)user;
            GroupManager gman = provider.getGroupManager();
            Iterator i = gman.getGroupHeaders(imp).iterator();
            while (i.hasNext()) {
                EntityHeader grp = (EntityHeader)i.next();
                // is he an administrator?
                if (Group.ADMIN_GROUP_NAME.equals(grp.getName())) {
                    // is he the last one ?
                    Set adminUserHeaders = gman.getUserHeaders( grp.getStrId() );
                    if ( adminUserHeaders.size() <= 1 ) {
                        String msg = "An attempt was made to nuke the last standing adminstrator";
                        logger.severe(msg);
                        throw new DeleteException(msg);
                    }
                }
            }
        } catch ( FindException e ) {
            throw new DeleteException("Couldn't find out if user is the last administrator", e);
        }
    }

    protected String getNameFieldname() {
        return "login";
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

    private final Logger logger = Logger.getLogger(getClass().getName());
}
