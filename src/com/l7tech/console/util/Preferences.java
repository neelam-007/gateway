package com.l7tech.console.util;

import org.springframework.context.support.ApplicationObjectSupport;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * The Preferencs class is a console property manager, that is
 * implemented as a light light wrapper arround
 * the <CODE>Properties</CODE> class.
 * It adds specific implementation for saving preferences,
 * (save preferences in home directory) and constants for
 * well known keys.
 * <p/>
 * The implementation uses the <CODE>PropertyChangeSupport</CODE>
 * to notify listeners of properties changes.
 * <p/>
 * <i>Migrate to JDK 1.4 Preferences.</i>
 * <p/>
 * The class is not synchronized.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @see java.beans.PropertyChangeSupport
 */
public class Preferences extends ApplicationObjectSupport {
    private static boolean debug = false;
    public static final String SERVICE_URL = "service.url";
    private static Preferences prefs = null;

    protected Properties props = new Properties();


    /**
     * private constructor use getPreferences to
     * instantiate the Preferences
     */
    protected Preferences() {
    }

    /**
     * get the Preferences instance
     * 
     * @return the Preferences instance
     */
    public static Preferences getPreferences() {
        if (prefs != null) return prefs;
        prefs = new Preferences();

//        if (null == System.getProperties().getProperty("javawebstart.version")) {
//            prefs = new Preferences();
//        } else { // app is invoked from JWS
//            prefs = new JNLPPreferences();
//        }
        prefs.setupDefaults();
        try {
            prefs.initialize();
        } catch (IOException e) {
            prefs.log("initialize", e);
        }

        return prefs;
    }

