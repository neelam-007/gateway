package com.l7tech.server.config.commands;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.common.security.prov.luna.LunaCmu;
import com.l7tech.common.util.*;
import com.l7tech.server.config.*;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.server.security.keystore.sca.ScaException;
import com.l7tech.server.security.keystore.sca.ScaManager;
import com.l7tech.server.util.MakeLunaCerts;
import com.l7tech.server.util.SetKeys;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 */
public class KeystoreConfigCommand extends BaseConfigurationCommand {

    private static final Logger logger = Logger.getLogger(KeystoreConfigCommand.class.getName());

    private static final String BACKUP_FILE_NAME = "keystore_config_backups";
    private  static final String MASTER_KEY_BACKUP_FILE_NAME="ssg_mkey.bak";


    private static final String PROPERTY_COMMENT = "This file was updated by the SSG configuration utility";

    private static final String XML_KSFILE = "keystoreFile";
    private static final String XML_KSPASS = "keystorePass";
    private static final String XML_KSTYPE = "keystoreType";
    private static final String XML_KSALIAS  = "keyAlias";

    private static final String LUNA_SYSTEM_CMU_PROPERTY = "lunaCmuPath";

    private static final String PROPKEY_SECURITY_PROVIDER = "security.provider";
    private static final String PROPKEY_JCEPROVIDER = "com.l7tech.common.security.jceProviderEngine";

    private static final String[] LUNA_SECURITY_PROVIDERS =
            {
                "com.chrysalisits.crypto.LunaJCAProvider",
                "com.chrysalisits.cryptox.LunaJCEProvider",
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider"
            };

    private static final String[] DEFAULT_SECURITY_PROVIDERS =
            {
                "sun.security.provider.Sun",
                "sun.security.rsa.SunRsaSign",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE",
                "sun.security.jgss.SunProvider",
                "com.sun.security.sasl.Provider"
            };

    private static final String PKCS11_CFG_FILE = "/ssg/etc/conf/pkcs11.cfg";
    private static final String[] HSM_SECURITY_PROVIDERS =
            {
                "sun.security.pkcs11.SunPKCS11 " + PKCS11_CFG_FILE,
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE"
            };
    private KeystoreConfigBean ksBean;
    private SharedWizardInfo sharedWizardInfo;
//    private static final String HSM_SETUP_SCRIPT = "bin/hsm_setup.sh";

    private static final String SUDO_COMMAND = "sudo";
    private static final String SCADIAG_COMMAND = "scadiag";


    public KeystoreConfigCommand(ConfigurationBean bean) {
        super(bean);
        ksBean = (KeystoreConfigBean) configBean;
        sharedWizardInfo = SharedWizardInfo.getInstance();
    }

    public boolean execute() {
        boolean success = true;
        if (ksBean.isDoKeystoreConfig()) {
            KeystoreType ksType = ksBean.getKeyStoreType();
            try {
                switch(ksType) {
                    case DEFAULT_KEYSTORE_NAME:
                        doDefaultKeyConfig(ksBean);
                        break;
                    case LUNA_KEYSTORE_NAME:
                        doLunaKeyConfig(ksBean);
                        break;
                    case SCA6000_KEYSTORE_NAME:
                        doHSMConfig(ksBean);
                        break;
                }
                updateSharedKey(ksBean);
            } catch (Exception e) {
                success = false;
            }
        } else {
            if (PartitionManager.getInstance().getActivePartition().isNewPartition()) {
                String partitionName = PartitionManager.getInstance().getActivePartition().getPartitionId();
                logger.warning("The \"" + partitionName + "\" partition has been created but no keystore has been specified.");
                logger.warning("The \"" + partitionName + "\" partition will not be able to start without a keystore");
            }
        }
        return success;
    }

    private void updateSharedKey(KeystoreConfigBean ksBean) throws Exception {
        byte[] sharedKeyData = ksBean.getSharedKeyData();
        logger.info("Updating the shared key if necessary");
        if (sharedKeyData == null || sharedKeyData.length == 0) {
            logger.info("No shared key found. No need to update it.");
        } else {
            //get the new keystore
            KeystoreActions ka = new KeystoreActions(getOsFunctions());
            KeyStore ks = ka.loadExistingKeyStore(
//                    ksBean.getKsPassword(),
//                    ksBean.getKeyStoreType().shortTypeName(),
                    new File(getOsFunctions().getKeystoreDir()+KeyStoreConstants.SSL_KEYSTORE_FILE),
                    false,
                    null);

            Certificate[] chain = ks.getCertificateChain(KeyStoreConstants.SSL_ALIAS);
            Key newKey = chain[0].getPublicKey();

            DBInformation dbInfo = sharedWizardInfo.getDbinfo();
            try {
                DBActions dba = new DBActions();
                Connection conn = null;
                Statement stmt = null;
                try {
                    conn = dba.getConnection(dbInfo);
                    stmt = conn.createStatement();
                    String pubKeyId = EncryptionUtil.computeCustomRSAPubKeyID((RSAPublicKey) newKey);
                    String encryptedSharedData = EncryptionUtil.rsaEncAndB64(sharedKeyData, newKey);
                    stmt.executeQuery(MessageFormat.format("insert into shared_keys (encodingid, b64edval) values {0},{1}",pubKeyId, encryptedSharedData));
                } catch (SQLException e) {
                    logger.warning(MessageFormat.format("Error while updating the shared key in the database. {0}", e.getMessage()));
                    throw e;
                } catch (BadPaddingException e) {
                    logger.warning(MessageFormat.format("Error while updating the shared key in the database. {0}", e.getMessage()));
                    throw e;
                } catch (NoSuchAlgorithmException e) {
                    logger.warning(MessageFormat.format("Error while updating the shared key in the database. {0}", e.getMessage()));
                    throw e;
                } catch (IllegalBlockSizeException e) {
                    logger.warning(MessageFormat.format("Error while updating the shared key in the database. {0}", e.getMessage()));
                    throw e;
                } catch (InvalidKeyException e) {
                    logger.warning(MessageFormat.format("Error while updating the shared key in the database. {0}", e.getMessage()));
                    throw e;
                } catch (NoSuchPaddingException e) {
                    logger.warning(MessageFormat.format("Error while updating the shared key in the database. {0}", e.getMessage()));
                    throw e;
                } finally {
                    ResourceUtils.closeQuietly(stmt);
                    ResourceUtils.closeQuietly(conn);
                }
            } catch (ClassNotFoundException e) {
                logger.warning(MessageFormat.format("Could not initialize the connection to the database for updating the shared key. {0}",e.getMessage()));
                throw e;
            }
        }
    }

