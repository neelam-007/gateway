package com.l7tech.console.util;

import com.l7tech.common.util.ExceptionUtils;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * The Preferences class is a console property manager, that is
 * implemented as a light light wrapper arround
 * the <CODE>Properties</CODE> class.
 * It adds specific implementation for saving preferences,
 * (save preferences in home directory) and constants for
 * well known keys.
 * <p/>
 * The implementation uses the <CODE>PropertyChangeSupport</CODE>
 * to notify listeners of properties changes.
 * <p/>
 * <i>Migrate to JDK 1.4 HeavySsmPreferences.</i>
 * <p/>
 * The class is not synchronized.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @see java.beans.PropertyChangeSupport
 */
public class HeavySsmPreferences extends AbstractSsmPreferences implements SsmPreferences {
    private static HeavySsmPreferences prefs = null;
    /**
     * the file name for the preferences
     */
    protected static final String STORE = "ssg.properties";
    /**
     * where is home (properties are stored there)
     */
    public static final String SSM_USER_HOME =
      System.getProperties().getProperty("user.home") + File.separator + ".l7tech";
    private static final String DEFAULT_TRUST_STORE_FILE = SSM_USER_HOME + File.separator + "trustStore";
    private static final String DEFAULT_TRUST_STORE_PASS = "password";
    private static final String DEFAULT_TRUST_STORE_TYPE = "JKS";
    private static final String SYSPROP_TRUST_STORE_FILE = "javax.net.ssl.trustStore";
    private static final String SYSPROP_TRUST_STORE_PASS = "javax.net.ssl.trustStorePassword";
    private static final String SYSPROP_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

    /**
     * Deprecated properties will be removed from the properties file
     */
    private static final String[] DEPRECATED_PROPERTIES = { SYSPROP_TRUST_STORE_FILE, SYSPROP_TRUST_STORE_PASS };

    /**
     * private constructor use getPreferences to
     * instantiate the HeavySsmPreferences
     */
    protected HeavySsmPreferences() {
    }

    /**
     * setup default application properties
     */
    private void setupDefaults() {
        prepare();
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


    public static HeavySsmPreferences getPreferences() {
        if (prefs != null) return prefs;
        prefs = new HeavySsmPreferences();

//        if (null == System.getProperties().getProperty("javawebstart.version")) {
//            prefs = new HeavySsmPreferences();
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

    public void updateSystemProperties() {
        System.getProperties().putAll(props);

        System.setProperty( SYSPROP_TRUST_STORE_FILE, System.getProperty( SYSPROP_TRUST_STORE_FILE, DEFAULT_TRUST_STORE_FILE ) );
        System.setProperty( SYSPROP_TRUST_STORE_PASS, System.getProperty( SYSPROP_TRUST_STORE_PASS, DEFAULT_TRUST_STORE_PASS ) );
        System.setProperty( SYSPROP_TRUST_STORE_TYPE, System.getProperty( SYSPROP_TRUST_STORE_TYPE, DEFAULT_TRUST_STORE_TYPE ) );
    }

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

    public String getHomePath() {
        return SSM_USER_HOME;
    }

    public void importSsgCert(X509Certificate cert, String hostname) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        String trustStoreFile = System.getProperty( SYSPROP_TRUST_STORE_FILE, DEFAULT_TRUST_STORE_FILE );
        char[] trustStorePass = System.getProperty( SYSPROP_TRUST_STORE_PASS, DEFAULT_TRUST_STORE_PASS ).toCharArray();
        String trustStoreType = System.getProperty( SYSPROP_TRUST_STORE_TYPE, DEFAULT_TRUST_STORE_TYPE );
        KeyStore ks = KeyStore.getInstance( trustStoreType );
        try {
            FileInputStream ksfis = new FileInputStream(trustStoreFile);
            try {
                ks.load(ksfis, trustStorePass);
            } finally {
                ksfis.close();
            }
        } catch (FileNotFoundException e) {
            // Create a new one.
            ks.load(null, trustStorePass);
        }

        logger.info("Adding certificate: " + cert);
        ks.setCertificateEntry(hostname, cert);

        FileOutputStream ksfos = null;
        try {
            ksfos = new FileOutputStream(trustStoreFile);
            ks.store(ksfos, trustStorePass);
        } finally {
            if (ksfos != null)
                ksfos.close();
        }
    }

    public void initializeSsgCertStorage() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        getTrustStore();
    }

    private KeyStore getTrustStore() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        String trustStoreFile = System.getProperty( SYSPROP_TRUST_STORE_FILE, DEFAULT_TRUST_STORE_FILE );
        char[] trustStorePass = System.getProperty( SYSPROP_TRUST_STORE_PASS, DEFAULT_TRUST_STORE_PASS ).toCharArray();
        String trustStoreType = System.getProperty( SYSPROP_TRUST_STORE_TYPE, DEFAULT_TRUST_STORE_TYPE );

        File storeFile = new File(trustStoreFile);
        KeyStore ts = KeyStore.getInstance( trustStoreType );

        if (!storeFile.exists()) {
            ts.load(null, null);
            FileOutputStream fo = new FileOutputStream(storeFile);
            try {
                ts.store(fo, trustStorePass);
            } finally {
                fo.close();
            }
            logger.fine("Emtpy Internal Admin trustStore created - " + trustStoreFile);
            return ts;
        }
        final FileInputStream fis = new FileInputStream(storeFile);
        try {
            ts.load(fis, trustStorePass);
        } finally {
            fis.close();
        }
        logger.fine("Internal admin trustStore opened - " + trustStoreFile);
        return ts;
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
                        log(ExceptionUtils.getMessage(e), e);
                    }
                }
            }

            for ( String prop : DEPRECATED_PROPERTIES ) {
                props.remove( prop );
            }
        }
    }
}
