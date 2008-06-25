package com.l7tech.console.util;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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
 * <i>Migrate to JDK 1.4 HeavySsmPreferences.</i>
 * <p/>
 * The class is not synchronized.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @see java.beans.PropertyChangeSupport
 */
public class HeavySsmPreferences extends AbstractSsmPreferences implements SsmPreferences {

    /**
     * the file name for the preferences
     */
    protected static final String STORE = "ssg.properties";
    /**
     * where is home (properties are stored there)
     */
    public static final String SSM_USER_HOME =
      System.getProperties().getProperty("user.home") + File.separator + ".l7tech";
    protected final String TRUST_STORE_FILE = SSM_USER_HOME + File.separator + "trustStore";
    protected final String TRUST_STORE_PASSWORD = "password";

    public HeavySsmPreferences() {
        setupDefaults();
        try {
            initialize();
        } catch (IOException e) {
            log("initialize", e);
        }

        /* load user preferences and merge them with system props */
        updateFromProperties(System.getProperties(), false);
        /* so it is visible in help/about */
        updateSystemProperties();
        // ensure trust store exists
        initializeSsgCertStorage();
    }

    /**
     * setup default application properties
     */
    private void setupDefaults() {
        prepare();
        configureProperties();
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
        Map<String, String> knownProps = new HashMap<String, String>();
        knownProps.put("javax.net.ssl.trustStore", new File(TRUST_STORE_FILE).getAbsolutePath());
        knownProps.put("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);

        for (String key : knownProps.keySet()) {
            if (null == props.getProperty(key)) {
                putProperty(key, knownProps.get(key));
            }
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

    public void updateSystemProperties() {
        System.getProperties().putAll(props);
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
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] trustStorPassword = TRUST_STORE_PASSWORD.toCharArray();
        String trustStoreFile = TRUST_STORE_FILE;
        try {
            FileInputStream ksfis = new FileInputStream(trustStoreFile);
            try {
                ks.load(ksfis, trustStorPassword);
            } finally {
                ksfis.close();
            }
        } catch (FileNotFoundException e) {
            // Create a new one.
            ks.load(null, trustStorPassword);
        }

        logger.info("Adding certificate: " + cert);
        ks.setCertificateEntry(hostname, cert);

        FileOutputStream ksfos = null;
        try {
            ksfos = new FileOutputStream(trustStoreFile);
            ks.store(ksfos, trustStorPassword);
        } finally {
            if (ksfos != null)
                ksfos.close();
        }
    }

    private void initializeSsgCertStorage() {
        try {
            getTrustStore();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error initializing certificate storage.", e);
        }
    }

    private KeyStore getTrustStore()
      throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        File storeFile = new File(TRUST_STORE_FILE);
        String defaultKeystoreType = KeyStore.getDefaultType();
        KeyStore ts = KeyStore.getInstance(defaultKeystoreType);
        final char[] password = TRUST_STORE_PASSWORD.toCharArray();

        if (!storeFile.exists()) {
            ts.load(null, null);
            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream(storeFile);
                ts.store(fo, password);
            } finally {
                ResourceUtils.closeQuietly(fo);
            }
            logger.fine("Emtpy Internal Admin trustStore created - " + TRUST_STORE_FILE);
            return ts;
        }
        final FileInputStream fis = new FileInputStream(storeFile);
        try {
            ts.load(fis, password);
        } finally {
            fis.close();
        }
        logger.fine("Internal admin trustStore opened - " + TRUST_STORE_FILE);
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
        }
    }
}
