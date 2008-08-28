package com.l7tech.server.ems.user;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;

import java.util.Map;

/**
 * User Property Management interface.
 */
public interface UserPropertyManager {

    /**
     * Load properties for a user.
     *
     * @param user the user whose properties are to be loaded
     * @return the Map of property names to property values (not null)
     * @throws FindException if an error occurs
     */
    Map<String,String> getUserProperties( User user ) throws FindException;

    /**
     * Save properties for a user.
     *
     * @param user the user whose properties are to be saved
     * @param properties the properties to save
     * @throws UpdateException if an error occurs 
     */
    void saveUserProperties( User user, Map<String,String> properties )  throws UpdateException;
}
