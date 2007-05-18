package com.l7tech.server.config.commands;

import com.l7tech.common.security.prov.luna.LunaCmu;
import com.l7tech.common.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.sql.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 */
public class KeystoreConfigCommand extends BaseConfigurationCommand {

    private static final Logger logger = Logger.getLogger(KeystoreConfigCommand.class.getName());

    private static final String BACKUP_FILE_NAME = "keystore_config_backups";

    private static final String CA_KEYSTORE_FILE = "ca.ks";
    private static final String SSL_KEYSTORE_FILE = "ssl.ks";
    private static final String CA_CERT_FILE = "ca.cer";
    private static final String SSL_CERT_FILE = "ssl.cer";


    public static final String CA_ALIAS = "ssgroot";
    public static final String CA_DN_PREFIX = "cn=root.";
    public static final int CA_VALIDITY_DAYS = 5 * 365;

    public static final String SSL_ALIAS = "tomcat";
    public static final String SSL_DN_PREFIX = "cn=";
    public static final int SSL_VALIDITY_DAYS = 2 * 365;

    private static final String PROPERTY_COMMENT = "This file was updated by the SSG configuration utility";

    private static final String XML_KSFILE = "keystoreFile";
    private static final String XML_KSPASS = "keystorePass";
    private static final String XML_KSTYPE = "keystoreType";
    private static final String XML_KSALIAS  = "keyAlias";

    private static final String LUNA_SYSTEM_CMU_PROPERTY = "lunaCmuPath";

    private static final String PROPKEY_SECURITY_PROVIDER = "security.provider";
    private static final String PROPKEY_JCEPROVIDER = "com.l7tech.common.security.jceProviderEngine";
    private static final String PROPERTY_LUNA_JCEPROVIDER_VALUE = "com.l7tech.common.security.prov.luna.LunaJceProviderEngine";

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

    private static final String PKCS11_CFG_FILE = "pkcs11.cfg;";
    private static final String[] HSM_SECURITY_PROVIDERS =
            {
                "sun.security.pkcs11.SunPKCS11 " + PKCS11_CFG_FILE,
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE"
            };
    private KeystoreConfigBean ksBean;
    private SharedWizardInfo sharedWizardInfo;


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
                 if (ksType == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                    doDefaultKeyConfig(ksBean);
                    success = true;
                } else if (ksType == KeystoreType.LUNA_KEYSTORE_NAME) {
                    doLunaKeyConfig(ksBean);
                    success = true;
                } else if (ksType == KeystoreType.SCA6000_KEYSTORE_NAME) {
                     doHSMConfig(ksBean);
                     success = true;
                 }
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
        File caKeyStoreFile = new File( ksDir + CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + CA_CERT_FILE);
        File sslCertFile = new File(ksDir + SSL_CERT_FILE);
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
            updateJavaSecurity(ksBean, javaSecFile, newJavaSecFile,DEFAULT_SECURITY_PROVIDERS );
            updateKeystoreProperties(keystorePropertiesFile, ksPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
            updateSystemPropertiesFile(ksBean, systemPropertiesFile);
        } catch (Exception e) {
            logger.severe("problem generating keys or keystore - skipping keystore configuration");
            logger.severe(e.getMessage());
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
        File caKeyStoreFile = new File( ksDir + CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + CA_CERT_FILE);
        File sslCertFile = new File(ksDir + SSL_CERT_FILE);

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
            updateJavaSecurity(ksBean, javaSecFile, newJavaSecFile, LUNA_SECURITY_PROVIDERS);
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
        char[] ksPassword = ksBean.getKsPassword();
        String ksDir = getOsFunctions().getKeystoreDir();
        File keystoreDir = new File(ksDir);
        if (!keystoreDir.exists() && !keystoreDir.mkdir()) {
            String msg = "Could not create the directory: \"" + ksDir + "\". Cannot generate keystores";
            logger.severe(msg);
            throw new IOException(msg);
        }

        File keystorePropertiesFile = new File(getOsFunctions().getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        File caKeyStoreFile = new File( ksDir + CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + CA_CERT_FILE);
        File sslCertFile = new File(ksDir + SSL_CERT_FILE);
        File javaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile());
        File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());

