package com.l7tech.console.util;

import com.l7tech.console.jnlp.JNLPPreferences;
import org.apache.log4j.BasicConfigurator;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Security;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * The Preferencs class is a console property manager, that is
 * implemented as a light light wrapper arround
 * the <CODE>Properties</CODE> class.
 * It adds specific implementation for saving preferences,
 * (save preferences in home directory) and constants for
 * well known keys.
 *
 * The implementation uses the <CODE>PropertyChangeSupport</CODE>
 * to notify listeners of properties changes.
 *
 * <i>Migrate to JDK 1.4 Preferences.</i>
 *
 * The class is not synchronized.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @see java.beans.PropertyChangeSupport
 */
public class Preferences extends PropertyChangeSupport {
    private static boolean debug = false;
    public static final String SERVICE_URL = "service.url";
    public static final String SERVICE_URL_SUFFIX = "/ssg";
    private static Preferences prefs = null;

    protected Properties props = new Properties();


    /**
     * private constructor use getPreferences to
     * instantiate the Preferences
     */
    protected Preferences() {
        super("preferences"); // unused value for super constructor
    }

    /**
     * get the Preferences instance
     *
     * @return the Preferences instance
     * @exception IOException
     *                   if an Io error occured
     */
    public static Preferences getPreferences()
            throws IOException {
        if (prefs == null) {
            if (null ==
                    System.getProperties().getProperty("javawebstart.version")) {
                prefs = new Preferences();
            } else { // app is invoked from JWS
                prefs = new JNLPPreferences();
            }
            prefs.setupDefaults();
            prefs.initialize();
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
     * @exception IOException
     *                   thrown if an io error occurred
     */
    public void updateFromProperties(Properties p, boolean append)
            throws IOException {
        Iterator keys = p.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
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
     *
     * @exception IOException
     *                   thrown if an io error occurred
     */
    public void updateSystemProperties() throws IOException {
        System.getProperties().putAll(props);
    }

    /**
     * store the preferences.
     * This stores the preferences in the user home directory.
     *
     * @exception IOException
     *                   thrown if an io error occurred
     */
    public void store() throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(CONSOLE_CONFIG + File.separator + STORE);
            props.store(fout, "SSG properties");
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    }

    /**
     *
     * @return String representing the home path where the
     *         preferences are stored.
     */
    public String getHomePath() {
        return CONSOLE_CONFIG;
    }

    /**
     * setup default application properties
     *
     * @exception IOException
     *                   thrown on I/O error
     */
    private void setupDefaults() throws IOException {
        prepare();
        copyResources();
        configureProperties();
        sslIinitialize();
        logIinitialize();
    }

    private void prepare() {
        // verify home path exist, create if necessary
        File home =
                new File(CONSOLE_CONFIG);
        if (!home.exists()) {
            home.mkdir();
        }
    }

    /**
     * copy resources from jar to the local client
     * storage
     *
     * @exception IOException
     *                   thrown if an I/O error occurs
     */
    private void copyResources() throws IOException {
        InputStream in = null;
        try {
            ClassLoader cl = getClass().getClassLoader();
            for (int i = 0; i < res.length; i++) {
                in = cl.getResourceAsStream(res[i]);
                if (in == null) {
                    log("warning couldn't load " + res[i]);
                } else {
                    File file = new File(CONSOLE_CONFIG + File.separator + res[i]);
                    // if (!file.exists()) {
                    dumpStreamToFile(in, file);
                    // }
                    in.close();
                    in = null;
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * configure well known application properties.
     * If the property has not been set use the default
     * value.
     *
     * @exception IOException
     *                   thrown if an I/O error occurs
     */
    private void configureProperties() throws IOException {
        // well known/predefined properties

        Map knownProps = new HashMap();
        knownProps.put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        knownProps.put("javax.net.ssl.trustStore",
                new File(CONSOLE_CONFIG + File.separator + "trustStore").getAbsolutePath());
        knownProps.put("javax.net.ssl.trustStorePassword", "password");

        Iterator keys = knownProps.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (null == props.getProperty(key)) {
                putProperty(key, (String) knownProps.get(key));
            }
        }
    }

    /**
     * initialize logging
     */
    private void logIinitialize() {
        BasicConfigurator.configure();
/*    File file = new File(CONSOLE_CONFIG+File.separator+"log4j.properties");
    if (file.exists()) {
      PropertyConfigurator.configure(file.getAbsolutePath());
    } else {
      BasicConfigurator.configure();
    }*/
    }

    /**
     * initialize ssl
     */
    private void sslIinitialize() {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        System.
                setProperty("java.protocol.handler.pkgs",
                        "com.sun.net.ssl.internal.www.protocol");
        // try to actually load the https URL.
        // This is how we verify that ssl support was registered.
        // In JWS additional registration (setURLStreamHandlerFactory) is
        // required for not known reasons.
        try {
            new URL("https://localhost:8080");
        } catch (MalformedURLException e) {
            URL
                    .setURLStreamHandlerFactory(
                            new URLStreamHandlerFactory() {
                                public URLStreamHandler
                                        createURLStreamHandler(final String protocol) {
                                    if (protocol != null && protocol.compareTo("https") == 0) {
                                        return new
                                                com.sun.net.ssl.internal.www.protocol.https.Handler();
                                    }
                                    return null;
                                }
                            });
            // JSSE SSLContext initialization on a separate thread,
            // attempt to improve performance on app startup. The
            // Sun SSL provider is hardcoded.
            new Thread(
                    new Runnable() {
                        public void run() {
                            /*
                            com.sun.net.ssl.TrustManagerFactory
                              tmf = com.sun.net.ssl.TrustManagerFactory.getInstance("SunX509", "SunJSSE");
                            com.sun.net.ssl.KeyManagerFactory
                              kmf = com.sun.net.ssl.KeyManagerFactory.getInstance("SunX509", "SunJSSE");
                            */
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
    }

    /**
     * Dump the contests of the InputStream into the specified
     * <CODE>File</CODE>..
     *
     * Note that method is private, and does not check for
     * <B>null</B> parameters.
     *
     * @param input    the InputStream the source
     * @param destfile th destination file
     * @exception IOException
     */
    private void dumpStreamToFile(InputStream input, File destfile)
            throws IOException {

        byte[] bytearray = new byte[512];
        int len = 0;
        // delete if exists
        if (destfile.exists()) {
            destfile.delete();
        }
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(destfile);
            while ((len = input.read(bytearray)) != -1) {
                output.write(bytearray, 0, len);
            }
        } catch (FileNotFoundException e) {
            log("copyStreamToFile", e);
        } catch (SecurityException e) {
            log("copyStreamToFile", e);
        } finally {
            output.close();
        }
    }

    /**
     * Returns the value associated with the specified key
     * Returns the specified default if there is no value
     * associated with the key.
     *
     * @param key    key whose associated value is to be returned.
     * @param def    the value to be returned in the event that this
     *               preference key has no value
     * @return the value associated with key, or def if no value is
     *         associated with key.
     * @exception NullPointerException
     *                   if key is null. (A null value for def is permitted.)
     */
    public String getString(String key, String def)
            throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        return props.getProperty(key, def);
    }

    /**
     * Returns the value associated with the specified key
     *
     * @param key    key whose associated value is to be returned.
     * @return the value associated with key, or null if no value is
     *         associated with key.
     * @exception NullPointerException
     *                   if key is null. (A null value for def is permitted.)
     */
    public String getString(String key) {
        if (key == null) throw new NullPointerException("key == null");
        return props.getProperty(key);
    }

    /**
     * Returns a pair of properties as a Dimension.
     * More of a convenience method.
     *
     * @param widthKey the key where the width is stored
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


    /** Get the last screen size, or null if not set. */
    public Dimension getLastScreenSize() {
        return getDimension(LAST_SCREEN_SIZE_WIDTH, LAST_SCREEN_SIZE_HEIGHT);
    }

    /** Set the last screen size. */
    public void setLastScreenSize(Dimension d) {
        setDimension(LAST_SCREEN_SIZE_WIDTH, LAST_SCREEN_SIZE_HEIGHT, d);
    }

    /** Get the last window size, or null if not set. */
    public Dimension getLastWindowSize() {
        return getDimension(LAST_WINDOW_SIZE_WIDTH, LAST_WINDOW_SIZE_HEIGHT);
    }

    /** Set the last window size. */
    public void setLastWindowSize(Dimension d) {
        setDimension(LAST_WINDOW_SIZE_WIDTH, LAST_WINDOW_SIZE_HEIGHT, d);
    }

    /** Get the last window location, or null if not set. */
    public Point getLastWindowLocation() {
        // For now we'll just abuse getDimension().
        Dimension d = getDimension(LAST_WINDOW_LOC_X, LAST_WINDOW_LOC_Y);
        if (null == d)
            return null;
        return new Point(d.width, d.height);
    }

    /** Set the last window location. */
    public void setLastWindowLocation(Point p) {
        // For now we'll just abuse setDimension().
        Dimension d = p != null ? new Dimension(p.x, p.y) : null;
        setDimension(LAST_WINDOW_LOC_X, LAST_WINDOW_LOC_Y, d);
    }

    /**
     * Associates the specified value with the specified key in this
     * preference node.
     *
     * @param key    key with which the specified value is to be associated.
     * @param value  value to be associated with the specified key.
     * @exception NullPointerException
     *                   if key or value is null.
     */
    public void putProperty(String key, String value) throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        if (value == null) throw new NullPointerException("value == null");
        Object old = props.setProperty(key, value);
        PropertyChangeEvent e = new PropertyChangeEvent(this, key, old, value);
        firePropertyChange(e);
    }

    /**
     * Removes the key (and its corresponding value) from this
     * preferences. This method does nothing if the key is not
     * in the preferences.
     *
     * @param key    the key that needs to be removed.
     * @exception NullPointerException
     *                   if key or value is null.
     */
    public void remove(String key) throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        Object old = props.remove(key);
        PropertyChangeEvent e = new PropertyChangeEvent(this, key, old, null);
        firePropertyChange(e);
    }

    /**
     * Returns the service url. This will typically be in the format
     * protocol://host:port.
     *
     * @return the service url.
     */
    public String getServiceUrl() {
        return props.getProperty(SERVICE_URL) + SERVICE_URL_SUFFIX;
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
     * prints this property list out to the specified output
     * stream.
     *
     * @param ps     an output stream.
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
     *
     * @exception IOException
     *                   thrown if an io error occurred
     */
    protected void initialize() throws IOException {
        if (CONSOLE_CONFIG == null) {
            throw new IOException("Invalid/null home directory.");
        }
        File file = new File(CONSOLE_CONFIG + File.separator + STORE);

        if (file.exists()) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(file);
                props.load(fin);
            } finally {
                if (fin != null) {
                    fin.close();
                }
            }
        }
    }

    /**
     * simple log (no log4j used here)
     *
     * @param msg    the message to log
     */
    private void log(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }

    /**
     * simple log (no log4j used here)
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


    /** look and feel key */
    public static final String LOOK_AND_FEEL = "look.and.feel";

    /** look and feel key */
    public static final String INACTIVITY_TIMEOUT = "inactivity.timeout";

    /** last login id */
    public static final String LAST_LOGIN_ID = "last.login.id";

    /** remember last login id */
    public static final String SAVE_LAST_LOGIN_ID = "last.login.id.save";

    /** toolbars property (icons, text, icons and text) */
    public static final String STATUS_BAR_VISIBLE = "status.bar.enable";

/** text only */
    public static final Integer TEXT = new Integer(1);
    /** icons only */
    public static final Integer ICONS = new Integer(2);
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


    /** the file name for the preferences */
    protected static final String STORE = "ssg.properties";

    /** where is home (properties are stored there) */
    private final String CONSOLE_CONFIG =
            System.getProperties().getProperty("user.home") + File.separator + ".ssg";

    private static final
    String[] res =
            new String[]
            {
                /*"log4j.properties",*/
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
