package com.l7tech.server.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.PersistentUserManagerImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.logging.Logger;

/**
 * SSG-side implementation of the UserManager for the internal identity provider.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003<br/>
 *
 * @version $Revision$
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class InternalUserManagerImpl
        extends PersistentUserManagerImpl<InternalUser, InternalGroup, InternalUserManager, InternalGroupManager>
        implements InternalUserManager 
{
    public InternalUserManagerImpl(InternalIdentityProvider identityProvider) {
        super(identityProvider);
    }

    protected InternalUserManagerImpl() {}

    public String getTableName() {
        return "internal_user";
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public IdentityHeader userToHeader(InternalUser user) {
        InternalUser imp = cast(user);
        return new IdentityHeader(imp.getProviderId(), imp.getUniqueIdentifier(), EntityType.USER, imp.getLogin(), null);
    }

    public InternalUser reify(UserBean bean) {
        return new InternalUser(bean);
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public InternalUser headerToUser(IdentityHeader header) {
        InternalUser iu = new InternalUser();
        iu.setProviderId(getProviderOid());
        iu.setOid(header.getOid());
        iu.setLogin(header.getName());
        return iu;
    }

    public Class getImpClass() {
        return InternalUser.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }

    protected void preSave(InternalUser user) throws SaveException {
        // check to see if an existing user with same login exist
        InternalUser existingDude;
        try {
            existingDude = findByLogin(user.getLogin());
        } catch (FindException e) {
            existingDude = null;
        }
        if (existingDude != null) {
            throw new DuplicateObjectException("Cannot save this user. Existing user with login \'"
              + user.getLogin() + "\' present.");
        }
    }

    protected void checkUpdate(InternalUser originalUser,
                               InternalUser updatedUser) throws ObjectModelException {
        // checks whether the updatedUser changed his password
        String originalPasswd = originalUser.getPassword();
        String newPasswd = updatedUser.getPassword();

        // if password has changed, any cert should be revoked
        if (!originalPasswd.equals(newPasswd)) {
            logger.info("Revoking cert for updatedUser " + originalUser.getLogin() + " because he is changing his passwd.");
            // must revoke the cert

            revokeCert(originalUser);
        }
    }

    /**
     * Checks whether this user is the last member of the "Gateway Administrators" group.
     * Prevents the administrator from removing his own admin accout membership
     *
     * @throws DeleteException if the proposed deletion would remove the last member of the "Gateway Administrators" group.
     */
    protected void preDelete(InternalUser user) throws DeleteException {
        try {
            GroupManager<InternalUser, InternalGroup> groupManager = identityProvider.getGroupManager();
            for (IdentityHeader grp : groupManager.getGroupHeaders(user)) {
                // is he an administrator?
                if (Group.ADMIN_GROUP_NAME.equals(grp.getName())) {
                    // is he the last one ?
                    Set adminUserHeaders = groupManager.getUserHeaders(grp.getStrId());
                    if (adminUserHeaders.size() <= 1) {
                        String msg = "An attempt was made to nuke the last standing adminstrator";
                        logger.severe(msg);
                        throw new DeleteException(msg);
                    }
                }
            }
        } catch (FindException e) {
            throw new DeleteException("Couldn't find out if user is the last administrator", e);
        }
    }

    protected String getNameFieldname() {
        return "login";
    }

    public InternalUser cast(User user) {
        InternalUser imp;
        if (user instanceof UserBean) {
            imp = new InternalUser((UserBean)user);
        } else {
            imp = (InternalUser)user;
        }
        return imp;
    }


    private final Logger logger = Logger.getLogger(getClass().getName());
}
