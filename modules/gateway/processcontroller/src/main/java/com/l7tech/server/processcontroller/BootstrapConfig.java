/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.gateway.common.security.BouncyCastleCertUtils;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class BootstrapConfig {
    private static final Logger logger = Logger.getLogger(BootstrapConfig.class.getName());
    private static final String DEFAULT_ALIAS = "processController";

    public static void main(String[] args) {
        try {
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

    private static int realMain() {
        final String pcHomeDirName = System.getProperty("com.l7tech.server.processcontroller.homeDirectory");
        if (pcHomeDirName == null) throw new DieDieDie("com.l7tech.server.processcontroller.homeDirectory is required", 1);

        final File pcDir = checkFile(new File(pcHomeDirName), true);
        final File etcDir = checkFile(new File(pcDir,"etc"), true);

        final File hostPropertiesFile = new File(etcDir, "host.properties");
        if (hostPropertiesFile.exists()) {
            logger.fine("Found existing " + hostPropertiesFile.getAbsolutePath());
            // TODO do we care what's in there?
            return 0;
        }

        final File ksFile = checkFile(new File(etcDir, "localhost.p12"), false);
        final String ksPass = generatePassword();
        createKeystore(ksFile, ksPass);
        createPropertiesFile(hostPropertiesFile, ksFile, getMasterPasswordManager(etcDir).encryptPassword(ksPass.toCharArray()));

        return 0;
    }

    private static String generatePassword() {
        final byte[] passBytes = new byte[4];
        new SecureRandom().nextBytes(passBytes);
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
            KeyPair keyPair = JceProvider.generateRsaKeyPair(1024);
            X509Certificate cert = BouncyCastleCertUtils.generateSelfSignedCertificate(new X500Principal("cn=localhost"), 3652, keyPair, false);
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
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREFILE, keystoreFile.getAbsolutePath());
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREPASSWORD, obfKeystorePass);
        newProps.setProperty(ConfigService.HOSTPROPERTIES_JRE, System.getProperty("java.home"));

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

}
