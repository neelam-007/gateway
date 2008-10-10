package com.l7tech.server.logon;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;

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
     * @return              The LogonInfo object if found.
     * @throws FindException
     */
    public LogonInfo findByCompositeKey(long providerId, String login) throws FindException;

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
    public void delete(long providerId, String login) throws DeleteException;
}