    private void doDefaultKeyConfig(KeystoreConfigBean ksBean) throws Exception {
        char[] ksPassword = ksBean.getKsPassword();
        boolean doBothKeys = ksBean.isDoBothKeys();

        String ksDir = getOsFunctions().getKeystoreDir();
        File keystoreDir = new File(ksDir);
        if (!keystoreDir.exists() && !keystoreDir.mkdir()) {
            String msg = "Could not create the directory: \"" + ksDir + "\". Cannot generate keystores";
            logger.severe(msg);
            throw new IOException(msg);
        }

        File keystorePropertiesFile = new File(getOsFunctions().getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        File caKeyStoreFile = new File( ksDir + KeyStoreConstants.CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + KeyStoreConstants.SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + KeyStoreConstants.CA_CERT_FILE);
        File sslCertFile = new File(ksDir + KeyStoreConstants.SSL_CERT_FILE);
        File javaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile());
        File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());

        File newJavaSecFile  = new File(getOsFunctions().getPathToJavaSecurityFile() + ".new");

        File[] files = new File[]
        {
            javaSecFile,
            keystorePropertiesFile,
            tomcatServerConfigFile,
            caKeyStoreFile,
            sslKeyStoreFile,
            caCertFile,
            sslCertFile,
            systemPropertiesFile
        };

        backupFiles(files, BACKUP_FILE_NAME);

        try {
            prepareJvmForNewKeystoreType(KeystoreType.DEFAULT_KEYSTORE_NAME);
            makeDefaultKeys(doBothKeys, ksBean, ksDir, ksPassword);
            updateJavaSecurity(javaSecFile, newJavaSecFile,DEFAULT_SECURITY_PROVIDERS );
            updateKeystoreProperties(keystorePropertiesFile, ksPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
            updateSystemPropertiesFile(ksBean, systemPropertiesFile);
        } catch (Exception e) {
            String mess = "problem generating keys or keystore - skipping keystore configuration: ";
            logger.log(Level.SEVERE, mess + e.getMessage(), e);
            throw e;
        }
    }

    private void doLunaKeyConfig(KeystoreConfigBean ksBean) throws Exception {
        char[] ksPassword = ksBean.getKsPassword();
        //we don't actually care what the password is for Luna, so make it obvious.
        ksPassword = "ignoredbyluna".toCharArray();

        String ksDir = getOsFunctions().getKeystoreDir();

        File newJavaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile() + ".new");

