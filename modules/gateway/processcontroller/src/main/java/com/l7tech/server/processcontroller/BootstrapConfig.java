/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.security.cert.BouncyCastleCertUtils;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.*;
import com.l7tech.server.management.config.host.HostConfig;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;

/** @author alex */
public class BootstrapConfig {
    private static final Logger logger = Logger.getLogger(BootstrapConfig.class.getName());
    private static final String DEFAULT_ALIAS = "processController";
    private static final SecureRandom rand = new SecureRandom();

    public static void main(String[] args) {
        try {
            System.setProperty("com.l7tech.common.security.jceProviderEngineName", "BC");
            System.exit(realMain());
        } catch (DieDieDie e) {
            logger.log(Level.SEVERE, e.getMessage(), e.getCause());
            System.exit(e.status);
        }
    }

    private static class DieDieDie extends RuntimeException {
        private final int status;

        private DieDieDie(String message, int status, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        private DieDieDie(String message, int status) {
            super(message);
            this.status = status;
        }
    }

    private static void initLogging() {
        // configure logging if the logs directory is found, else leave console output
        final File logsDir = new File("var/logs");
        if ( logsDir.exists() && logsDir.canWrite() ) {
            JdkLoggerConfigurator.configure("com.l7tech.server.processcontroller", "com/l7tech/server/processcontroller/resources/logging.properties", "etc/conf/logging.properties", false, true);
        }
        if ( SyspropUtil.getBoolean( "com.l7tech.server.log.console", false ) ) {
            Logger.getLogger( "" ).addHandler( new ConsoleHandler() );
        }
    }

    private static int realMain() {
        final File etcDir = checkFile(new File("etc"), true);
        final File hostPropertiesFile = new File(etcDir, "host.properties");
        final File overrideProperties = new File(etcDir, "override.properties");

        initLogging();
        logger.info( "Starting configuration bootstrap " + BuildInfo.getLongBuildString());

        if (hostPropertiesFile.exists()) {
            logger.fine("Found existing " + hostPropertiesFile.getAbsolutePath());
            logger.fine("Checking for overrides");
        } else {
            final File ksFile = checkFile(new File(etcDir, "localhost.p12"), false);
            final String ksPass = generatePassword();
            createKeystore(ksFile, ksPass);
            createPropertiesFile(hostPropertiesFile, ksFile, getMasterPasswordManager(etcDir).encryptPassword(ksPass.toCharArray()));
        }

        doPropertiesUpdates(hostPropertiesFile, overrideProperties);
        logger.info( "Configuration bootstrap complete." );
        return 0;
    }

    private static void doPropertiesUpdates( final File hostPropertiesFile,
                                             final File overridePropertiesFile) {
        boolean updated = false;

        final Properties hostProperties = loadProperties(hostPropertiesFile);

        boolean processedOverrides = false;
        if ( !overridePropertiesFile.exists() ) {
            logger.fine("No override properties found, no need to merge with host properties.");
        } else {
            updated = doOverrideProperties( hostProperties, overridePropertiesFile );
            processedOverrides = true;
        }

        if ( doPropertiesUpgrade( hostProperties ) ) {
            updated = true;
        }

        if ( updated ) {
            saveProperties( hostProperties, hostPropertiesFile );
            logger.info("Updated " + hostPropertiesFile.getAbsolutePath());
        }

        if ( processedOverrides && !overridePropertiesFile.delete() ) {
            logger.warning( "Unable to delete override properties file '"+overridePropertiesFile.getAbsolutePath()+"'." );
        }
    }

    private static boolean doOverrideProperties( final Properties origProps,
                                                 final File overridePropertiesFile ) {
        boolean updated = false;

        final Properties newProps = loadProperties(overridePropertiesFile);
        for ( final String newPropKey : newProps.stringPropertyNames()) {
            final String newPropvalue = newProps.getProperty(newPropKey);
            final Object oldValue = origProps.setProperty(newPropKey, newPropvalue);
            if (oldValue != null) {
                logger.info(MessageFormat.format("Overriding {0} (previous value = {1}) with new value ({2})", newPropKey, oldValue, newPropvalue));
            }
            else {
                logger.info(MessageFormat.format("Setting {0} to value {1}", newPropKey, newPropvalue));
            }
            updated = true;
        }

        return updated;
    }

