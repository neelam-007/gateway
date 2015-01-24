package com.l7tech.console.util;

import org.springframework.context.support.ApplicationObjectSupport;

import java.awt.*;
import java.io.PrintStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of SsmPreferences shared between applet and fat client.
 */
public abstract class AbstractSsmPreferences extends ApplicationObjectSupport implements SsmPreferences {
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    protected static final Logger logger = Logger.getLogger(AbstractSsmPreferences.class.getName());
    protected static boolean debug = false;
    protected Properties defaultProps = new Properties();
    protected Properties props = new Properties(defaultProps);
    private static final int DEFAULT_INACTIVITY_TIMEOUT = 30;

    @Override
    public void updateFromProperties(Properties p, boolean append) {
        for (Object o : p.keySet()) {
            String key = (String) o;
            if (append) {
                putProperty(key, p.getProperty(key));
            } else {
                if (null != props.getProperty(key)) {
                    putProperty(key, p.getProperty(key));
                }
            }
        }
    }

    @Override
    public String getString(String key, String def)
      throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        return props.getProperty(key, def);
    }

    @Override
    public String getString(String key) {
        if (key == null) throw new NullPointerException("key == null");
        return props.getProperty(key);
    }

    @Override
    public int getIntProperty( final String key, final int def ) {
        if (key == null) throw new NullPointerException("key == null");
        final String value = props.getProperty(key);
        try {
            return Integer.parseInt(value);
        } catch ( NumberFormatException e ) {
            return def;
        }
    }

    @Override
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

    @Override
    public Dimension getLastScreenSize() {
        return getDimension(LAST_SCREEN_SIZE_WIDTH, LAST_SCREEN_SIZE_HEIGHT);
    }

    @Override
    public void setLastScreenSize(Dimension d) {
        setDimension(LAST_SCREEN_SIZE_WIDTH, LAST_SCREEN_SIZE_HEIGHT, d);
    }

    @Override
    public Dimension getLastWindowSize() {
        return getDimension(LAST_WINDOW_SIZE_WIDTH, LAST_WINDOW_SIZE_HEIGHT);
    }

    @Override
    public void setLastWindowSize(Dimension d) {
        setDimension(LAST_WINDOW_SIZE_WIDTH, LAST_WINDOW_SIZE_HEIGHT, d);
    }

    @Override
    public Point getLastWindowLocation() {
        // For now we'll just abuse getDimension().
        Dimension d = getDimension(LAST_WINDOW_LOC_X, LAST_WINDOW_LOC_Y);
        if (null == d)
            return null;
        return new Point(d.width, d.height);
    }

    @Override
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
    @Override
    public void putProperty(String key, String value) throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        if (value == null) throw new NullPointerException("value == null");
        props.setProperty(key, value);
    }

    /**
     * Removes the key (and its corresponding value) from this
     * preferences. This method does nothing if the key is not
     * in the preferences.
     *
     * @param key the key that needs to be removed.
     * @throws NullPointerException if key or value is null.
     */
    @Override
    public void remove(String key) throws NullPointerException {
        if (key == null) throw new NullPointerException("key == null");
        props.remove(key);
        //firePropertyChange(e);
    }

    @Override
    public String getServiceUrl() {
        return props.getProperty(SERVICE_URL);
    }

    @Override
    public int getInactivityTimeout() {
        //when the inactivity timeout was never set to begin with, we'll use this default
        //note: this value MUST be the same as the value in PreferencesDialog.java
        return getIntProperty( INACTIVITY_TIMEOUT, DEFAULT_INACTIVITY_TIMEOUT );
    }

    @Override
    public boolean rememberLoginId() {
        return Boolean.
          valueOf(props.getProperty(SAVE_LAST_LOGIN_ID)).booleanValue();
    }

    /**
     * Returns the shortcut bar  visible propery value.
     *
     * @return the shortcut bar visible value as boolean.
     */
    @Override
    public boolean isStatusBarBarVisible() {
        // default set
        String sbprop = props.getProperty(STATUS_BAR_VISIBLE);
        return sbprop == null || Boolean.valueOf(sbprop).booleanValue();
    }

    @Override
    public void seStatusBarVisible(boolean b) {
        putProperty(STATUS_BAR_VISIBLE, Boolean.toString(b));
    }

    /**
     * Returns the policy messages visible property value.
     *
     * @return the policy messages visible value as boolean.
     */
    @Override
    public boolean isPolicyMessageAreaVisible() {
        // default set
        String pmaprop = props.getProperty(POLICY_MSG_AREA_VISIBLE);
        return pmaprop == null || Boolean.valueOf(pmaprop).booleanValue();
    }

    @Override
    public boolean isPolicyInputsAndOutputsVisible() {
        // default set
        String pmaprop = props.getProperty( POLICY_INPUTS_AND_OUTPUTS_VISIBLE );
        return pmaprop == null || Boolean.valueOf( pmaprop );
    }

    @Override
    public void setPolicyMessageAreaVisible(boolean b) {
        putProperty(POLICY_MSG_AREA_VISIBLE, Boolean.toString(b));
    }

    @Override
    public void setPolicyInputsAndOutputsVisible( boolean b ) {
        putProperty( POLICY_INPUTS_AND_OUTPUTS_VISIBLE, Boolean.toString( b ) );
    }

    @Override
    public void list(PrintStream ps) {
        props.list(ps);
    }

    @Override
    public Properties asProperties() {
        Properties p = new Properties();
        p.putAll(props);
        return p;
    }

    private class PropertyHistory implements History {
        private String propertyName;
        /*For 5.0 added getter and setter for this property
        * leaving default here as 5. Only use case of this class so far is in the Logon Dialog for Server URL
        * however as it is generic and can be used for any property, I'm leaving maxSize's with it's original default
        * value and not pulling any default value out from a property file */
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
        @Override
        public void add(Object o) {
            LinkedList<Object> values = values();
            Collection<Object> remove = new ArrayList<Object>();
            for (Object value : values) {
                if (value.equals(o)) {
                    remove.add(value);
                }
            }
            values.removeAll(remove);
            values.addFirst(o);
            rekeyValues(values);
        }


        private LinkedList<Object> values() {
            SortedSet<Object> sortedKeys = new TreeSet<Object>(new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    //service.url
                    //service.url.1

                    if(o1.toString().equals(o2.toString())) return 0;

                    int index1 = o1.toString().lastIndexOf(".");
                    //lowest index always has no index e.g. service.url
                    if(index1 < propertyName.length()) return -1;

                    int index2 = o2.toString().lastIndexOf(".");
                    if(index2 < propertyName.length()) return 1;

                    //get their indexes to compare
                    Integer i1 = new Integer(o1.toString().substring(index1+1));
                    Integer i2 = new Integer(o2.toString().substring(index2+1));

                    return i1.compareTo(i2);
                }
            });

            Enumeration enumeration = props.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                if (key.toString().startsWith(propertyName)) {
                    sortedKeys.add(key);
                }
            }

            LinkedList<Object> values = new LinkedList<Object>();
            for (Object key : sortedKeys) {
                values.add(props.get(key));
            }
            return values;
        }

        private void rekeyValues(List values) {
            Enumeration enumeration = props.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                if (key.toString().startsWith(propertyName)) {
                    remove(key.toString());
                }
            }
            int index = 0;
            for (Iterator iterator = values.iterator(); iterator.hasNext() && index < maxSize;) {
                Object value = iterator.next();
                String pn;
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
        @Override
        public Object[] getEntries() {
            return values().toArray();
        }

        /*
       * Set how large the history can get
       * What ever value you set here is reflected immediately in the number of values
       * returned by getEntries()
       * */
        @Override
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
            LinkedList<Object> values = values();
            rekeyValues(values);
        }

        /*
       * @return how large the history is allowed for this instance of History
           * */
        @Override
        public int getMaxSize() {
            return maxSize;
        }
    }

    @Override
    public Set<X509Certificate> getKeys() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        return new HashSet<X509Certificate>();
    }

    @Override
    public Set<X509Certificate> getCertificates() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        return new HashSet<X509Certificate>();
    }

    @Override
    public void importPrivateKey(X509Certificate[] cert, PrivateKey privateKey)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
    }

    @Override
    public void deleteCertificate(X509Certificate cert)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
    }

    @Override
    public X509Certificate getClientCertificate() {
        return null;
    }

    @Override
    public void setClientCertificate(X509Certificate cert) {
    }
}
