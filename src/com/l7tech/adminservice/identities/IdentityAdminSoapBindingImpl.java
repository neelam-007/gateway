package com.l7tech.adminservice.identities;

import com.l7tech.adminservice.ListResultEntry;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 6, 2003
 *
 */
public class IdentityAdminSoapBindingImpl implements IdentityAdmin {
    /**
     * Get a list of existing identity providers
     */
    public ListResultEntry[] listProviders() {
        // todo (the real thing)
        ListResultEntry[] res = new ListResultEntry[1];
        res[0] = new ListResultEntry(3216, "LocalIdentityProvider", "com.l7tech.identity.IdentityProvider");
        return res;
    }

    public ListResultEntry[] listUsersInProvider(long providerId) {
        // todo (the real thing)
        ListResultEntry[] res = new ListResultEntry[3];
        res[0] = new ListResultEntry(6349, "Darth Vader", "com.l7tech.identity.User");
        res[1] = new ListResultEntry(6350, "Yoda", "com.l7tech.identity.User");
        res[2] = new ListResultEntry(6351, "Boba Fett", "com.l7tech.identity.User");
        return res;
    }

    public ListResultEntry[] listGroupsInProvider(long providerId) {
        // todo (the real thing)
        ListResultEntry[] res = new ListResultEntry[4];
        res[0] = new ListResultEntry(6366, "Jedi Knights", "com.l7tech.identity.Group");
        res[1] = new ListResultEntry(6367, "Dark Side Meddlers", "com.l7tech.identity.Group");
        res[2] = new ListResultEntry(6368, "Storm Troopers", "com.l7tech.identity.Group");
        res[3] = new ListResultEntry(6368, "Sexy Princesses", "com.l7tech.identity.Group");
        return res;
    }
}
