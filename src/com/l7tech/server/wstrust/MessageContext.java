package com.l7tech.server.wstrust;

import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 12-Aug-2004
 */
public class MessageContext {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private Map properties = new HashMap();
    private LoginCredentials credentials;

    /**
     * Sets (add oir update) the property
     * @param key the property name
     * @param value the property value
     */
    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * Returns the property value for the key
     * @param key the key name
     * @return the value for the key name
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public LoginCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(LoginCredentials credentials) {
        this.credentials = credentials;
    }
}