        File pkcs11ConfigFile = new File(getOsFunctions().getSsgInstallRoot() + PKCS11_CFG_FILE);
        File newJavaSecFile  = new File(getOsFunctions().getPathToJavaSecurityFile() + ".new");

        File[] files = new File[]
        {
            javaSecFile,
            pkcs11ConfigFile,
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
            logger.info("Initializing HSM");
            try {

                //HSM Specific setup
                ScaManager scaManager = getScaManager();
                checkGDDCDongle();
                initializeSCA(scaManager);
                writePKCS11Config(pkcs11ConfigFile);
                createNewKeystoreOnHsm(javaSecFile, newJavaSecFile, ksDir, ksPassword);
                insertKeystoreIntoDatabase(scaManager);

                //General Keystore Setup
                updateKeystoreProperties(keystorePropertiesFile, ksPassword);
                updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
                updateSystemPropertiesFile(ksBean, systemPropertiesFile);
            } catch (ScaException e) {
                logger.severe("Error while initializing the SCA Manager: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.severe("problem initializing the HSM - skipping HSM configuration");
                logger.severe(e.getMessage());
                throw e;
            }
        } else {
            logger.info("Restoring HSM Backup");
            checkGDDCDongle();
        }
    }

    private ScaManager getScaManager() throws ScaException {
        return new ScaManager();
    }

    private void checkGDDCDongle() {
        //TODO get some GDDC and find some way to check for a reacharound
    }

    private void initializeSCA(ScaManager scaManager) {
        if (getOsFunctions().isUnix()) {
            startSCA();
            zeroHsm();
            emptyHsmKeydataDir(scaManager);
            String initializeScript = getOsFunctions().getSsgInstallRoot() + "bin/" + "initialize-hsm.expect <keystorepassword>";
            logger.info("Execute \"" + initializeScript + "\"");
        }
    }