    private static boolean doPropertiesUpgrade(  final Properties hostProperties ) {
        boolean updated = false;

        if ( !hostProperties.stringPropertyNames().contains(ConfigService.HOSTPROPERTIES_SECRET)) {
            hostProperties.setProperty(ConfigService.HOSTPROPERTIES_SECRET, UUID.randomUUID().toString());
            updated = true;
        }

        return updated;
    }

    private static String generatePassword() {
        final byte[] passBytes = new byte[32];
        rand.nextBytes(passBytes);
        return HexUtils.encodeBase64(passBytes, true);
    }

    private static File checkFile(File file, boolean mustExist) {
        if (mustExist == file.exists()) return file;

        throw new DieDieDie(file.getAbsolutePath() + (mustExist ? " does not exist" : " already exists"), 2);
    }

    private static MasterPasswordManager getMasterPasswordManager(File etcDir) {
        return new MasterPasswordManager(new DefaultMasterPasswordFinder(checkFile(new File(new File(etcDir,"conf"), "omp.dat"), true)));
    }

    private static String createKeystore(final File ksFile, final String pass) {
        FileOutputStream kfos = null;
        try {
            logger.info("Generating keypair...");
            KeyPair keyPair = JceProvider.getInstance().generateRsaKeyPair();
            CertGenParams cgp = new CertGenParams();
            cgp.setSubjectDn(new X500Principal("cn=localhost"));
            cgp.setDaysUntilExpiry(3652);
            X509Certificate cert = BouncyCastleCertUtils.generateSelfSignedCertificate(cgp, keyPair);
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setKeyEntry(DEFAULT_ALIAS, keyPair.getPrivate(), pass.toCharArray(), new Certificate[] {cert});
            kfos = new FileOutputStream(ksFile);
            ks.store(kfos, pass.toCharArray());
            logger.info("Created " + ksFile.getAbsolutePath());
        } catch (Exception e) {
            throw new DieDieDie("Unable to create keystore", 4, e);
        } finally {
            ResourceUtils.closeQuietly(kfos);
        }
        return pass;
    }

    private static void createPropertiesFile(final File hostPropertiesFile, final File keystoreFile, final String obfKeystorePass) {
        final Properties newProps = new Properties();
        newProps.setProperty(ConfigService.HOSTPROPERTIES_ID, UUID.randomUUID().toString());
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SECRET, UUID.randomUUID().toString());        
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREFILE, keystoreFile.getAbsolutePath());
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREPASSWORD, obfKeystorePass);
        newProps.setProperty( ConfigService.HOSTPROPERTIES_JRE, SyspropUtil.getProperty( "java.home" ) );
        newProps.setProperty(ConfigService.HOSTPROPERTIES_TYPE, PCUtils.isAppliance() ? HostConfig.HostType.APPLIANCE.name() : HostConfig.HostType.SOFTWARE.name());

        saveProperties( newProps, hostPropertiesFile );
        logger.info("Created " + hostPropertiesFile.getAbsolutePath());
    }

    private static Properties loadProperties( final File propertiesFile ) {
        final Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream( propertiesFile );
            properties.load(fileInputStream);
        } catch ( IOException e ) {
            throw new DieDieDie("Unable to load " +propertiesFile.getName()+ " file.", 5, e);
        } finally {
            ResourceUtils.closeQuietly(fileInputStream);
        }
        return properties;
    }

    private static void saveProperties( final Properties properties, final File propertiesFile ) {
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(new FileOutputStream( propertiesFile ));
            properties.store( outputStreamWriter, null );
        } catch ( IOException e ) {
            throw new DieDieDie("Unable to save " + propertiesFile.getName() + " file.", 5, e);
        } finally {
            ResourceUtils.closeQuietly(outputStreamWriter);
        }
    }
}
