/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.common.io.CertUtils;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.options.OptionType;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.gateway.config.client.beans.ConfigInterviewer;
import com.l7tech.gateway.config.client.beans.TypedConfigurableBean;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

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
import java.text.ParseException;

/** @author alex */
public class TrustInterviewer {
    private static final Logger logger = Logger.getLogger(TrustInterviewer.class.getName());

    // TODO find some place to put these where we can share them with ConfigService
    static String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE = "host.controller.remoteNodeManagement.truststore.file";
    static String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE = "host.controller.remoteNodeManagement.truststore.type";
    static String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD = "host.controller.remoteNodeManagement.truststore.password";
    static String HOSTPROPERTIES_NODEMANAGEMENT_ENABLED = "host.controller.remoteNodeManagement.enabled";
    static String HOSTPROPERTIES_NODEMANAGEMENT_IPADDRESS ="host.controller.sslIpAddress";
    static String HOSTPROPERTIES_NODEMANAGEMENT_PORT ="host.controller.sslPort";

    private static final String DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME = "remoteNodeManagementTruststore.jks";
    public static void main(String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        new TrustInterviewer().run();
    }

    void run() {
        final File pcHomeDir = new File(SyspropUtil.getString("com.l7tech.server.controller.home", "/opt/SecureSpan/Appliance/controller"));
        final File etcDirectory = new File(pcHomeDir, "etc");
        final File etcConfDirectory = new File(etcDirectory, "conf");
        final File hostPropsFile = new File(etcDirectory, "host.properties");
        doTrustInterview( hostPropsFile, new File(etcConfDirectory, "omp.dat"), etcDirectory ) ;
    }

    void doTrustInterview( final File hostPropsFile, final File masterPasswordFile, final File etcDirectory ) {
        try {

            final Properties hostProps;
            try {
                hostProps = readHostProperties(hostPropsFile);
            } catch ( ConfigurationException ce ) {
                throw new ExitErrorException( 2, "Error reading configuration '"+ ExceptionUtils.getMessage(ce) +"'.");
            }

            final String trustStoreFilename = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE, new File(etcDirectory, DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME).getAbsolutePath());
            final File tsFile = new File(trustStoreFilename);

            final String trustStoreType = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE, "JKS");
            char[] trustStorePass = null;