    private void writePKCS11Config(File pkcs11ConfigFile) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(pkcs11ConfigFile);
            pw.println("name=SCA6000");
            pw.println("library=/usr/local/lib/pkcs11/PKCS11_API.so64");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly(pw);
        }
    }

    private void createNewKeystoreOnHsm(File javaSecFile, File newJavaSecFile, String ksDir, char[] hsmPassword) throws IllegalAccessException, IOException, ClassNotFoundException, InstantiationException, NoSuchAlgorithmException, SignatureException, CertificateException, InvalidKeyException, KeyStoreException {
        prepareJvmForNewKeystoreType(KeystoreType.SCA6000_KEYSTORE_NAME);
        updateJavaSecurity(ksBean, javaSecFile, newJavaSecFile, HSM_SECURITY_PROVIDERS);
        makeHSMKeys(new File(ksDir), hsmPassword);
    }

    private void insertKeystoreIntoDatabase(ScaManager scaManager) throws ScaException, ClassNotFoundException, SQLException {
        DBActions dba = null;
        byte[] keyData = null;

        try {
            dba = new DBActions();
            keyData = scaManager.loadKeydata();
        } catch (ClassNotFoundException e) {
            logger.severe("Could not connect to the database for keystore update: " + e.getMessage());
            throw e;
        } catch (ScaException e) {
            logger.severe("Could not connect to the database for keystore update: " + e.getMessage());
            throw e;
        }


        int originalVersion = -1;
        DBInformation dbinfo = sharedWizardInfo.getDbinfo();

        Connection connection = null;
        try {
            connection = dba.getConnection(dbinfo);
            try {
                originalVersion = getOriginalVersion(connection);
            } catch (SQLException e) {
                logger.severe("Could not determine the current version of the HSM keystore in the database: " + e.getMessage());
                throw e;
            }

            try {
                putKeydataInDatabase(connection, keyData, originalVersion);
            } catch (SQLException e) {
                logger.severe("Could not perform the keystore update: " + e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            logger.severe("Could not connect to the database to update the keystore: " + e.getMessage());
            throw e;
        } finally {
            if (connection != null) try {connection.close();} catch (SQLException e) {}
        }
    }

    private void putKeydataInDatabase(Connection connection, byte[] keyData, int originalVersion) throws SQLException {
        ByteArrayInputStream is = new ByteArrayInputStream(keyData);

        PreparedStatement preparedStmt = null;
        try {
            preparedStmt = connection.prepareStatement("update keystore_file set version=?, databytes=? where objectid=1 and name=\"HSM\"");
            preparedStmt.setInt(1, originalVersion+1);
            preparedStmt.setBinaryStream(2, is, keyData.length);
            preparedStmt.addBatch();
            preparedStmt.executeBatch();
            logger.info("inserted the HSM keystore information into the database.");
        } finally {
            if (preparedStmt != null) try {preparedStmt.close();} catch (SQLException e) {}
        }
    }

    private int getOriginalVersion(Connection connection) throws SQLException {
        int originalVersion = -1;
        String getVersionSql = new String("Select version from keystore_file where objectid=1 and name=\"HSM\"");

        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(getVersionSql);
            while(rs.next()) {
                originalVersion = rs.getInt("version");
            }
        } finally {
            if (stmt != null) try {stmt.close();} catch (SQLException e) {}
            if (originalVersion == -1) {
                logger.warning("Could not find an existing version for the HSM keystore in the database. Defaulting to 0");
                originalVersion = 0;
            } else {
                logger.info("Found an existing version for the HSM keystore in the database [" + originalVersion + "]");
            }
        }
        return originalVersion;
    }

    private void makeHSMKeys(File keystoreDir, char[] ksPassword) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignatureException, InvalidKeyException {
        if ( !keystoreDir.exists() ) throw new IOException( "Keystore directory '" + keystoreDir.getAbsolutePath() + "' does not exist" );
        if ( !keystoreDir.isDirectory() ) throw new IOException( "Keystore directory '" + keystoreDir.getAbsolutePath() + "' is not a directory" );
        String kstype = "PKCS11";
        char[] keystoreAccessPassword = ("gateway:"+new String(ksPassword)).toCharArray();

        //TODO try to refactor SetKeys to understand HSM-speak instead of just PKCS12 and then there's no need to reinvent things here.
        X509Certificate caCert = null;
        X509Certificate sslCert = null;
        PrivateKey caPrivateKey;

        KeyStore theHsmKeystore = KeyStore.getInstance(kstype);
        logger.info("Connecting to " + kstype + " keystore");
        theHsmKeystore.load(null,keystoreAccessPassword);


        logger.info("Generating RSA keypair for CA cert");
        KeyPair cakp = JceProvider.generateRsaKeyPair();
        caPrivateKey = cakp.getPrivate();

        logger.info("Generating self-signed CA cert");
        caCert = BouncyCastleRsaSignerEngine.makeSelfSignedRootCertificate(CA_DN_PREFIX + sharedWizardInfo.getHostname(), CA_VALIDITY_DAYS, cakp);

        logger.info("Storing CA cert in HSM");
        theHsmKeystore.setKeyEntry(CA_ALIAS, caPrivateKey, keystoreAccessPassword, new X509Certificate[] { caCert } );

        logger.info("Generating RSA keypair for SSL cert" );
        KeyPair sslkp = JceProvider.generateRsaKeyPair();

        logger.info("Generating SSL cert");
        sslCert = BouncyCastleRsaSignerEngine.makeSignedCertificate( SSL_DN_PREFIX + sharedWizardInfo.getHostname(),
                                                                       SSL_VALIDITY_DAYS,
                                                                       sslkp.getPublic(), caCert, caPrivateKey, RsaSignerEngine.CertType.SSL );

        logger.info("Storing SSL cert in HSM");
        theHsmKeystore.setKeyEntry(SSL_ALIAS, sslkp.getPrivate(), keystoreAccessPassword, new X509Certificate[] { sslCert, caCert } );

        File caCertFile = new File(keystoreDir,CA_CERT_FILE);
        File sslCertFile = new File(keystoreDir,SSL_CERT_FILE);

        createDummyKeystorse(keystoreDir);
        exportCerts(caCert, caCertFile, sslCert, sslCertFile);
    }

    private void createDummyKeystorse(File keystoreDir) throws IOException {
        File dummmyCaKeystore = new File(keystoreDir, CA_KEYSTORE_FILE);
        File summySslKeystore = new File(keystoreDir, SSL_KEYSTORE_FILE);
        
        dummmyCaKeystore.createNewFile();
        summySslKeystore.createNewFile();
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

    private void emptyHsmKeydataDir(ScaManager scaManager) {
        stopSca();
        logger.info("Deleting the " + scaManager.getKeydataDir() + " directory");
        //empty dir
        startSCA();
    }

    private void stopSca() {
        if (getOsFunctions().isUnix()) {
            String stopScaCommand = null;

            if (OSDetector.isLinux()) {
                stopScaCommand = "service sca stop";
            } else {
                stopScaCommand = "scvadmin stop sca";
            }
            logger.info("Stopping the SCA.");
            logger.info("Execute \"" + stopScaCommand + "\"");
        }
    }

    private void zeroHsm() {
        String zeroHSM = "scadiag -z mca0";
        logger.info("Execute \"" + zeroHSM + "\"");
    }

    private void startSCA() {
        if (getOsFunctions().isUnix()) {
            String startScaCommand = null;

            if (OSDetector.isLinux()) {
                startScaCommand = "service sca start";
            } else {
                startScaCommand = "scvadmin start sca";
            }
            logger.info("Starting the SCA.");
            logger.info("Execute \"" + startScaCommand + "\"");
        }
    }


    private void prepareJvmForNewKeystoreType(KeystoreType ksType) throws IllegalAccessException, InstantiationException, FileNotFoundException, ClassNotFoundException {
        Provider[] currentProviders = Security.getProviders();
        for (Provider provider : currentProviders) {
            Security.removeProvider(provider.getName());
        }

        String[] whichProviders = null;
        if (ksType == KeystoreType.DEFAULT_KEYSTORE_NAME || ksType == KeystoreType.SCA6000_KEYSTORE_NAME) {
            if (ksType == KeystoreType.DEFAULT_KEYSTORE_NAME)
                whichProviders = DEFAULT_SECURITY_PROVIDERS;
            else
                whichProviders = HSM_SECURITY_PROVIDERS;

            for (String providerName : whichProviders) {
                try {
                    Security.addProvider((Provider) Class.forName(providerName).newInstance());
                } catch (ClassNotFoundException e) {
                    logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                    throw e;
                }
            }
        } else if (ksType == KeystoreType.LUNA_KEYSTORE_NAME) {
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

        BufferedReader reader = null;
        PrintWriter writer = null;
        File newFile = new File(getOsFunctions().getSsgSystemPropertiesFile() + ".confignew");

        try {
            if (!systemPropertiesFile.exists()) {
                systemPropertiesFile.createNewFile();
            }
            reader = new BufferedReader(new FileReader(systemPropertiesFile));
            writer = new PrintWriter(newFile);
            String line = null;
            boolean jceProviderFound = false;

            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && line.startsWith(PROPKEY_JCEPROVIDER)) {
                    jceProviderFound = true;
                    if (ksBean.getKeyStoreType() == KeystoreType.LUNA_KEYSTORE_NAME) {
                        line = PROPKEY_JCEPROVIDER + "=" + PROPERTY_LUNA_JCEPROVIDER_VALUE;
                    }
                    else {
                        continue;
                    }
                }
                writer.println(line);
            }
            if (ksBean.getKeyStoreType() == KeystoreType.LUNA_KEYSTORE_NAME) {
                String lunaPropLine = PROPKEY_JCEPROVIDER + "=" + PROPERTY_LUNA_JCEPROVIDER_VALUE;
                if (!jceProviderFound) {
                    writer.println(lunaPropLine);
                }
                logger.info("Writing " + lunaPropLine + " to system.properties file");
            }
            reader.close();
            reader = null;
            writer.close();
            writer = null;

            logger.info("Updating the system.properties file");
            renameFile(newFile, systemPropertiesFile);

        } catch (FileNotFoundException e) {
            logger.severe("Error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("Error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void updateJavaSecurity(KeystoreConfigBean ksBean, File javaSecFile, File newJavaSecFile, String[] providersList) throws IOException {
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
            reader.close();
            reader = null;
            writer.close();
            writer = null;

            logger.info("Updating the java.security file");
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
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }

            if (writer != null) {
                writer.close();
            }
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
        } catch (IOException e) {
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
        if (ksTypeFromBean == KeystoreType.LUNA_KEYSTORE_NAME) {
            return "Luna";
        } else if (ksTypeFromBean == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                return "PKCS12";
        } else {
            return "PKCS11";
        }
    }
}
