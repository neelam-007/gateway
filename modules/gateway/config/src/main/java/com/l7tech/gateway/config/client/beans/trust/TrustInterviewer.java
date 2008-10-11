/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.ConfigInterviewer;
import com.l7tech.gateway.config.client.beans.ConfigurationBean;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class TrustInterviewer {
    private static final Logger logger = Logger.getLogger(RemoteManagementTrustConfigurationBeanProvider.class.getName());
    private static final String FS = System.getProperty("file.separator");

    // TODO find some place to put these where we can share them with ConfigService
    static String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE = "host.controller.remoteNodeManagement.truststore.file";
    static String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE = "host.controller.remoteNodeManagement.truststore.type";
    static String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD = "host.controller.remoteNodeManagement.truststore.password";
    static String HOSTPROPERTIES_NODEMANAGEMENT_ENABLED = "host.controller.remoteNodeManagement.enabled";

    private static final String DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME = "remoteNodeManagementTruststore.jks";
    public static void main(String[] args) throws ConfigurationException {
        final File pcHomeDir = new File(".." + FS + "controller");
        final File etcDirectory = new File(pcHomeDir, "etc");
        final File etcConfDirectory = new File(etcDirectory, "conf");

        final File hostPropsFile = new File(etcDirectory, "host.properties");
        final Properties hostProps = readHostProperties(hostPropsFile);

        final String trustStoreFilename = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE, new File(etcDirectory, DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME).getAbsolutePath());
        final File tsFile = new File(trustStoreFilename);

        final String trustStoreType = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE, "JKS");
        char[] trustStorePass = null;

        boolean enabled = "true".equalsIgnoreCase(hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_ENABLED));

        final MasterPasswordManager masterPasswordManager = new MasterPasswordManager( new DefaultMasterPasswordFinder( new File(etcConfDirectory, "omp.dat") ) );

        List<ConfigurationBean> inBeans = new ArrayList<ConfigurationBean>();
        Map<ConfiguredTrustedCert, String> inCertBeans = null;
        KeyStore trustStore = null;
        if (enabled && tsFile.exists()) {
            String pass = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD);
            if (pass == null) throw new ConfigurationException("Node management truststore password was not found in " + hostPropsFile.getAbsolutePath());
            trustStorePass = masterPasswordManager.decryptPasswordIfEncrypted(pass);

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(tsFile);
                trustStore = KeyStore.getInstance(trustStoreType);
                trustStore.load(fis, trustStorePass);
            } catch (GeneralSecurityException e) {
                throw new ConfigurationException("Couldn't open trust store", e);
            } catch (IOException e) {
                throw new ConfigurationException("Couldn't load trust store", e);
            } finally {
                ResourceUtils.closeQuietly(fis);
            }

            try {
                inCertBeans = readCertsFromTruststore(trustStore);
                inBeans.add(new RemoteNodeManagementEnabled(enabled));
                inBeans.addAll(inCertBeans.keySet());
            } catch (GeneralSecurityException e) {
                throw new ConfigurationException("Couldn't read certs from trust store", e);
            }
        } else {
            logger.info("Remote node management truststore " + tsFile.getAbsolutePath() + " does not exist; remote node management is disabled");
            inBeans.add(new RemoteNodeManagementEnabled(false));
        }

        List<ConfigurationBean> outBeans;
        try {
            outBeans = new ConfigInterviewer(inBeans.toArray(new ConfigurationBean[0])).doInterview();
        } catch (IOException e) {
            throw new ConfigurationException("UI problem", e);
        }

        boolean writeProps = false;
        boolean writeTruststore = false;
        Map<String, X509Certificate> deleteCerts = new HashMap<String, X509Certificate>();
        Map<String, X509Certificate> addCerts = new HashMap<String, X509Certificate>();
        for (ConfigurationBean bean : outBeans) {
            if (bean instanceof RemoteNodeManagementEnabled) {
                final boolean newEnabled = ((RemoteNodeManagementEnabled)bean).getConfigValue();
                if (newEnabled != enabled) {
                    hostProps.setProperty(HOSTPROPERTIES_NODEMANAGEMENT_ENABLED, newEnabled ? "true" : "false");
                    writeProps = true;
                }
            } else if (bean instanceof ConfiguredTrustedCert) {
                ConfiguredTrustedCert trustedCert = (ConfiguredTrustedCert)bean;
                X509Certificate cert = trustedCert.getConfigValue();
                if (inCertBeans == null || !inCertBeans.containsKey(trustedCert)) {
                    try {
                        addCerts.put("trustedCert-" + CertUtils.getThumbprintSHA1(cert) + "-" + System.currentTimeMillis(), cert);
                    } catch (CertificateEncodingException e) {
                        logger.warning("Couldn't get thumbprint for " + cert.getSubjectDN().getName() + "; skipping");
                    }
                }
            }
        }

        if (inCertBeans != null) {
            for (ConfiguredTrustedCert bean : inCertBeans.keySet()) {
                String oldAlias = inCertBeans.get(bean);
                boolean found = false;
                for (ConfigurationBean configurationBean : outBeans) {
                    if (configurationBean instanceof ConfiguredTrustedCert) {
                        ConfiguredTrustedCert configuredTrustedCert = (ConfiguredTrustedCert)configurationBean;
                        if (bean == configuredTrustedCert) found = true;
                    }
                }
                if (!found) deleteCerts.put(oldAlias, bean.getConfigValue());
            }
        }

        if (trustStore == null) {
            try {
                byte[] newpass = new byte[8];
                new SecureRandom().nextBytes(newpass);
                trustStorePass = HexUtils.hexDump(newpass).toCharArray();
                trustStore = KeyStore.getInstance(trustStoreType);
                trustStore.load(null, null);
                writeTruststore = true;
                hostProps.put(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE, tsFile.getCanonicalPath());
                hostProps.put(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD, masterPasswordManager.encryptPassword(trustStorePass));
                hostProps.put(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE, "JKS");
                writeProps = true;
            } catch (GeneralSecurityException e) {
                throw new ConfigurationException("Couldn't create trust store");
            } catch (IOException e) {
                throw new ConfigurationException("Couldn't create trust store");
            }
        }

        for (Map.Entry<String, X509Certificate> entry : addCerts.entrySet()) {
            final X509Certificate cert = entry.getValue();
            try {
                trustStore.setCertificateEntry(entry.getKey(), cert);
            } catch (KeyStoreException e) {
                logger.log(Level.WARNING, "Couldn't add new truststore entry for " + cert.getSubjectDN().getName() + "; skipping", e);
                continue;
            }
            writeTruststore = true;
        }

        for (Map.Entry<String, X509Certificate> entry : deleteCerts.entrySet()) {
            try {
                trustStore.deleteEntry(entry.getKey());
            } catch (KeyStoreException e) {
                logger.log(Level.WARNING, "Couldn't delete truststore entry " + entry.getValue().getSubjectDN().getName() + "; skipping");
                continue;
            }
            writeTruststore = true;
        }

        if (writeTruststore) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(tsFile);
                trustStore.store(fos, trustStorePass);
            } catch (IOException e) {
                throw new ConfigurationException("Couldn't write trust store", e);
            } catch (GeneralSecurityException e) {
                throw new ConfigurationException("Couldn't write trust store", e);
            } finally {
                ResourceUtils.closeQuietly(fos);
            }
        }

        if (writeProps) {
            OutputStreamWriter w = null;
            try {
                w = new OutputStreamWriter(new FileOutputStream(hostPropsFile));
                hostProps.store(w, null);
            } catch (IOException e) {
                throw new ConfigurationException("Couldn't write host.properties file", e);
            } finally {
                ResourceUtils.closeQuietly(w);
            }
        }
    }

    private static Properties readHostProperties(File hostPropsFile) throws ConfigurationException {
        if (!hostPropsFile.exists()) throw new ConfigurationException(hostPropsFile.getAbsolutePath() + " not found");
        final Properties hostProps = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(hostPropsFile);
            hostProps.load(fis);
        } catch (IOException e) {
            throw new ConfigurationException("Couldn't load " + hostPropsFile.getAbsolutePath(), e);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return hostProps;
    }

    private static Map<ConfiguredTrustedCert, String> readCertsFromTruststore(final KeyStore trustStore) throws GeneralSecurityException {
        Map<ConfiguredTrustedCert, String> beansFromTruststore = new HashMap<ConfiguredTrustedCert, String>();
        final Enumeration<String> aliases = trustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = trustStore.getCertificate(alias);
            if (!(cert instanceof X509Certificate)) continue;
            beansFromTruststore.put(new ConfiguredTrustedCert((X509Certificate)cert, true), alias);
        }
        return beansFromTruststore;
    }
}
