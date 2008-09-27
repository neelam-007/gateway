/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.util;

import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * @author mike
 */
public interface SsmPreferences {
    String SERVICE_URL = "service.url";
    /**
     * look and feel key
     */
    String LOOK_AND_FEEL = "look.and.feel";
    /**
     * look and feel key
     */
    String INACTIVITY_TIMEOUT = "inactivity.timeout";
    /**
     * last login id
     */
    String LAST_LOGIN_ID = "last.login.id";
    /**
     * remember last login id
     */
    String SAVE_LAST_LOGIN_ID = "last.login.id.save";

    String ENABLE_POLICY_VALIDATION_ID = "enable.policy.validation";

    /*
    * key for value for how many ssg url's the Manager remembers 
    * */
    String NUM_SSG_HOSTS_HISTORY = "num.ssg.hosts.history";
    
    /**
     * toolbars property (icons, text, icons and text)
     */
    String STATUS_BAR_VISIBLE = "status.bar.enable";
    /**
     * toolbars property (icons, text, icons and text)
     */
    String POLICY_MSG_AREA_VISIBLE = "policy.msg.area.visible";
    // Screen size last time the app was started up
    String LAST_SCREEN_SIZE_WIDTH = "last.screen.size.width";
    String LAST_SCREEN_SIZE_HEIGHT = "last.screen.size.height";
    // Window size and location last time app was exited normally
    String LAST_WINDOW_SIZE_WIDTH = "last.window.size.width";
    String LAST_WINDOW_SIZE_HEIGHT = "last.window.size.height";
    String LAST_WINDOW_LOC_X = "last.window.location.x";
    String LAST_WINDOW_LOC_Y = "last.window.location.y";

    String AUDIT_WINDOW_RETRIEVAL_MODE = "auditWindow.retrieval.mode";
    String AUDIT_WINDOW_DURATION_MILLIS = "auditWindow.retrieval.duration.millis";
    String AUDIT_WINDOW_DURATION_AUTO_REFRESH = "auditWindow.retrieval.duration.autoRefresh";
    String AUDIT_WINDOW_TIME_RANGE_START = "auditWindow.retrieval.timeRange.startTime";
    String AUDIT_WINDOW_TIME_RANGE_END = "auditWindow.retrieval.timeRange.endTime";
    String AUDIT_WINDOW_TIME_RANGE_TIMEZONE = "auditWindow.retrieval.timeRange.timeZone";
    String AUDIT_WINDOW_LOG_LEVEL = "auditWindow.search.logLevel";
    String AUDIT_WINDOW_SERVICE_NAME = "auditWindow.search.serviceName";
    String AUDIT_WINDOW_MESSAGE = "auditWindow.search.message";
    String AUDIT_WINDOW_AUDIT_TYPE = "auditWindow.search.auditType";
    String AUDIT_WINDOW_NODE = "auditWindow.search.node";
    String AUDIT_WINDOW_REQUEST_ID = "auditWindow.search.requestId";

    /**
     * Update preferences from given properties, optionally
     * appending the properties
     *
     * @param p      the source properties
     * @param append true append or replace the property, false
     *               ignore unknown properties, and update the
     *               existing properties
     */
    void updateFromProperties(Properties p, boolean append);

    /**
     * merge preferences into system properties.
     * Note that this method overrwrites system properties with
     * user preferences.
     */
    void updateSystemProperties();

    /**
     * store the preferences.
     * This stores the preferences in the user home directory.
     *
     * @throws java.io.IOException thrown if an io error occurred
     */
    void store() throws IOException;

    /**
     * For heavy application only, gets the home path where preferences are stored.  Should never be called
     * when running in applet mode.
     *
     * @return String representing the home path where the
     *         preferences are stored.
     * @throws UnsupportedOperationException if we are currently running in applet mode.
     */
    String getHomePath();

    /**
     * Save the SSG cert.  The heavy prefs saves this in the trust store.  The applet prefs takes no action.
     *
     * @param cert
     * @param hostname
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     */
    void importSsgCert(X509Certificate cert, String hostname) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException;

    /**
     * Returns the value associated with the specified key
     * Returns the specified default if there is no value
     * associated with the key.
     *
     * @param key key whose associated value is to be returned.
     * @param def the value to be returned in the event that this
     *            preference key has no value
     * @return the value associated with key, or def if no value is
     *         associated with key.
     * @throws NullPointerException if key is null. (A null value for def is permitted.)
     */
    String getString(String key, String def)
      throws NullPointerException;

    /**
     * Returns the value associated with the specified key
     *
     * @param key key whose associated value is to be returned.
     * @return the value associated with key, or null if no value is
     *         associated with key.
     * @throws NullPointerException if key is null. (A null value for def is permitted.)
     */
    String getString(String key);

    /**
     * retrieve the history property
     *
     * @param property the property to get the history
     * @return the <code>History</code> instance
     */
    History getHistory(String property);

    /**
     * Get the last screen size, or null if not set.
     */
    Dimension getLastScreenSize();

    /**
     * Set the last screen size.
     */
    void setLastScreenSize(Dimension d);

    /**
     * Get the last window size, or null if not set.
     */
    Dimension getLastWindowSize();

    /**
     * Set the last window size.
     */
    void setLastWindowSize(Dimension d);

    /**
     * Get the last window location, or null if not set.
     */
    Point getLastWindowLocation();

    /**
     * Set the last window location.
     */
    void setLastWindowLocation(Point p);

    /**
     * Associates the specified value with the specified key in this
     * preference node.
     *
     * @param key   key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @throws NullPointerException if key or value is null.
     */
    void putProperty(String key, String value) throws NullPointerException;

    /**
     * Removes the key (and its corresponding value) from this
     * preferences. This method does nothing if the key is not
     * in the preferences.
     *
     * @param key the key that needs to be removed.
     * @throws NullPointerException if key or value is null.
     */
    void remove(String key) throws NullPointerException;

    /**
     * Returns the service url. This will typically be in the format
     * protocol://host:port.
     *
     * @return the service url.
     */
    String getServiceUrl();

    /**
     * Returns the inactivity timeout value.
     *
     * @return the inactivity timeout value as an int.
     */
    int getInactivityTimeout();

    /**
     * Returns the remeber login propery value.
     *
     * @return the 'remember login ID' value as boolean.
     */
    boolean rememberLoginId();

    /**
     * Returns the shortcut bar  visible propery value.
     *
     * @return the shortcut bar visible value as boolean.
     */
    boolean isStatusBarBarVisible();

    /**
     * Set the shortcut bar visible property value.
     *
     * @param b the shortcut bar visible
     */
    void seStatusBarVisible(boolean b);

    /**
     * Returns the policy messages visible property value.
     *
     * @return the policy messages visible value as boolean.
     */
    boolean isPolicyMessageAreaVisible();

    /**
     * Set the policy messages visible property value.
     *
     * @param b the shortcut bar visible
     */
    void setPolicyMessageAreaVisible(boolean b);

    /**
     * prints this property list out to the specified output
     * stream.
     *
     * @param ps an output stream.
     */
    void list(PrintStream ps);

    /**
     * @return the copy of the underlying properties instance.
     */
    Properties asProperties();
}
