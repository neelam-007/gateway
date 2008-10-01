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
import java.security.GeneralSecurityException;
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

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        try {
            realMain();
            System.exit(0);
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

    private static void realMain() {
        final String pcDirName = System.getProperty("com.l7tech.server.processcontroller.homeDirectory");
        if (pcDirName == null) throw new DieDieDie("com.l7tech.server.processcontroller.homeDirectory is required", 1);

        final File pcDir = new File(pcDirName);
        if (!pcDir.exists()) throw new DieDieDie(pcDir.getAbsolutePath() + " does not exist", 2);

        final File etcDir = new File(pcDir,"etc");
        final File hostPropertiesFile = new File(etcDir, "host.properties");

        final File etcConfDir = new File(etcDir,"conf");
        if (hostPropertiesFile.exists()) {
            logger.info("Found existing " + hostPropertiesFile.getAbsolutePath());
            // TODO do we care what's in there?
            System.exit(0);
            return;
        }

        final File masterPasswordFile = new File(etcConfDir, "omp.dat");
        if (!masterPasswordFile.exists()) throw new DieDieDie(masterPasswordFile.getAbsolutePath() + " does not exist!", 3);

        MasterPasswordManager masterPasswordManager = new MasterPasswordManager( new DefaultMasterPasswordFinder(masterPasswordFile) );

        logger.info("Generating keypair...");
        FileOutputStream kfos = null;
        final File ksFile = new File(etcDir, "localhost.p12");
        if (ksFile.exists()) throw new DieDieDie("Keystore file " + ksFile.getAbsolutePath() + " already exists", 6);
        byte[] passBytes = new byte[4];
        new SecureRandom().nextBytes(passBytes);
        String pass = HexUtils.hexDump(passBytes);
        try {
            KeyPair keyPair = JceProvider.generateRsaKeyPair(1024);
            X509Certificate cert = BouncyCastleCertUtils.generateSelfSignedCertificate(new X500Principal("cn=localhost"), 3652, keyPair, false);
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setKeyEntry(DEFAULT_ALIAS, keyPair.getPrivate(), pass.toCharArray(), new Certificate[] {cert});
            kfos = new FileOutputStream(ksFile);
            ks.store(kfos, pass.toCharArray());
            logger.info("Created " + ksFile.getAbsolutePath());
        } catch (Exception e) {
            throw new DieDieDie("Unable to create default keystore", 4, e);
        } finally {
            ResourceUtils.closeQuietly(kfos);
        }

        Properties newProps = new Properties();
        newProps.setProperty(ConfigService.HOSTPROPERTIES_ID, UUID.randomUUID().toString());
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREFILE, ksFile.getAbsolutePath());
        newProps.setProperty(ConfigService.HOSTPROPERTIES_SSL_KEYSTOREPASSWORD, masterPasswordManager.encryptPassword(pass.toCharArray()));
        newProps.setProperty(ConfigService.HOSTPROPERTIES_JRE, System.getProperty("java.home"));

        FileOutputStream pfos = null;
        try {
            pfos = new FileOutputStream(hostPropertiesFile);
            newProps.store(new OutputStreamWriter(pfos), null);
            logger.info("Created " + hostPropertiesFile.getAbsolutePath());
        } catch (IOException e) {
            throw new DieDieDie("Unable to write create default properties file", 5, e);
        } finally {
            ResourceUtils.closeQuietly(pfos);
        }
    }

}