            boolean enabled = "true".equalsIgnoreCase(hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_ENABLED));
            String listenIpAddr = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_IPADDRESS, "127.0.0.1");
            String listenPort = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_PORT, "8765");

            final MasterPasswordManager masterPasswordManager = new MasterPasswordManager( new DefaultMasterPasswordFinder( masterPasswordFile ) );

            final NewTrustedCertFactory trustedCertFactory = new NewTrustedCertFactory(1); // max=1, temporary fix for Bug #6979
            Map<ConfiguredTrustedCert, String> inCertBeans = null;
            KeyStore trustStore = null;
            if ( tsFile.exists() ) {
                String pass = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD);
                if (pass == null) throw new ExitErrorException(2, "Node management truststore password was not found in " + hostPropsFile.getAbsolutePath());
                trustStorePass = masterPasswordManager.decryptPasswordIfEncrypted(pass);

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(tsFile);
                    trustStore = KeyStore.getInstance(trustStoreType);
                    trustStore.load(fis, trustStorePass);
                } catch (GeneralSecurityException e) {
                    logger.log(Level.WARNING, "Couldn't open trust store", e);
                    throw new ExitErrorException(2, "Couldn't open trust store");
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Couldn't load trust store", e);
                    throw new ExitErrorException(2, "Couldn't load trust store");
                } finally {
                    ResourceUtils.closeQuietly(fis);
                }

                try {
                    inCertBeans = readCertsFromTruststore(trustStore, trustedCertFactory);
                } catch (GeneralSecurityException e) {
                    logger.log(Level.WARNING, "Couldn't read certs from trust store", e);
                    throw new ExitErrorException(2, "Couldn't read certs from trust store");
                }
            }

            trustedCertFactory.setConsumedInstances(inCertBeans.size());

            List<ConfigurationBean> inBeans = new ArrayList<ConfigurationBean>();
            inBeans.add(new RemoteNodeManagementEnabled(enabled));
            inBeans.add(trustedCertFactory);
            inBeans.add(new TypedConfigurableBean<String>( "host.controller.sslIpAddress", "Listener IP Address", "Valid inputs are any IP Address or * for all.", "localhost", listenIpAddr, OptionType.IP_ADDRESS ) );
            inBeans.add(new TypedConfigurableBean<Integer>( "host.controller.sslPort", "Listener Port", "Valid inputs are integers in the range 1024-65535.", 8765, Integer.parseInt(listenPort), OptionType.PORT, 1024, null ) );
            inBeans.addAll(inCertBeans.keySet());

            ResourceBundle bundle = ResourceBundle.getBundle("com/l7tech/gateway/config/client/beans/trust/TrustInterviewerMessages");
            List<ConfigurationBean> outBeans;
            try {
                outBeans = doInterview(inBeans, bundle);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error during configuration", e);
                throw new ExitErrorException(1, "Error during configuration");
            }

            if ( !outBeans.isEmpty() ) { // empty on QUIT
                boolean writeProps = false;
                boolean writeTruststore = false;
                boolean hasCert = false;
                Map<String, X509Certificate> deleteCerts = new HashMap<String, X509Certificate>();
                Map<String, X509Certificate> addCerts = new HashMap<String, X509Certificate>();
                for (ConfigurationBean bean : outBeans) {
                    if (bean instanceof ConfiguredTrustedCert) {
                        ConfiguredTrustedCert trustedCert = (ConfiguredTrustedCert)bean;
                        X509Certificate cert = trustedCert.getConfigValue();
                        if (inCertBeans == null || !inCertBeans.containsKey(trustedCert)) {
                            try {
                                hasCert = true;
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
                                if (bean == configuredTrustedCert) {
                                    hasCert = true;
                                    found = true;
                                }
                            }
                        }
                        if (!found) deleteCerts.put(oldAlias, bean.getConfigValue());
                    }
                }
                for (ConfigurationBean bean : outBeans) {
                    if ( bean instanceof RemoteNodeManagementEnabled ) {
                        final boolean newEnabled = ((RemoteNodeManagementEnabled)bean).getConfigValue() && hasCert;
                        if (newEnabled != enabled) {
                            hostProps.setProperty(HOSTPROPERTIES_NODEMANAGEMENT_ENABLED, newEnabled ? "true" : "false");
                            writeProps = true;
                        }
                    } else if ( bean instanceof TypedConfigurableBean ) {
                        if ( bean.getConfigValue()!=null) {
                            hostProps.setProperty( bean.getId(), bean.getConfigValue().toString() );
                        } else {
                            hostProps.remove( bean.getId() );
                        }
                        writeProps = true;
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
                        logger.log(Level.WARNING, "Couldn't create trust store", e);
                        throw new ExitErrorException(3, "Couldn't create trust store");
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Couldn't create trust store", e);
                        throw new ExitErrorException(3, "Couldn't create trust store");
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
                        logger.log(Level.WARNING, "Couldn't write trust store", e);
                        throw new ExitErrorException(3, "Couldn't write trust store");
                    } catch (GeneralSecurityException e) {
                        logger.log(Level.WARNING, "Couldn't write trust store", e);
                        throw new ExitErrorException(3, "Couldn't write trust store");
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
                        logger.log(Level.WARNING, "Couldn't write host.properties file", e);
                        throw new ExitErrorException(3, "Couldn't write host.properties file");
                    } finally {
                        ResourceUtils.closeQuietly(w);
                    }
                }
            }
        } catch ( ExitErrorException eee ) {
            System.out.println( eee.getMessage() );
            System.exit( eee.getExitCode() );
        }
    }

    List<ConfigurationBean> doInterview( final List<ConfigurationBean> inBeans,
                                         final ResourceBundle bundle) throws IOException {
        return new ConfigInterviewer(bundle, inBeans.toArray(new ConfigurationBean[inBeans.size()])).doInterview( new ConfigInterviewer.ConfigValidator(){
            public boolean isConfigurationValid( final ConfigInterviewer interviewer,
                                                 final Collection<ConfigurationBean> beans,
                                                 final BufferedReader in,
                                                 final PrintStream out ) {
                boolean ok = false;

                if ( !validConfiguration( beans ) ) {
                    if ( confirmInvalid( in, out, bundle ) ) {
                        ok = true;
                    }
                } else {
                    ok = true;
                }

                return ok;
            }
        } );
    }

    /**
     * Check if the given configuration beans represent a valid set of properties.
     *
     * <p>For configuration to be valid either remote management must be disabled
     * or the listener ip address must be a remotely accessible IP and at least
     * one trusted certificate must be present.</p>
     *
     * @param beans The beans for the configuration
     * @return true If the configuration is valid
     */
    private boolean validConfiguration( Collection<ConfigurationBean> beans )  {
        boolean remoteEnabled = false;
        for (ConfigurationBean bean : beans) {
            if ( bean instanceof RemoteNodeManagementEnabled ) {
                remoteEnabled = ((RemoteNodeManagementEnabled)bean).getConfigValue();
                break;
            }
        }

        boolean hasCert = false;
        for (ConfigurationBean bean : beans) {
            if (bean instanceof ConfiguredTrustedCert) {
                hasCert = true;
                break;
            }
        }

        boolean isLocalhost = false;
        for (ConfigurationBean bean : beans) {
            if (bean instanceof TypedConfigurableBean && "host.controller.sslIpAddress".equals(bean.getId())) {
                isLocalhost = "127.0.0.1".equals(bean.getConfigValue());
                break;
            }
        }

        return !remoteEnabled || (hasCert && !isLocalhost);
    }

    private boolean confirmInvalid( final BufferedReader in,
                                    final PrintStream out,
                                    final ResourceBundle bundle ) {
        System.out.print( bundle.getString("warning.remoteManagementEnable") );
        boolean valid = false;
        try {
            String value = in.readLine();
            if ( value!=null && !value.isEmpty() ) {
                valid = (Boolean) OptionType.BOOLEAN.getFormat().parseObject(value);
            }
        } catch (ParseException e) {
            logger.log( Level.WARNING, "Error confirming save.", e );
        } catch (IOException e) {
            logger.log( Level.WARNING, "Error confirming save.", e );
        }

        return valid;
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

    private static Map<ConfiguredTrustedCert, String> readCertsFromTruststore(final KeyStore trustStore, NewTrustedCertFactory trustedCertFactory) throws GeneralSecurityException {
        Map<ConfiguredTrustedCert, String> beansFromTruststore = new HashMap<ConfiguredTrustedCert, String>();
        final Enumeration<String> aliases = trustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = trustStore.getCertificate(alias);
            if (!(cert instanceof X509Certificate)) continue;
            beansFromTruststore.put(new ConfiguredTrustedCert((X509Certificate)cert, trustedCertFactory), alias);
            // Note, don't consume() here because we want to leave open the "back door" of modifying the truststore externally
        }
        return beansFromTruststore;
    }

    private static final class ExitErrorException extends Exception {
        final int exitCode;

        ExitErrorException( final int code, final String message ) {
            super( message );
            exitCode = code;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}
