/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.util.KeystoreInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.remote.jini.export.RemoteService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class AdminServicesBootProcess implements ServerComponentLifecycle {

    public void setComponentConfig(ComponentConfig config) throws LifecycleException {
        try {
            initializeSslEnvironment();
        } catch (Exception e) {
            throw new LifecycleException("Error initializing admin services. The admin services may not be available.", e);
        }
    }

    /**
     * Initialize the SSL environment. Sets the ssl system properties (javax.net.ssl.*)
     * and creates the internal truststore dynamically.
     * Currently the JERI SSL components do not support programmatic SSL initialization
     * using {@link javax.net.ssl.SSLContext#init(javax.net.ssl.KeyManager[], javax.net.ssl.TrustManager[], java.security.SecureRandom)}
     * so it has to be done with system properties.
     */
    private void initializeSslEnvironment()
      throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        KeystoreUtils ku = KeystoreUtils.getInstance();
        KeystoreInfo ki = ku.getSslKeystoreInfo();
        String kspwd = ki.getStorePassword();
        String kspath = ki.getStoreFile();
        String kstype = ki.getStoreType();
        Properties props = System.getProperties();

        logger.finest("Set 'javax.net.ssl.keyStore' to " + kspath);
        props.setProperty("javax.net.ssl.keyStore", kspath);

        logger.finest("Set 'javax.net.ssl.keyStorePassword'");
        props.setProperty("javax.net.ssl.keyStorePassword", kspwd);

        logger.finest("Setting 'javax.net.ssl.keyStoreType' to " + kstype);
        props.setProperty("javax.net.ssl.keyStoreType", kstype);

        String targetDir = new File(kspath).getParent();
        if (targetDir == null || !new File(targetDir).exists()) {
            throw new FileNotFoundException("target directory " + targetDir);
        }
        String store = targetDir + File.separator + "admin.ks";
        deleteIfExists(store);
        File storeFile = new File(store);
        storeFile.deleteOnExit();
        String defaultKeystoreType = KeyStore.getDefaultType();
        KeyStore ts = KeyStore.getInstance(defaultKeystoreType);
        ts.load(null, null);
        ts.setCertificateEntry("admin", ku.getSslCert());
        FileOutputStream fo = null;
        fo = new FileOutputStream(storeFile);
        ts.store(fo, kspwd.toCharArray());
        fo.close();
        logger.log(Level.FINEST, "Internal admin trustStore created in " + store);

        logger.finest("Set 'javax.net.ssl.trustStore' to " + store);
        props.setProperty("javax.net.ssl.trustStore", store);

        logger.finest("Set 'javax.net.ssl.trustStorePassword'");
        props.setProperty("javax.net.ssl.trustStorePassword", kspwd);
        logger.finest("Setting 'javax.net.ssl.trustStoreType' to " + defaultKeystoreType);
        props.setProperty("javax.net.ssl.trustStoreType", defaultKeystoreType);
    }

    /**
     * Delete the file if exists. If delete fails, throws IOException
     *
     * @param store the file path
     */
    private void deleteIfExists(String store) throws IOException {
        File storeFile = new File(store);
        if (storeFile.exists()) {
           if (!storeFile.delete()) {
               throw new IOException("Unable to delete the file "+storeFile);
           }
        }
    }

    public void start() throws LifecycleException {
    }

    public void stop() throws LifecycleException {
    }

    public void close() throws LifecycleException {
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
