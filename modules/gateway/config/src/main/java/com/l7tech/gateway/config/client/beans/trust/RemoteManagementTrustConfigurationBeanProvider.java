/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.ProcessControllerConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.ConfigurationBean;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/** @author alex */
public class RemoteManagementTrustConfigurationBeanProvider extends ProcessControllerConfigurationBeanProvider {
    private static final Logger logger = Logger.getLogger(RemoteManagementTrustConfigurationBeanProvider.class.getName());
    private static final String FS = System.getProperty("file.separator");

    // TODO find some place to put these where we can share them with ConfigService 
    String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE = "host.controller.remoteNodeManagement.truststore.file";
    String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE = "host.controller.remoteNodeManagement.truststore.type";
    String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD = "host.controller.remoteNodeManagement.truststore.password";

    private static final String DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME = "remoteNodeManagementTruststore.p12";
    static final String ENABLED_ID = "host.controller.remoteNodeManagement.enabled";
    static final String ENABLED_NAME = "Remote Node Management Enabled";
    private static final ConfigurationBean DISABLED_BEAN = new ConfigurationBean(ENABLED_ID, ENABLED_NAME, "false");
    private static final ConfigurationBean ENABLED_BEAN = new ConfigurationBean(ENABLED_ID, ENABLED_NAME, "true");


    public RemoteManagementTrustConfigurationBeanProvider(final URL nodeManagementUrl) {
        super(nodeManagementUrl);
    }

    private static class TrustedCertBean extends ConfigurationBean {
        private final int index;
        private final X509Certificate cert;

        private TrustedCertBean(int index, X509Certificate xcert) throws GeneralSecurityException {
            super("host.controller.remoteNodeManagement.trustedCert." + index, "Remote Node Management Trusted Certificate #" + index, CertUtils.getCertificateFingerprint(xcert, "SHA1", "hex"));
            this.index = index;
            this.cert = xcert;
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        final File pcHomeDir = new File(".." + FS + "controller");
        final File etcDirectory = new File(pcHomeDir, "etc");
        final File etcConfDirectory = new File(etcDirectory, "conf");

        final MasterPasswordManager masterPasswordManager = new MasterPasswordManager( new DefaultMasterPasswordFinder( new File(etcConfDirectory, "omp.dat") ) );

        final File hostPropsFile = new File(etcDirectory, "host.properties");
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

        final String trustStoreFilename = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE, new File(etcDirectory, DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME).getAbsolutePath());
        final File tsFile = new File(trustStoreFilename);
        if (!tsFile.exists()) {
            logger.info("Remote node management truststore " + tsFile.getAbsolutePath() + " does not exist; remote node management is disabled");
            return Collections.singleton(DISABLED_BEAN);
        }

        final String trustStoreType = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE, "PKCS12");
        final String pass = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD);
        if (pass == null) throw new ConfigurationException("Node management truststore password was not found in " + hostPropsFile.getAbsolutePath());
        char[] trustStorePass = masterPasswordManager.decryptPasswordIfEncrypted(pass);

        final Set<ConfigurationBean> beans = new HashSet<ConfigurationBean>();
        final KeyStore ks;
        try {
            fis = new FileInputStream(tsFile);
            ks = KeyStore.getInstance(trustStoreType);
            ks.load(fis, trustStorePass);
            final Enumeration<String> aliases = ks.aliases();
            int i = 0;
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = ks.getCertificate(alias);
                if (!(cert instanceof X509Certificate)) continue;
                beans.add(new TrustedCertBean(i++, (X509Certificate)cert));
            }

            if (beans.isEmpty()) {
                return Collections.singleton(DISABLED_BEAN);
            } else {
                beans.add(ENABLED_BEAN);
                return beans;
            }
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException("Couldn't open trust store", e);
        } catch (IOException e) {
            throw new ConfigurationException("Couldn't load trust store", e);
        }
    }

    @Override
    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {

    }

    private static enum InputType {
        YES_NO("Y/N"),
        STRING("Enter a value"),
        INTEGER("Enter an integer"),
        URL("Enter a URL"),
        DELETE_ONLY("<delete only>");

        private InputType(String prompt) {
            this.prompt = prompt;
        }

        private final String prompt;

        public String getPrompt() {
            return prompt;
        }
    }

}
