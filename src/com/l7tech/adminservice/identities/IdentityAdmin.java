package com.l7tech.adminservice.identities;

import com.l7tech.adminservice.ListResultEntry;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 6, 2003
 *
 * This is the admin web service for the management of identities
 * and identity providers
 */
public interface IdentityAdmin {
    /**
     * Get a list of existing identity providers
     */
    public ListResultEntry[] listProviders();

    public ListResultEntry[] listUsersInProvider(long providerId);

    public ListResultEntry[] listGroupsInProvider(long providerId);
}
