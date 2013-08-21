package com.l7tech.server.logon;

import com.l7tech.identity.LogonInfo;
import com.l7tech.objectmodel.*;

/**
 * Interface that handles the logon information object between the application and database layers.
 *
 * User: dlee
 * Date: Jun 27, 2008
 */
public interface LogonInfoManager extends EntityManager<LogonInfo, EntityHeader> {

    /**
     * Finds a the logon information object based on the composite keys.
     *
     * @param providerId    The provider that handles the user
     * @param login         The login name
     * @param lock          True to lock the object for update
     * @return              The LogonInfo object if found.
     * @throws FindException
     */
    public LogonInfo findByCompositeKey(Goid providerId, String login, boolean lock) throws FindException;

    /**
     * Updates the logon information object into the database.
     *
     * @param entity    The logon information object
     * @throws UpdateException
     */
    public void update(LogonInfo entity) throws UpdateException;

    /**
     * Deletes the logon information for the provided login and provide ID.
     *
     * @param providerId    The provider ID
     * @param login         The login name
     * @throws DeleteException
     */
    public void delete(Goid providerId, String login) throws DeleteException;
}