    /**
     * Update preferences from given properties, optionally
     * appending the properties
     * 
     * @param p      the source properties
     * @param append true append or replace the property, false
     *               ignore unknown properties, and update the
     *               existing properties
     */
    public void updateFromProperties(Properties p, boolean append) {
        Iterator keys = p.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            if (append) {
                putProperty(key, p.getProperty(key));
            } else {
                if (null != props.getProperty(key)) {
                    putProperty(key, p.getProperty(key));
                }
            }
        }
    }

    /**
     * merge preferences into system properties.
     * Note that this method overrwrites system properties with
     * user preferences.
     */
    public void updateSystemProperties() {
        System.getProperties().putAll(props);
    }

    /**
     * store the preferences.
     * This stores the preferences in the user home directory.
     * 
     * @throws IOException thrown if an io error occurred
     */
    public void store() throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(SSM_USER_HOME + File.separator + STORE);
            props.store(fout, "SSG properties");
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    }

    /**
     * @return String representing the home path where the
     *         preferences are stored.
     */
    public String getHomePath() {
        return SSM_USER_HOME;
    }

    public String getTrustStoreFile() {
        return TRUST_STORE_FILE;
    }

    public String getTrustStorePassword() {
        return TRUST_STORE_PASSWORD;
    }

    /**
     * setup default application properties
     */
    private void setupDefaults() {
        prepare();
        //copyResources();
        configureProperties();
        sslIinitialize();
        //logIinitialize();
    }

    private void prepare() {
        // verify home path exist, create if necessary
        File home =
          new File(SSM_USER_HOME);
        if (!home.exists()) {
            home.mkdir();
        }
    }


    /**
     * configure well known application properties.
     * If the property has not been set use the default
     * value.
     * thrown if an I/O error occurs
     */
    private void configureProperties() {
        // well known/predefined properties

        Map knownProps = new HashMap();
        knownProps.put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        knownProps.put("javax.net.ssl.trustStore",
          new File(TRUST_STORE_FILE).getAbsolutePath());
        knownProps.put("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);

        Iterator keys = knownProps.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            if (null == props.getProperty(key)) {
                putProperty(key, (String)knownProps.get(key));
            }
        }
    }


    /**
     * initialize ssl
     */
    private void sslIinitialize() {
        // JSSE SSLContext initialization on a separate thread,
        // attempt to improve performance on app startup. The
        // Sun SSL provider is hardcoded.
        new Thread(new Runnable() {
            public void run() {
                try {
                    long start = System.currentTimeMillis();
                    javax.net.ssl.SSLContext ctx =
                      javax.net.ssl.SSLContext.getInstance("SSL", "SunJSSE");
                    // SSL init with defaults
                    ctx.init(null, null, null);
                    long end = System.currentTimeMillis();
                    Preferences.this.log("SSLContext.init() - finished took " + (end - start) + " ms");
                } catch (java.security.GeneralSecurityException e) {
                    Preferences.this.log("SSLContext.init()", e);
                }
            }
        }).start();
    }


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
    public String getString(String key, String def)
      throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        return props.getProperty(key, def);
    }

    /**
     * Returns the value associated with the specified key
     * 
     * @param key key whose associated value is to be returned.
     * @return the value associated with key, or null if no value is
     *         associated with key.
     * @throws NullPointerException if key is null. (A null value for def is permitted.)
     */
    public String getString(String key) {
        if (key == null) throw new NullPointerException("key == null");
        return props.getProperty(key);
    }

    /**
     * retrieve the history property
     * 
     * @param property the property to get the history
     * @return the <code>History</code> instance
     */
    public History getHistory(String property) {
        return new PropertyHistory(property);
    }

    /**
     * Returns a pair of properties as a Dimension.
     * More of a convenience method.
     * 
     * @param widthKey  the key where the width is stored
     * @param heightKey the key where the height is stored
     * @return the Dimension, or null.
     */
    private Dimension getDimension(String widthKey, String heightKey) {
        try {
            return new Dimension(Integer.parseInt(props.getProperty(widthKey)),
              Integer.parseInt(props.getProperty(heightKey)));
        } catch (NumberFormatException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }


    /**
     * Set a pair of properties using values taken from a Dimension.
     * More of a convenience method.
     * 
     * @param d the new width and height, or null to clear them
     */
    private void setDimension(String widthKey, String heightKey, Dimension d) {
        if (d == null) {
            props.remove(widthKey);
            props.remove(heightKey);
        } else {
            props.setProperty(widthKey, Integer.toString(d.width));
            props.setProperty(heightKey, Integer.toString(d.height));
        }
    }


    /**
     * Get the last screen size, or null if not set.
     */
    public Dimension getLastScreenSize() {
        return getDimension(LAST_SCREEN_SIZE_WIDTH, LAST_SCREEN_SIZE_HEIGHT);
    }

    /**
     * Set the last screen size.
     */
    public void setLastScreenSize(Dimension d) {
        setDimension(LAST_SCREEN_SIZE_WIDTH, LAST_SCREEN_SIZE_HEIGHT, d);
    }

    /**
     * Get the last window size, or null if not set.
     */
    public Dimension getLastWindowSize() {
        return getDimension(LAST_WINDOW_SIZE_WIDTH, LAST_WINDOW_SIZE_HEIGHT);
    }

    /**
     * Set the last window size.
     */
    public void setLastWindowSize(Dimension d) {
        setDimension(LAST_WINDOW_SIZE_WIDTH, LAST_WINDOW_SIZE_HEIGHT, d);
    }

    /**
     * Get the last window location, or null if not set.
     */
    public Point getLastWindowLocation() {
        // For now we'll just abuse getDimension().
        Dimension d = getDimension(LAST_WINDOW_LOC_X, LAST_WINDOW_LOC_Y);
        if (null == d)
            return null;
        return new Point(d.width, d.height);
    }

    /**
     * Set the last window location.
     */
    public void setLastWindowLocation(Point p) {
        // For now we'll just abuse setDimension().
        Dimension d = p != null ? new Dimension(p.x, p.y) : null;
        setDimension(LAST_WINDOW_LOC_X, LAST_WINDOW_LOC_Y, d);
    }

    /**
     * Associates the specified value with the specified key in this
     * preference node.
     * 
     * @param key   key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @throws NullPointerException if key or value is null.
     */
    public void putProperty(String key, String value) throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        if (value == null) throw new NullPointerException("value == null");
        Object old = props.setProperty(key, value);
        PropertyChangeEvent e = new PropertyChangeEvent(this, key, old, value);
        //firePropertyChange(e);
    }

    /**
     * Removes the key (and its corresponding value) from this
     * preferences. This method does nothing if the key is not
     * in the preferences.
     * 
     * @param key the key that needs to be removed.
     * @throws NullPointerException if key or value is null.
     */
    public void remove(String key) throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        Object old = props.remove(key);
        PropertyChangeEvent e = new PropertyChangeEvent(this, key, old, null);
        //firePropertyChange(e);
    }

    /**
     * Returns the service url. This will typically be in the format
     * protocol://host:port.
     * 
     * @return the service url.
     */
    public String getServiceUrl() {
        return props.getProperty(SERVICE_URL);
    }

    /**
     * Returns the inactivity timeout value.
     * 
     * @return the inactivity timeout value as an int.
     */
    public int getInactivityTimeout() {
        String value = props.getProperty(INACTIVITY_TIMEOUT);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns the remeber login propery value.
     * 
     * @return the 'remember login ID' value as boolean.
     */
    public boolean rememberLoginId() {
        return Boolean.
          valueOf(props.getProperty(SAVE_LAST_LOGIN_ID)).booleanValue();
    }

    /**
     * Returns the shortcut bar  visible propery value.
     * 
     * @return the shortcut bar visible value as boolean.
     */
    public boolean isStatusBarBarVisible() {
        // default set
        if (props.getProperty(STATUS_BAR_VISIBLE) == null) {
            return true;
        }
        return Boolean.
          valueOf(props.getProperty(STATUS_BAR_VISIBLE)).booleanValue();
    }

    /**
     * Set the shortcut bar visible property value.
     * 
     * @param b the shortcut bar visible
     */
    public void seStatusBarVisible(boolean b) {
        putProperty(STATUS_BAR_VISIBLE, Boolean.toString(b));
    }

    /**
     * Returns the policy messages visible property value.
     *
     * @return the policy messages visible value as boolean.
     */
    public boolean isPolicyMessageAreaVisible() {
        // default set
        if (props.getProperty(POLICY_MSG_AREA_VISIBLE) == null) {
            return true;
        }
        return Boolean.
          valueOf(props.getProperty(POLICY_MSG_AREA_VISIBLE)).booleanValue();
    }

    /**
     * Set the policy messages visible property value.
     *
     * @param b the shortcut bar visible
     */
    public void setPolicyMessageAreaVisible(boolean b) {
        putProperty(POLICY_MSG_AREA_VISIBLE, Boolean.toString(b));
    }


    /**
     * prints this property list out to the specified output
     * stream.
     * 
     * @param ps an output stream.
     */
    public void list(PrintStream ps) {
        props.list(ps);
    }

    /**
     * @return the copy of the underlying properties instance.
     */
    public Properties asProperties() {
        Properties p = new Properties();
        p.putAll(props);
        return p;
    }

    /**
     * initialize the properties from the user properties in user's
     * home directory.
     */
    protected void initialize() throws IOException {
        File file = new File(SSM_USER_HOME + File.separator + STORE);

        if (file.exists()) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(file);
                props.load(fin);
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
     * simple log (no log4j used here)
     * 
     * @param msg the message to log
     */
    private void log(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }

    /**
     * simple log (no logger used here)
     * 
     * @param msg       the message to log
     * @param throwable throwable to log
     */
    private void log(String msg, Throwable throwable) {
        if (debug) {
            System.err.println(msg);
            throwable.printStackTrace(System.err);
        }
    }

    private class PropertyHistory implements History {
        private String propertyName;
        private int maxSize = 5;

        public PropertyHistory(String propertyName) {
            this.propertyName = propertyName;
        }

        /**
         * Add the object to the <code>History</code>.
         * <p/>
         * Implementations may remove the older element of the History if
         * their maximum size is reached.
         * 
         * @param o the object to add to the
         */
        public void add(Object o) {
            LinkedList values = values();
            Collection remove = new ArrayList();
            for (Iterator iterator = values.iterator(); iterator.hasNext();) {
                Object value = (Object)iterator.next();
                if (value.equals(o)) {
                    remove.add(value);
                }
            }
            values.removeAll(remove);
            values.addFirst(o);
            rekeyValues(values);
        }


        private LinkedList values() {
            SortedSet sortedKeys = new TreeSet();

            Enumeration enumeration = props.keys();
            while (enumeration.hasMoreElements()) {
                Object key = (Object)enumeration.nextElement();
                if (key.toString().startsWith(propertyName)) {
                    sortedKeys.add(key);
                }
            }

            LinkedList values = new LinkedList();
            for (Iterator iterator = sortedKeys.iterator(); iterator.hasNext();) {
                Object key = (Object)iterator.next();
                values.add(props.get(key));
            }
            return values;
        }

        private void rekeyValues(List values) {
            Enumeration enumeration = props.keys();
            while (enumeration.hasMoreElements()) {
                Object key = (Object)enumeration.nextElement();
                if (key.toString().startsWith(propertyName)) {
                    remove(key.toString());
                }
            }
            int index = 0;
            for (Iterator iterator = values.iterator(); iterator.hasNext() && index < maxSize;) {
                Object value = (Object)iterator.next();
                String pn = null;
                if (index == 0) {
                    pn = propertyName;
                } else {
                    pn = propertyName + "." + index;
                }
                putProperty(pn, value.toString());
                ++index;
            }
        }

        /**
         * get the array of history entries.
         * 
         * @return the array of history entries.
         */
        public Object[] getEntries() {
            return values().toArray();
        }
    }

    /**
     * look and feel key
     */
    public static final String LOOK_AND_FEEL = "look.and.feel";

    /**
     * look and feel key
     */
    public static final String INACTIVITY_TIMEOUT = "inactivity.timeout";

    /**
     * last login id
     */
    public static final String LAST_LOGIN_ID = "last.login.id";

    /**
     * remember last login id
     */
    public static final String SAVE_LAST_LOGIN_ID = "last.login.id.save";

    /**
     * toolbars property (icons, text, icons and text)
     */
    public static final String STATUS_BAR_VISIBLE = "status.bar.enable";

    /**
     * toolbars property (icons, text, icons and text)
     */
    public static final String POLICY_MSG_AREA_VISIBLE = "policy.msg.area.visible";

    // Screen size last time the app was started up
    public static final String LAST_SCREEN_SIZE_WIDTH = "last.screen.size.width";
    public static final String LAST_SCREEN_SIZE_HEIGHT = "last.screen.size.height";

    // Window size and location last time app was exited normally
    public static final String LAST_WINDOW_SIZE_WIDTH = "last.window.size.width";
    public static final String LAST_WINDOW_SIZE_HEIGHT = "last.window.size.height";
    public static final String LAST_WINDOW_LOC_X = "last.window.location.x";
    public static final String LAST_WINDOW_LOC_Y = "last.window.location.y";

    // Time formats
    public static final String LONG_24_HOUR_TIME_FORMAT = "HH:mm:ss z";


    /**
     * the file name for the preferences
     */
    protected static final String STORE = "ssg.properties";

    /**
     * where is home (properties are stored there)
     */
    public static final String SSM_USER_HOME =
      System.getProperties().getProperty("user.home") + File.separator + ".l7tech";

    private final String TRUST_STORE_FILE = SSM_USER_HOME + File.separator + "trustStore";
    private final String TRUST_STORE_PASSWORD = "password";


    private static final
    String[] res =
      new String[]
      {
          "trustStore"
      };

    public static void main(String[] args) {
        try {
            Preferences prefs = Preferences.getPreferences();
            prefs.list(System.out);
            prefs.putProperty(SERVICE_URL, "http://localhost:8080");
            java.text.DateFormat df =
              java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);

            prefs.store();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