        File javaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile());
        File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());
        File keystorePropertiesFile = new File(getOsFunctions().getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        File caKeyStoreFile = new File( ksDir + KeyStoreConstants.CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + KeyStoreConstants.SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + KeyStoreConstants.CA_CERT_FILE);
        File sslCertFile = new File(ksDir + KeyStoreConstants.SSL_CERT_FILE);

        File[] files = new File[]
        {
            javaSecFile,
            systemPropertiesFile,
            keystorePropertiesFile,
            tomcatServerConfigFile,
            caKeyStoreFile,
            sslKeyStoreFile,
            caCertFile,
            sslCertFile
        };

        backupFiles(files, BACKUP_FILE_NAME);


        try {
            //prepare the JDK and the Luna environment before generating keys
            setLunaSystemProps(ksBean);
            copyLunaJars(ksBean);
            prepareJvmForNewKeystoreType(KeystoreType.LUNA_KEYSTORE_NAME);
            makeLunaKeys(ksBean, caCertFile, sslCertFile, caKeyStoreFile, sslKeyStoreFile);
            updateJavaSecurity(javaSecFile, newJavaSecFile, LUNA_SECURITY_PROVIDERS);
            updateKeystoreProperties(keystorePropertiesFile, ksPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
            updateSystemPropertiesFile(ksBean, systemPropertiesFile);
        } catch (Exception e) {
            String mess = "problem generating keys or keystore - skipping keystore configuration: ";
            logger.log(Level.SEVERE, mess + e.getMessage(), e);
            throw e;
        }                                                                                                                           
    }
    
    private void doHSMConfig(KeystoreConfigBean ksBean) throws Exception {
        final char[] shortPasswd = ksBean.getKsPassword();
        final char[] fullKsPassword = ("gateway:" + new String(shortPasswd)).toCharArray();
        final String ksDir = getOsFunctions().getKeystoreDir();
        final File keystoreDir = new File(ksDir);
        if (!keystoreDir.exists() && !keystoreDir.mkdir()) {
            String msg = "Could not create the directory: \"" + ksDir + "\". Cannot generate keystores";
            logger.severe(msg);
            throw new IOException(msg);
        }

        final File keystorePropertiesFile = new File(getOsFunctions().getKeyStorePropertiesFile());
        final File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        final File caKeyStoreFile = new File( ksDir + KeyStoreConstants.CA_KEYSTORE_FILE);
        final File sslKeyStoreFile = new File(ksDir + KeyStoreConstants.SSL_KEYSTORE_FILE);
        final File caCertFile = new File(ksDir + KeyStoreConstants.CA_CERT_FILE);
        final File sslCertFile = new File(ksDir + KeyStoreConstants.SSL_CERT_FILE);
        final File javaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile());
        final File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());

        final File newJavaSecFile  = new File(getOsFunctions().getPathToJavaSecurityFile() + ".new");

        File[] files = new File[]
        {
            javaSecFile,
            keystorePropertiesFile,
            tomcatServerConfigFile,
            caKeyStoreFile,
            sslKeyStoreFile,
            caCertFile,
            sslCertFile,
            systemPropertiesFile
        };

        backupFiles(files, BACKUP_FILE_NAME);
        if (ksBean.isInitializeHSM()) {
            doInitializeHsm(fullKsPassword, ksBean, ksDir, javaSecFile, newJavaSecFile, keystorePropertiesFile, tomcatServerConfigFile, sslKeyStoreFile, systemPropertiesFile);
        } else {
            doRestoreHsm(fullKsPassword, ksBean, ksDir, javaSecFile, newJavaSecFile, keystorePropertiesFile, tomcatServerConfigFile, sslKeyStoreFile, systemPropertiesFile);
        }
    }

    private void doInitializeHsm(char[] ksPassword, KeystoreConfigBean ksBean, String ksDir, File javaSecFile, File newJavaSecFile, File keystorePropertiesFile, File tomcatServerConfigFile, File sslKeyStoreFile, File systemPropertiesFile) throws Exception {
        logger.info("Initializing HSM");
        try {
            //HSM Specific setup
            MyScaManager scaManager = getScaManager();

            //TODO start kiod if it's not started
            scaManager.startSca();
            //zero the board
            zeroHsm();
            scaManager.wipeKeydata();
            initializeHSM(ksPassword);
            prepareJvmForNewKeystoreType(KeystoreType.SCA6000_KEYSTORE_NAME);
            makeHSMKeys(new File(ksDir), ksPassword);

            insertKeystoreIntoDatabase(scaManager);
            backupHsmMasterkey(ksBean);
            updateJavaSecurity(javaSecFile, newJavaSecFile, HSM_SECURITY_PROVIDERS);

            //General Keystore Setup
            updateKeystoreProperties(keystorePropertiesFile, ksPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
            updateSystemPropertiesFile(ksBean, systemPropertiesFile);

        } catch (ScaException e) {
            logger.severe("Error while initializing the SCA Manager: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            String mess = "Problem initializing the HSM - skipping HSM configuration: ";
            logger.log(Level.SEVERE, mess + e.getMessage(), e);
            throw e;
        }
    }

    private void doRestoreHsm(char[] fullKsPassword, KeystoreConfigBean ksBean, String ksDir, File javaSecFile, File newJavaSecFile, File keystorePropertiesFile, File tomcatServerConfigFile, File sslKeyStoreFile, File systemPropertiesFile) throws Exception {
        logger.info("Restoring HSM Backup");
        try {
            MyScaManager scaManager = getScaManager();

            //fetch from the db
            DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();
            final byte[] databytes = getKeydataFromDatabase(dbinfo);

            //zero the board
            zeroHsm();

            //delete keydata dir
            scaManager.wipeKeydata();

            //restore the master key
            restoreHsmMasterkey(fullKsPassword, ksBean.getMasterKeyBackupPassword());

            //replace keydata dir
            scaManager.saveKeydata(databytes);
            
            prepareJvmForNewKeystoreType(KeystoreType.SCA6000_KEYSTORE_NAME);

            updateJavaSecurity(javaSecFile, newJavaSecFile, HSM_SECURITY_PROVIDERS);

            //General Keystore Setup
            updateKeystoreProperties(keystorePropertiesFile, fullKsPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), fullKsPassword);
            updateSystemPropertiesFile(ksBean, systemPropertiesFile);
        } catch (ScaException e) {
            logger.severe("Error while initializing the SCA Manager: " + e.getMessage());
            throw e;
        } catch (KeystoreActions.KeystoreActionsException e) {
            logger.severe("Error while restoring a keystore to the HSM. Could not get the keydata from the database: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            String mess = "Problem restoring the keystore to the HSM - skipping HSM configuration: ";
            logger.log(Level.SEVERE, mess + e.getMessage(), e);
            throw e;
        }
    }

    private MyScaManager getScaManager() throws ScaException {
        return new MyScaManager();
    }

    private void backupHsmMasterkey(KeystoreConfigBean ksBean) throws IOException, KeystoreActions.KeystoreActionsException {
        if (getOsFunctions().isUnix()) {
            if (ksBean.isShouldBackupMasterKey()) {
                final char[] keystorePassword = ksBean.getKsPassword();
                final char[] masterKeyBackupPassword = ksBean.getMasterKeyBackupPassword();
                String masterKeyBackupScript = getOsFunctions().getSsgInstallRoot() + KeystoreConfigBean.MASTERKEY_MANAGE_SCRIPT;
                try {
                    logger.info("Executing \"" + masterKeyBackupScript + "\"");
                    ProcResult result = ProcUtils.exec(null, new File(masterKeyBackupScript), ProcUtils.args("backup", String.valueOf(keystorePassword), MASTER_KEY_BACKUP_FILE_NAME, String.valueOf(masterKeyBackupPassword)), null, true);
                    if (result.getExitStatus() != 0) {
                        logger.warning(MessageFormat.format("{0} exited with a non zero return code: {1} ({2})",
                                masterKeyBackupScript,
                                result.getExitStatus(),
                                new String(result.getOutput()))
                        );
                    } else {
                        logger.info("HSM master key backup successful");
                    }
                } catch (IOException e) {
                    logger.warning("There was an error while trying to backup the HSM master key: " + e.getMessage());
                    throw e;
                }
            }
        }
    }

    private void initializeHSM(char[] keystorePassword) throws IOException {
        if (getOsFunctions().isUnix()) {
            final String initializeScript = getOsFunctions().getSsgInstallRoot() + KeystoreConfigBean.MASTERKEY_MANAGE_SCRIPT;
            try {
                logger.info("Executing \"" + initializeScript + "\"");
                ProcResult result = ProcUtils.exec(null, new File(initializeScript), ProcUtils.args("init", String.valueOf(keystorePassword)), null, true);
                if (result.getExitStatus() != 0) {
                    logger.warning(MessageFormat.format("{0} exited with a non zero return code: {1} ({2})",
                            initializeScript,
                            result.getExitStatus(),
                            new String(result.getOutput()))
                    );
                } else {
                    logger.info(MessageFormat.format("Successfully initialized the HSM: {0}", new String(result.getOutput())));
                }
            } catch (IOException e) {
                logger.warning("There was an error trying to initialize the HSM: " + e.getMessage());
                throw e;
            }
        }
    }

    private void restoreHsmMasterkey(final char[] keystorePassword, final char[] backupPassword) throws IOException {
        if (getOsFunctions().isUnix()) {
            final String restoreScript = getOsFunctions().getSsgInstallRoot() + MASTER_KEY_BACKUP_FILE_NAME;
            try {
                logger.info("Executing \"" + restoreScript + "\"");
                ProcResult result = ProcUtils.exec(null, new File(restoreScript), ProcUtils.args("restore", String.valueOf(keystorePassword), MASTER_KEY_BACKUP_FILE_NAME, String.valueOf(backupPassword)), null, true);
                if (result.getExitStatus() != 0) {
                    logger.warning(MessageFormat.format("{0} exited with a non zero return code: {1} ({2})",
                            restoreScript,
                            result.getExitStatus(),
                            new String(result.getOutput()))
                    );
                } else {
                    logger.info("Successfully restore the HSM master key");
                }
            } catch (IOException e) {
                logger.warning("There was an error trying to restore the HSM master key: " + e.getMessage());
                throw e;
            }
        }
    }

    private void zeroHsm() throws IOException {
        if (getOsFunctions().isUnix()) {
            String sudoCommand = SUDO_COMMAND;
            try {
                logger.info("Executing \"" + SCADIAG_COMMAND + "\"");
                ProcUtils.exec(null, new File(sudoCommand), ProcUtils.args(SCADIAG_COMMAND, "-z", "mca0"), null, false);
                logger.info("Successfully zeroed the HSM");
            } catch (IOException e) {
                logger.warning("There was an error trying to zero the HSM: " + e.getMessage());
                throw e;
            }
        }
    }

    private void insertKeystoreIntoDatabase(ScaManager scaManager) throws ScaException, KeystoreActions.KeystoreActionsException {
        try {
            byte[] keyData = scaManager.loadKeydata();
            DBInformation dbinfo = sharedWizardInfo.getDbinfo();
            putKeydataInDatabase(dbinfo, keyData);
        } catch (ScaException e) {
            logger.severe("Could not load the keystore information from the disk: " + e.getMessage());
            throw e;
        } catch (KeystoreActions.KeystoreActionsException e) {
            logger.severe(e.getMessage());
            throw e;
        }
    }

    private void putKeydataInDatabase(DBInformation dbinfo, byte[] keyData) throws KeystoreActions.KeystoreActionsException {
        KeystoreActions ka = new KeystoreActions(getOsFunctions());
        ka.putKeydataInDatabase(dbinfo, keyData);
    }

    private byte[] getKeydataFromDatabase(DBInformation dbinfo) throws KeystoreActions.KeystoreActionsException {
        KeystoreActions ka = new KeystoreActions(getOsFunctions());
        return ka.getKeydataFromDatabase(dbinfo);

    }

    private void makeHSMKeys(File keystoreDir, char[] keystoreAccessPassword) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignatureException, InvalidKeyException {
        if ( !keystoreDir.exists() ) throw new IOException( "Keystore directory '" + keystoreDir.getAbsolutePath() + "' does not exist" );
        if ( !keystoreDir.isDirectory() ) throw new IOException( "Keystore directory '" + keystoreDir.getAbsolutePath() + "' is not a directory" );
        String kstype = "PKCS11";

        //TODO try to refactor SetKeys to understand HSM-speak instead of just PKCS12 and then there's no need to reinvent things here.
        X509Certificate caCert = null;
        X509Certificate sslCert = null;
        PrivateKey caPrivateKey;

        KeyStore theHsmKeystore = KeyStore.getInstance(kstype);
        logger.info("Connecting to " + kstype + " keystore using password: " + new String(keystoreAccessPassword));
        theHsmKeystore.load(null,keystoreAccessPassword);


        logger.info("Generating RSA keypair for CA cert");
        KeyPair cakp = JceProvider.generateRsaKeyPair();
        caPrivateKey = cakp.getPrivate();

        logger.info("Generating self-signed CA cert");
        caCert = BouncyCastleRsaSignerEngine.makeSelfSignedRootCertificate(
                KeyStoreConstants.CA_DN_PREFIX + sharedWizardInfo.getHostname(),
                KeyStoreConstants.CA_VALIDITY_DAYS, cakp);

        logger.info("Storing CA cert in HSM");
        theHsmKeystore.setKeyEntry(KeyStoreConstants.CA_ALIAS, caPrivateKey, keystoreAccessPassword, new X509Certificate[] { caCert } );

        logger.info("Generating RSA keypair for SSL cert" );
        KeyPair sslkp = JceProvider.generateRsaKeyPair();

        logger.info("Generating SSL cert");
        sslCert = BouncyCastleRsaSignerEngine.makeSignedCertificate(KeyStoreConstants.SSL_DN_PREFIX + sharedWizardInfo.getHostname(),
                                                                       KeyStoreConstants.SSL_VALIDITY_DAYS,
                                                                       sslkp.getPublic(), caCert, caPrivateKey, RsaSignerEngine.CertType.SSL );

        logger.info("Storing SSL cert in HSM");
        theHsmKeystore.setKeyEntry(KeyStoreConstants.SSL_ALIAS, sslkp.getPrivate(), keystoreAccessPassword, new X509Certificate[] { sslCert, caCert } );

        File caCertFile = new File(keystoreDir,KeyStoreConstants.CA_CERT_FILE);
        File sslCertFile = new File(keystoreDir,KeyStoreConstants.SSL_CERT_FILE);

        createDummyKeystorse(keystoreDir);
        exportCerts(caCert, caCertFile, sslCert, sslCertFile);
    }

    private void createDummyKeystorse(File keystoreDir) throws IOException {
        File dummmyCaKeystore = new File(keystoreDir, KeyStoreConstants.CA_KEYSTORE_FILE);
        File dummySslKeystore = new File(keystoreDir, KeyStoreConstants.SSL_KEYSTORE_FILE);
        
        truncateKeystores(dummmyCaKeystore, dummySslKeystore);
    }

    public void importExistingKeystoreToHsm(File existingCaKeystore, File existingSslKeystore,
                                            String existingCaPassword, String existingCaAlias,
                                            String existingSslPassword, String existingSslAlias,
                                            String hsmPassword) throws KeyStoreException {
        //load old keystore

        KeyStore oldCaKeystore = KeyStore.getInstance("PKCS12");
        FileInputStream caFis = null;
        boolean caKsOk = false;
        try {
            caFis = new FileInputStream(existingCaKeystore);
            logger.info("Loading PCKS12 keystore from " + existingCaKeystore.getAbsolutePath());
            oldCaKeystore.load(caFis, existingCaPassword.toCharArray());
            caKsOk = true;
        } catch (FileNotFoundException e) {
            logger.severe("Could not load the existing CA keystore: " + existingCaKeystore.getAbsolutePath());
        } catch (NoSuchAlgorithmException e) {
            logger.severe("Could not load the existing CA keystore: " + existingCaKeystore.getAbsolutePath());
        } catch (IOException e) {
            logger.severe("Could not load the existing CA keystore: " + existingCaKeystore.getAbsolutePath());
        } catch (CertificateException e) {
            logger.severe("Could not load the existing CA keystore: " + existingCaKeystore.getAbsolutePath());
        } finally{
            ResourceUtils.closeQuietly(caFis);
        }

        KeyStore oldSslKeystore = KeyStore.getInstance("PKCS12");
        FileInputStream sslFis = null;
        boolean sslKsOk = false;
        try {
            sslFis = new FileInputStream(existingSslKeystore);
            logger.info("Loading PCKS12 keystore from " + existingSslKeystore.getAbsolutePath());
            oldSslKeystore.load(sslFis, existingSslPassword.toCharArray());
            sslKsOk = true;
        } catch (FileNotFoundException e) {
            logger.severe("Could not load the existing SSL keystore: " + existingCaKeystore.getAbsolutePath());
        } catch (NoSuchAlgorithmException e) {
            logger.severe("Could not load the existing SSL keystore: " + existingCaKeystore.getAbsolutePath());
        } catch (IOException e) {
            logger.severe("Could not load the existing SSL keystore: " + existingCaKeystore.getAbsolutePath());
        } catch (CertificateException e) {
            logger.severe("Could not load the existing SSL keystore: " + existingCaKeystore.getAbsolutePath());
        } finally {
            ResourceUtils.closeQuietly(sslFis);
        }


        //now get contents of existing keystores and import into the HSM
        if (!caKsOk || !sslKsOk) {
            logger.severe("One of the existing keystores could not be loaded. Cannot proceed with the import into the HSM");
        } else {
            KeyStore newKeystore = KeyStore.getInstance("PKCS11");
            try {
                newKeystore.load(null, hsmPassword.toCharArray());
                try {
                    List<String> keyList = getCandidateKeys(oldCaKeystore, existingCaPassword);
                    if (keyList.isEmpty()) {
                        logger.warning("No candidate keys in the existing CA keystore [" + existingCaKeystore.getAbsolutePath() + "] for import into the HSM.");
                    } else {
                        copyKeys(keyList, oldCaKeystore, existingCaPassword, newKeystore);
                        keyList.clear();
                        keyList = getCandidateKeys(oldSslKeystore, existingSslPassword);
                        if (keyList.isEmpty()) {
                            logger.warning("No candidate keys in the existing CA keystore [" + existingCaKeystore.getAbsolutePath() + "] for import into the HSM.");
                        } else {
                            copyKeys(keyList, oldCaKeystore, existingCaPassword, newKeystore);
                        }
                    }
                } catch (UnrecoverableKeyException e) {
                    logger.warning("Error while retrieving contents of existing keystore [" + existingCaKeystore.getAbsolutePath() + "]: " + e.getMessage());
                }
            } catch (IOException e) {
                logger.severe("Could not connect to the HSM keystore: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                logger.severe("Could not connect to the HSM keystore: " + e.getMessage());
            } catch (CertificateException e) {
                logger.severe("Could not connect to the HSM keystore: " + e.getMessage());
            }
        }
    }

    private void copyKeys(List<String> keyList, KeyStore oldCaKeystore, String existingCaPassword, KeyStore hsmKeystore) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        for (String keyAlias : keyList) {
            PrivateKey pkey = (PrivateKey) oldCaKeystore.getKey(keyAlias, existingCaPassword.toCharArray());
            Certificate[] certChain = oldCaKeystore.getCertificateChain(keyAlias);
            hsmKeystore.setKeyEntry(keyAlias, pkey, existingCaPassword.toCharArray(), certChain);
        }
    }

    private List<String> getCandidateKeys(KeyStore keystore, String password) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        List<String> candidateKeys = new ArrayList<String>();
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keystore.isKeyEntry(alias)) {
                Object obj = keystore.getKey(alias, password.toCharArray());
                if (obj instanceof RSAPrivateKey) {
                    candidateKeys.add(alias);
                }
            }
        }
        return candidateKeys;
    }

    private void exportCerts(X509Certificate caCert, File caCertFile, X509Certificate sslCert, File sslCertFile) throws CertificateEncodingException {
        logger.info("Exporting DER-encoded CA certificate");
        byte[] caCertBytes = caCert.getEncoded();
        FileOutputStream caFos = null;
        boolean caCertOk = false;
        try {
            caFos = new FileOutputStream(caCertFile);
            caFos.write(caCertBytes);
            logger.info("Saved DER-encoded X.509 certificate to <" + caCertFile.getAbsolutePath() + ">" );
            caCertOk = true;
        } catch (FileNotFoundException e) {
            logger.severe("Could not export CA cert: " + e.getMessage());
        } catch (IOException e) {
            logger.severe("Could not export CA cert: " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(caFos);
        }

        if (caCertOk) {
            logger.info("Exporting DER-encoded SSL certificate");
            byte[] sslCertBytes = sslCert.getEncoded();
            FileOutputStream sslFos = null;
            try {
                sslFos = new FileOutputStream(sslCertFile);
                sslFos.write(sslCertBytes);
                logger.info("Saved DER-encoded X.509 certificate to <" + sslCertFile.getAbsolutePath() +">");
            } catch (FileNotFoundException e) {
                logger.severe("Could not export SSL cert: " + e.getMessage());
            } catch (IOException e) {
                logger.severe("Could not export SSL cert: " + e.getMessage());
            } finally {
                ResourceUtils.closeQuietly(sslFos);
            }
        }
    }

    private void prepareJvmForNewKeystoreType(KeystoreType ksType) throws IllegalAccessException, InstantiationException, FileNotFoundException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
        Provider[] currentProviders = Security.getProviders();
        for (Provider provider : currentProviders) {
            Security.removeProvider(provider.getName());
        }

       switch (ksType) {
            case DEFAULT_KEYSTORE_NAME:
                prepareProviders(DEFAULT_SECURITY_PROVIDERS);
                System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.BC_ENGINE);
                break;
            case SCA6000_KEYSTORE_NAME:
                prepareProviders(HSM_SECURITY_PROVIDERS);
                System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.PKCS11_ENGINE);
                break;
            case LUNA_KEYSTORE_NAME:
                prepareLunaProviders();
                System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.LUNA_ENGINE);
                break;
        }
    }

    private void prepareProviders(String[] securityProviders) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        for (String providerName : securityProviders) {
            try {
                Provider p = null;
                if (providerName.contains(" ")) {
                    String[] splitz = providerName.split(" ");
                    logger.info("Adding " + splitz[0]);
                    Class providerClass = Class.forName(splitz[0]);
                    Constructor ctor = providerClass.getConstructor(String.class);
                    p = (Provider) ctor.newInstance(splitz[1]);
                } else {
                    p = (Provider) Class.forName(providerName).newInstance();
                }
                Security.addProvider(p);
            } catch (ClassNotFoundException e) {
                logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                throw e;
            } catch (NoSuchMethodException e) {
                logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                throw e;
            } catch (InvocationTargetException e) {
                logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                throw e;
            }
        }
    }

    private void prepareLunaProviders() throws FileNotFoundException, IllegalAccessException, InstantiationException {
        File classDir = new File(getOsFunctions().getPathToJreLibExt());
        if (!classDir.exists()) {
            throw new FileNotFoundException("Could not locate the directory: \"" + classDir + "\"");
        }

        File[] lunaJars = classDir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return  s.toUpperCase().startsWith("LUNA") &&
                        s.toUpperCase().endsWith(".JAR");
            }
        });

        if (lunaJars == null) {
            throw new FileNotFoundException("Could not locate the Luna jar files in the specified directory: \"" + classDir + "\"");
        }

        URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;
        //this is a necessary hack to be able to hotplug some jars into the classloaders classpath.
        // On linux, this happens already, but  not on windows

        try {
            Class[] parameters = new Class[]{URL.class};
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            for (File lunaJar : lunaJars) {
                URL url = lunaJar.toURI().toURL();
                method.invoke(sysloader, new Object[]{url});
            }
            Class lunaJCAClass;
            String lunaJCAClassName = "com.chrysalisits.crypto.LunaJCAProvider";
            Class lunaJCEClass;
            String lunaJCEClassName = "com.chrysalisits.cryptox.LunaJCEProvider";

            try {
                lunaJCAClass = sysloader.loadClass(lunaJCAClassName);
                Object lunaJCA = lunaJCAClass.newInstance();
                Security.addProvider((Provider) lunaJCA);

                lunaJCEClass = sysloader.loadClass(lunaJCEClassName);
                Object lunaJCE = lunaJCEClass.newInstance();
                Security.addProvider((Provider) lunaJCE);

            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Security.addProvider(new sun.security.provider.Sun());
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }

    private boolean makeLunaKeys(KeystoreConfigBean ksBean, File caCertFile, File sslCertFile, File caKeyStoreFile, File sslKeyStoreFile) throws Exception {
        boolean success = false;
        String hostname = ksBean.getHostname();
        boolean exportOnly = false;
        try {
            exportOnly = (sharedWizardInfo.getClusterType() == ClusteringType.CLUSTER_JOIN);
            MakeLunaCerts.makeCerts(hostname, true, exportOnly, caCertFile, sslCertFile);
            success = true;
        } catch (LunaCmu.LunaCmuException e) {
            logger.severe("Could not locate the Luna CMU. Please rerun the wizard and check the Luna paths");
            logger.severe(e.getMessage());
            throw e;
        } catch (KeyStoreException e) {
            logger.severe("There has been a problem creating the Luna Keystore, or in creating/locating a key within it");
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("Could not write the certificates to disk");
            logger.severe(e.getMessage());
            throw e;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();    //should not happen
            throw e;
        } catch (CertificateException e) {
            e.printStackTrace(); //should not happen
            throw e;
        } catch (ClassNotFoundException e) {
            logger.severe("Luna classes not found in the classpath - cannot generate Luna certs");
            logger.severe(e.getMessage());
            throw e;
        } catch (MakeLunaCerts.CertsAlreadyExistException e) {
            logger.severe("Luna certificates already exist - new certs will not be written");
            throw e;
        } catch (LunaCmu.LunaTokenNotLoggedOnException e) {
            logger.severe("This SSG is not already logged into the luna partition, please log in and re-run");
            logger.severe(e.getMessage());
            throw e;
        }

        if (success) {
            truncateKeystores(caKeyStoreFile, sslKeyStoreFile);
        }
        return success;
    }

    private void updateSystemPropertiesFile(KeystoreConfigBean ksBean, File systemPropertiesFile) throws IOException {
        PropertiesConfiguration systemProps = new PropertiesConfiguration();
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(systemPropertiesFile);
            systemProps.load(is);
            is.close();
            is = null;

            switch (ksBean.getKeyStoreType()) {
                case LUNA_KEYSTORE_NAME:
                    systemProps.setProperty(PROPKEY_JCEPROVIDER, JceProvider.LUNA_ENGINE);
                    break;
                case SCA6000_KEYSTORE_NAME:
                    systemProps.setProperty(PROPKEY_JCEPROVIDER, JceProvider.PKCS11_ENGINE);
                    break;
                case DEFAULT_KEYSTORE_NAME:
                    systemProps.setProperty(PROPKEY_JCEPROVIDER, JceProvider.BC_ENGINE);
                    break;
            }

            logger.info("Updating the system.properties file");
            File newFile = new File(getOsFunctions().getSsgSystemPropertiesFile() + ".confignew");
            os = new FileOutputStream(newFile);
            systemProps.save(os, "iso-8859-1");
            renameFile(newFile, systemPropertiesFile);
        } catch (FileNotFoundException e) {
            logger.severe(MessageFormat.format("Error while updating the file: {0}. ({1})", systemPropertiesFile.getAbsolutePath(), e.getMessage()));
            throw e;
        } catch (IOException e) {
            logger.severe(MessageFormat.format("Error while updating the file: {0}. ({1})", systemPropertiesFile.getAbsolutePath(), e.getMessage()));
            throw e;
        } catch (ConfigurationException e) {
            logger.severe(MessageFormat.format("Error while updating the file: {0}. ({1})", systemPropertiesFile.getAbsolutePath(), e.getMessage()));
            throw new CausedIOException(e);            
        } finally {
            ResourceUtils.closeQuietly(is);
            ResourceUtils.closeQuietly(os);
        }
    }

    private void updateJavaSecurity(File javaSecFile, File newJavaSecFile, String[] providersList) throws IOException {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(javaSecFile));
            writer= new PrintWriter(newJavaSecFile);

            String line = null;
            int secProviderIndex = 0;
            while ((line = reader.readLine()) != null) { //start looking for the security providers section
                if (!line.startsWith("#") && line.startsWith(PROPKEY_SECURITY_PROVIDER)) { //ignore comments and match if we find a security providers section
                    while ((line = reader.readLine()) != null) { //read the rest of the security providers list, esssentially skipping over it
                        if (!line.startsWith("#") && line.startsWith(PROPKEY_SECURITY_PROVIDER)) {
                        }
                        else {
                            break; //we're done with the sec providers
                        }
                    }

                    //now write out the new ones
                    while (secProviderIndex < providersList.length) {
                        line = PROPKEY_SECURITY_PROVIDER + "." + String.valueOf(secProviderIndex + 1) + "=" + providersList[secProviderIndex];
                        writer.println(line);
                        secProviderIndex++;
                    }
                }
                else {
                    writer.println(line);
                }
            }
            logger.info("Updating the java.security file");
            writer.flush();
            writer.close();
            renameFile(newJavaSecFile, javaSecFile);

        } catch (FileNotFoundException e) {
            logger.severe("Error while updating the file: " + javaSecFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("Error while updating the file: " + javaSecFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } finally {
            ResourceUtils.closeQuietly(reader);
            ResourceUtils.closeQuietly(writer);
        }
    }

    private void renameFile(File srcFile, File destFile) throws IOException {
        String backupName = destFile.getAbsoluteFile() + ".bak";

        logger.info("Renaming: " + destFile + " to: " + backupName);
        File backupFile = new File(backupName);
        if (backupFile.exists()) {
            backupFile.delete();
        }

        //copy the old file to the backup location

        FileUtils.copyFile(destFile, backupFile);
        try {
            destFile.delete();
            FileUtils.copyFile(srcFile, destFile);
            srcFile.delete();
            logger.info("Successfully updated the " + destFile.getName() + " file");
        } catch (IOException e) {
            throw new IOException("You may need to restore the " + destFile.getAbsolutePath() + " file from: " + backupFile.getAbsolutePath() + "reason: " + e.getMessage());
        }
    }

    private void copyLunaJars(KeystoreConfigBean ksBean) {
        String lunaJarSourcePath = ksBean.getLunaJspPath() + "/lib/";
        String jreLibExtdestination = getOsFunctions().getPathToJreLibExt();
        String javaLibPathDestination = getOsFunctions().getPathToJavaLibPath();

        File srcDir = new File(lunaJarSourcePath);

        File destJarDir = new File(jreLibExtdestination);
        File destLibDir = new File(javaLibPathDestination);
        if (!destLibDir.exists()) {
            destLibDir.mkdir();
        }

        File[] fileList = srcDir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.toUpperCase().endsWith(".JAR");
            }
        });

        File[] dllFileList = srcDir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.toUpperCase().endsWith(".DLL") || s.toUpperCase().endsWith(".SO");
            }
        });

        try {
            //copy jars
            for (File file : fileList) {
                File destFile = new File(destJarDir, file.getName());
                logger.info("Copying " + file.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                FileUtils.copyFile(file, destFile);
            }

            //copy luna shared libs (dll or so)
            for (File file : dllFileList) {
                File destFile = new File(destLibDir, file.getName());
                logger.info("Copying " + file.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                FileUtils.copyFile(file, destFile);
            }

        } catch (IOException e) {
            logger.warning("Could not copy the Luna libraries - the files are possibly in use. Please close all programs and retry.");
            logger.warning(e.getMessage());
        }
    }

    private void setLunaSystemProps(KeystoreConfigBean ksBean) {
        OSSpecificFunctions.KeystoreInfo lunaInfo = getOsFunctions().getKeystore(KeystoreType.LUNA_KEYSTORE_NAME);
        if (lunaInfo != null)
            System.setProperty(LUNA_SYSTEM_CMU_PROPERTY, ksBean.getLunaInstallationPath() + lunaInfo.getMetaInfo("CMU_PATH"));
    }

    private void truncateKeystores(File caKeyStore, File sslKeyStore) {
        FileOutputStream emptyCaFileStream = null;
        FileOutputStream emptySslFileStream = null;

        try {
            emptyCaFileStream = new FileOutputStream(caKeyStore);
            emptyCaFileStream.close();
            emptyCaFileStream = null;

            emptySslFileStream = new FileOutputStream(sslKeyStore);
            emptySslFileStream.close();
            emptySslFileStream = null;
        } catch (FileNotFoundException e) {
            logger.warning("Error while creating 0 byte keystore: " + e.getMessage());
        } catch (IOException e) {
            logger.warning("Error while creating 0 byte keystore: " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(emptyCaFileStream);
            ResourceUtils.closeQuietly(emptySslFileStream);
        }


    }

    private boolean makeDefaultKeys(boolean doBothKeys, KeystoreConfigBean ksBean, String ksDir, char[] ksPassword) throws Exception {
        boolean keysDone;
        String args[] = new String[]
        {
                ksBean.getHostname(),
                ksDir,
                new String(ksPassword),
                new String(ksPassword),
                getKsType()
        };

        if (doBothKeys) {
            logger.info("Generating both keys");
            SetKeys.NewCa.main(args);
            keysDone = true;
        } else {
            logger.info("Generating only SSL key");
            SetKeys.ExistingCa.main(args);
            keysDone = true;
        }

        return keysDone;
    }



    private void updateServerConfig(File tomcatServerConfigFile, String sslKeyStorePath, char[] ksPassword) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(tomcatServerConfigFile);
            Document doc = XmlUtil.parse(fis);

            doConnectorElementInServerConfig(doc, sslKeyStorePath, ksPassword);
            fis.close();
            fis = null;

            fos = new FileOutputStream(tomcatServerConfigFile);
            XmlUtil.nodeToOutputStream(doc, fos);
            logger.info("Updating the server.xml");
            fos.close();
            fos = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
    }

    private void doConnectorElementInServerConfig(Document doc, String sslKeyStorePath, char[] ksPassword) {
        NodeList list = doc.getDocumentElement().getElementsByTagName("Connector");
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element)list.item(i);
            if (el.hasAttribute("secure") && el.getAttribute("secure").equalsIgnoreCase("true")) {
                el.setAttribute(XML_KSFILE, sslKeyStorePath);
                el.setAttribute(XML_KSPASS, new String(ksPassword));
                el.setAttribute(XML_KSTYPE, getKsType());
                el.setAttribute(XML_KSALIAS, KeyStoreConstants.PROP_KS_ALIAS_DEFAULTVALUE);
            }
        }
    }

    private void updateKeystoreProperties(File keystorePropertiesFile, char[] ksPassword) throws IOException {
        FileOutputStream fos = null;
        try {
            PropertiesConfiguration keystoreProps = PropertyHelper.mergeProperties(
                    keystorePropertiesFile,
                    new File(keystorePropertiesFile.getAbsolutePath() + "." + getOsFunctions().getUpgradedNewFileExtension()),
                    true, true);

            keystoreProps.setProperty(KeyStoreConstants.PROP_KS_TYPE, getKsType());
            keystoreProps.setProperty(KeyStoreConstants.PROP_KEYSTORE_DIR, getOsFunctions().getKeystoreDir());
            keystoreProps.setProperty(KeyStoreConstants.PROP_CA_KS_PASS, new String(ksPassword));
            keystoreProps.setProperty(KeyStoreConstants.PROP_SSL_KS_PASS, new String(ksPassword));
            keystoreProps.setProperty(KeyStoreConstants.PROP_KS_ALIAS, new String(KeyStoreConstants.PROP_KS_ALIAS_DEFAULTVALUE));

            fos = new FileOutputStream(keystorePropertiesFile);
            keystoreProps.setHeader(PROPERTY_COMMENT + "\n" + new Date());
            keystoreProps.save(fos, "iso-8859-1");
            logger.info("Updating the keystore.properties file");
            fos.close();
            fos = null;

        } catch (FileNotFoundException fnf) {
            logger.severe("error while updating the keystore properties file");
            logger.severe(fnf.getMessage());
            throw fnf;
        } catch (ConfigurationException ce) {
            logger.severe("error while updating the keystore properties file");
            logger.severe(ce.getMessage());
            throw new CausedIOException(ce);
        } catch (IOException ioex) {
            logger.severe("error while updating the keystore properties file");
            logger.severe(ioex.getMessage());
            throw ioex;
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    private String getKsType() {
        KeystoreType ksTypeFromBean = ((KeystoreConfigBean)configBean).getKeyStoreType();
        return ksTypeFromBean.shortTypeName();
    }

    private class MyScaManager extends ScaManager {
        public MyScaManager() throws ScaException {
            super();
        }

        public void startSca() throws ScaException {
            doStartSca();
        }

        public void stopSca() throws ScaException {
            doStopSca();
        }
    }
}
