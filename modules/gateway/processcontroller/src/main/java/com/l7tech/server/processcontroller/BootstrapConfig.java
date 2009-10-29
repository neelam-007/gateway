/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;
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
import java.util.Map;
import java.util.HashMap;
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
        if ( SyspropUtil.getBoolean("com.l7tech.server.log.console") ) {
            Logger.getLogger("").addHandler( new ConsoleHandler() );
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

        checkInitPatcherProperties(etcDir, hostPropertiesFile);

        doOverideProperties(hostPropertiesFile, overrideProperties);
        logger.info( "Configuration bootstrap complete." );
        return 0;
    }

    private static void checkInitPatcherProperties(File etcDir, File hostPropertiesFile) {
        Properties hostProperties = new Properties();
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(hostPropertiesFile);
            hostProperties.load(fin);
        } catch (Exception e) {
            throw new DieDieDie("Error reading host properties", 5, e);
        } finally {
            ResourceUtils.closeQuietly(fin);
        }

        if (!hostProperties.containsKey(ConfigService.HOSTPROPERTIES_PATCH_TRUSTSTORE_FILE)) {
            final File patchKeystoreFile = checkFile(new File(etcDir, ConfigService.DEFAULT_PATCH_TRUSTSTORE_FILENAME), false);
            final File patchCertFile = checkFile(new File(etcDir, ConfigService.DEFAULT_PATCHES_CERT_FILENAME), true);
            hostProperties.putAll(createPatchesKeystore(etcDir, patchKeystoreFile, generatePassword(), patchCertFile));
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(hostPropertiesFile);
                hostProperties.store(fos, null);
            } catch (Exception e) {
                throw new DieDieDie("Error storing patcher properties", 5, e);
            } finally {
                ResourceUtils.closeQuietly(fos);
            }
        }
    }

    private static void doOverideProperties(File hostPropertiesFile, File overridePropertiesFile) {
        if (!overridePropertiesFile.exists()) {
            logger.fine("no override.properties found, no need to merge with host.properties");
            return;
        }

        //open the original
        FileInputStream origFis = null;
        Properties origProps;

        try {
            origFis = new FileInputStream(hostPropertiesFile);
            origProps = new Properties();
            origProps.load(origFis);
        } catch (IOException e) {
            throw new DieDieDie("Unable to open the default host.properties file", 5, e);
        } finally {
            ResourceUtils.closeQuietly(origFis);
        }

        FileInputStream overrideFis = null;
        Properties newProps;
        try {
            overrideFis = new FileInputStream(overridePropertiesFile);
            newProps = new Properties();
            newProps.load(overrideFis);
        } catch (IOException e) {
            throw new DieDieDie("Unable to open the default host.properties file", 5, e);
        } finally {
            ResourceUtils.closeQuietly(overrideFis);
        }

        boolean wereChanges = false;
        for (Object newPropKey : newProps.keySet()) {
            String newPropvalue = newProps.getProperty((String) newPropKey);
            String oldValue = (String) origProps.setProperty((String) newPropKey, newPropvalue);
            if (oldValue != null) {
                logger.info(MessageFormat.format("Overriding {0} (previous value = {1}) with new value ({2})", newPropKey, oldValue, newPropvalue));
            }
            else {
                logger.info(MessageFormat.format("Setting {0} to value {1}", newPropKey, newPropvalue));
            }
            wereChanges = true;
        }

        if (wereChanges) {
            FileOutputStream propsFos = null;
            try {
                propsFos = new FileOutputStream(hostPropertiesFile);
                origProps.store(new OutputStreamWriter(propsFos), null);
                logger.info("Created " + hostPropertiesFile.getAbsolutePath());
            } catch (IOException e) {
                throw new DieDieDie("Unable to create host.properties file with overrides", 5, e);
            } finally {
                ResourceUtils.closeQuietly(propsFos);
            }
        }

        if ( !overridePropertiesFile.delete() ) {
            logger.warning( "Unable to delete override properties file '"+overridePropertiesFile.getAbsolutePath()+"'." );
        }
    }

    private static String generatePassword() {
        final byte[] passBytes = new byte[4];
        rand.nextBytes(passBytes);
        return HexUtils.hexDump(passBytes);
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

    /** Create trust keystore for validating patches and import the supplied certificate */
    private static Map<String,String> createPatchesKeystore(final File etcDir, final File patchKeystoreFile, final String ksPass, File patchCertFile) {
        logger.info("Initializing trust keystore for patches...");
        FileOutputStream kfos = null;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            ks.setCertificateEntry(ConfigService.DEFAULT_PATCHES_CERT_ALIAS, CertUtils.decodeCert(IOUtils.slurpFile(patchCertFile)));
            kfos = new FileOutputStream(patchKeystoreFile);
            ks.store(kfos, ksPass.toCharArray());
            return new HashMap<String, String>() {{
                put(ConfigService.HOSTPROPERTIES_PATCH_TRUSTSTORE_TYPE, "JKS");
                put(ConfigService.HOSTPROPERTIES_PATCH_TRUSTSTORE_FILE, patchKeystoreFile.getAbsolutePath());
                put(ConfigService.HOSTPROPERTIES_PATCH_TRUSTSTORE_PASSWORD, getMasterPasswordManager(etcDir).encryptPassword(ksPass.toCharArray()));
            }};
        } catch (Exception e) {
            throw new DieDieDie("Unable to create trust keystore for patches.", 4, e);
        } finally {
            ResourceUtils.closeQuietly(kfos);
        }
    }

    private static void createPropertiesFile(final File hostPropertiesFile, final File keystoreFile, final String obfKeystorePass) {
        final Properties newProps = new Properties();
        newProps.setProperty(ConfigService.HOSTPROPERTIES_ID, UUID.randomUUID().toString());
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREFILE, keystoreFile.getAbsolutePath());
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREPASSWORD, obfKeystorePass);
        newProps.setProperty(ConfigService.HOSTPROPERTIES_JRE, System.getProperty("java.home"));
        newProps.setProperty(ConfigService.HOSTPROPERTIES_TYPE, isAppliance() ? HostConfig.HostType.APPLIANCE.name() : HostConfig.HostType.SOFTWARE.name());

        FileOutputStream pfos = null;
        try {
            pfos = new FileOutputStream(hostPropertiesFile);
            newProps.store(new OutputStreamWriter(pfos), null);
            logger.info("Created " + hostPropertiesFile.getAbsolutePath());
        } catch (IOException e) {
            throw new DieDieDie("Unable to create default host.properties file", 5, e);
        } finally {
            ResourceUtils.closeQuietly(pfos);
        }
    }

    private static boolean isAppliance() {
        // rpm -q ssg-appliance
        try {
            File rpm = new File("/bin/rpm");
            if (! rpm.exists())
                rpm = new File("/usr/bin/rpm");
            ProcResult result = ProcUtils.exec(rpm, new String[] {"-q", "ssg-appliance"});
            logger.info("Appliance check (rpm -q ssg-appliance) returned: " + result.getExitStatus());
            return result.getExitStatus() == 0;
        } catch (Exception e) {
            logger.log(Level.INFO, "Error encountered while trying to determine if the host is an appliance: " + ExceptionUtils.getMessage(e));
            return false;
        }
    }
}
