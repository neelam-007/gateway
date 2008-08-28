package com.l7tech.server.ems;

import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;

import java.util.Map;
import java.util.Collections;

/**
 *
 */
public class MockUserPropertyManager implements UserPropertyManager {

    public Map<String, String> getUserProperties(User user) throws FindException {
        return Collections.singletonMap("dateformat", "formal");
    }

    public void saveUserProperties(User user, Map<String, String> properties) throws UpdateException {
    }
}
