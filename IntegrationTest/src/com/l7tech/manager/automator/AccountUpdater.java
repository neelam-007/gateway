package com.l7tech.manager.automator;

import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.AdminContext;

/**
 * Revokes certificates for certain accounts. The IIP accounts that are updated then have their password
 * set to "password".
 */
public class AccountUpdater {
    private IdentityAdmin identityAdmin;

    /**
     * Creates a new instance of AccountUpdater.
     *
     * @param adminContext
     */
    public AccountUpdater(AdminContext adminContext) {
        identityAdmin = adminContext.getIdentityAdmin();
    }

    /**
     * Returns the ID of the identity provider that matches the provided name.
     *
     * @param name The identity provider name to search for
     * @return The ID of the matching identity provider
     * @throws FindException
     */
    public long getIdentityProviderId(String name) throws FindException {
        EntityHeader[] headers = identityAdmin.findAllIdentityProviderConfig();

        String iipID = null;
        for(EntityHeader header : headers) {
            if(header.getName().equals(name)) {
                iipID = header.getStrId();
                break;
            }
        }

        if(iipID != null) {
            return Long.parseLong(iipID);
        } else {
            throw new FindException();
        }
    }

    /**
     * Updates the accounts specified in the properties: manager.automator.iip.userCertsToRevoke,
     * manager.automator.msad.userCertsToRevoke, and manager.automator.ldap.userCertsToRevoke.
     */
    public void updateAccounts() {
        try {
            long iipID = getIdentityProviderId("Internal Identity Provider");

            String[] usernames = Main.getProperties().getProperty("manager.automator.iip.userCertsToRevoke").split(",");
            for(String username : usernames) {
                User user = identityAdmin.findUserByLogin(iipID, username);
                identityAdmin.revokeCert(user);
                System.out.println("Revoked certificate of " + user.getLogin());
                user = identityAdmin.findUserByID(iipID, user.getId());
                ((InternalUser)user).setCleartextPassword(new String("password"));
                identityAdmin.saveUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, user, null);
                System.out.println("Reset the password of " + user.getLogin());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            long msadID = getIdentityProviderId("QAAD MSAD");

            String[] usernames = Main.getProperties().getProperty("manager.automator.msad.userCertsToRevoke").split(",");
            for(String username : usernames) {
                User user = identityAdmin.findUserByLogin(msadID, username);
                identityAdmin.revokeCert(user);
                System.out.println("Revoked certificate of " + user.getLogin());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            long ldapID = getIdentityProviderId("soran LDAP");

            String[] usernames = Main.getProperties().getProperty("manager.automator.ldap.userCertsToRevoke").split(",");
            for(String username : usernames) {
                User user = identityAdmin.findUserByLogin(ldapID, username);
                identityAdmin.revokeCert(user);
                System.out.println("Revoked certificate of " + user.getLogin());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("Finished updating accounts.");
    }
}
