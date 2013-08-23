package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

import java.util.Collection;

/**
 * The manager managing standard report settings.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 20, 2009
 * @since Enterprise Manager 1.0
 */
public interface StandardReportSettingsManager extends EntityManager<StandardReportSettings, EntityHeader> {

    /**
     * Find report settings for the given user.
     *
     * @param user The user whose settings are to be found
     * @return The settings
     * @throws FindException If an error occurs
     */
    Collection<StandardReportSettings> findByUser( User user ) throws FindException;

    /**
     * Find report by oid only if the user matches.
     *
     * @param user The user for the settings
     * @param goid The identifier for the settings
     * @return The setting or null if not found or user does not match
     * @throws FindException If an error occurs
     */
    StandardReportSettings findByPrimaryKeyForUser( User user, Goid goid ) throws FindException;

    /**
     * Find report by name and user.
     *
     * @param user The user for the settings
     * @param name The name for the settings
     * @return The setting or null if not found or user does not match
     * @throws FindException If an error occurs
     */
    StandardReportSettings findByNameAndUser( User user, String name ) throws FindException;
}
