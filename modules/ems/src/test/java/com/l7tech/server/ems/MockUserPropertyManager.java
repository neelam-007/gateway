package com.l7tech.server.ems;

import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 */
public class MockUserPropertyManager implements UserPropertyManager {

    @Override
    public Map<String, String> getUserProperties(User user) throws FindException {
        return new HashMap<String,String>();
    }

    @Override
    public void saveUserProperties(User user, Map<String, String> properties) throws UpdateException {
    }
}
