package com.l7tech.server.config;

import com.l7tech.common.util.EncryptionUtil;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import org.apache.commons.lang.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: May 22, 2007
 * Time: 3:03:50 PM
 */
public class KeystoreActions {
    private static final Logger logger = Logger.getLogger(KeystoreActions.class.getName());
    private OSSpecificFunctions osFunctions;


    public KeystoreActions(OSSpecificFunctions osFunctions) {
        this.osFunctions = osFunctions;
    }

    public KeyStore loadKeyStore(char[] ksPassword, String ksType, File keystoreFile, boolean shouldTryAgain, KeystoreActionsListener listener) throws KeystoreActionsException {

        if (StringUtils.isEmpty(ksType)) {
            ksType = "PKCS12";
        }

        if (ksPassword == null) {
            if (listener != null) ksPassword = listener.promptForKeystorePassword("Please provide the password for the existing keystore.");
            else throw new KeystoreActionsException("No password provided for opening the keystore");
        }

        KeyStore existingSslKeystore = null;
        try {
            existingSslKeystore = KeyStore.getInstance(ksType);
            InputStream is = null;
            try {
                is = new FileInputStream(keystoreFile);
                existingSslKeystore.load(is, ksPassword);
            } catch (FileNotFoundException e) {
                throw new KeystoreActionsException(MessageFormat.format("Could not find the file \"{0}\". Cannot open the keystore", keystoreFile.getAbsolutePath()), e);
            } catch (NoSuchAlgorithmException e) {
                throw new KeystoreActionsException(MessageFormat.format("Could not load the keystore at \"{0}\". Unable to find an appropriate algorithm.", keystoreFile.getAbsolutePath()), e);
            } catch (IOException e) {
                if (e.getCause() instanceof UnrecoverableKeyException) {
                    logger.warning("Could not load the keystore. Possibly the wrong password.");
                    if (shouldTryAgain) {
                        if (listener != null) {
                            existingSslKeystore = loadKeyStore(null, ksType, keystoreFile, false, listener);
                        } else {
                            throw new KeystoreActionsException("Could not load the keystore with the given password");
                        }
                    } else {
                        throw new KeystoreActionsException("Could not load the keystore. Possibly the wrong password.");
                    }
                } else {
                    throw new KeystoreActionsException(MessageFormat.format("Could not open the keystore \"{0}\": {1}", keystoreFile.getAbsolutePath(), e.getMessage()), e);
                }
            } catch (CertificateException e) {
                throw new KeystoreActionsException(MessageFormat.format("Could not load the keystore at \"{0}\". At least one of the certificates is invalid. ({1})", keystoreFile.getAbsolutePath(), e.getMessage()));
            } finally{
                ResourceUtils.closeQuietly(is);
            }
        } catch (KeyStoreException e) {
            throw new KeystoreActionsException(MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage()), e);
        }

        return existingSslKeystore;
    }

    public byte[] getSharedKey(KeystoreActionsListener listener) throws KeystoreActionsException {
        byte[] sharedKey = null;

        String ksType = null;
        char[] ksPassword = null;
        String ksDir = null;
        String ksFilename = null;

        try {
            Map<String, String> props = PropertyHelper.getProperties(osFunctions.getKeyStorePropertiesFile(), new String[]{
                KeyStoreConstants.PROP_KS_TYPE, KeyStoreConstants.PROP_SSL_KS_PASS
            });
            ksType = props.get(KeyStoreConstants.PROP_KS_TYPE);
            ksPassword = props.get(KeyStoreConstants.PROP_SSL_KS_PASS).toCharArray();
            ksDir = props.get(KeyStoreConstants.PROP_KEYSTORE_DIR);
            ksFilename = props.get(KeyStoreConstants.PROP_SSL_KEYSTORE_FILE);
        } catch (IOException e) {
            logger.warning("Keystore configuration could not be loaded. Assuming defaults");
        } finally {
            if (StringUtils.isEmpty(ksType)) ksType = "PCKS12";
            if (StringUtils.isEmpty(ksDir)) ksDir = osFunctions.getKeystoreDir();
            if (StringUtils.isEmpty(ksFilename)) ksFilename = KeyStoreConstants.SSL_KEYSTORE_FILE;
        }

        File existingKeystoreFile = new File(ksDir, ksFilename);
        if (!existingKeystoreFile.exists()) {
            logger.info(MessageFormat.format("No existing keystore found. No need to backup shared key. (tried {0})", existingKeystoreFile.getAbsolutePath()));
        } else {
            KeyStore existingKeystore = loadKeyStore(ksPassword, ksType, new File(ksDir, ksFilename), true, listener);
            try {
                sharedKey = fetchDecryptedSharedKeyFromDatabase(SharedWizardInfo.getInstance().getDbinfo(), existingKeystore, ksPassword);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }

        return sharedKey;
    }

    private byte[] fetchDecryptedSharedKeyFromDatabase(DBInformation dbInfo, KeyStore existingKeystore, char[] ksPassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeystoreActionsException {
        byte[] sharedKey = null;
        Key privateKey = existingKeystore.getKey(KeyStoreConstants.SSL_ALIAS, ksPassword);
        java.security.cert.Certificate[] chain = existingKeystore.getCertificateChain(KeyStoreConstants.SSL_ALIAS);
        PublicKey publicKey = chain[0].getPublicKey();
        String pubKeyId = EncryptionUtil.computeCustomRSAPubKeyID((RSAPublicKey) publicKey);
        Connection conn = null;
        Statement stmt = null;

        try {
            DBActions dba = new DBActions();
            conn = dba.getConnection(dbInfo);
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select b64edval from shared_keys where encodingid = \"" + pubKeyId + "\"");

            String keyData = null;
            while(rs.next()) {
                keyData = rs.getString("b64edval");
            }
            if (StringUtils.isEmpty(keyData)) {
                logger.info("No encrypted shared key found for the current SSL key. No need to save it.");
            } else {
                sharedKey = EncryptionUtil.deB64AndRsaDecrypt(keyData, privateKey);
            }
        } catch (ClassNotFoundException e) {
            logger.severe(MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage()));
            throw new KeystoreActionsException();
        } catch (SQLException e) {
           logger.severe(MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage()));
            throw new KeystoreActionsException();
        } catch (BadPaddingException e) {
            logger.severe(MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage()));
            throw new KeystoreActionsException();
        } catch (IOException e) {
            logger.severe(MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage()));
            throw new KeystoreActionsException();
        } catch (IllegalBlockSizeException e) {
            logger.severe(MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage()));
            throw new KeystoreActionsException();
        } catch (InvalidKeyException e) {
            logger.severe(MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage()));
            throw new KeystoreActionsException();
        } catch (NoSuchPaddingException e) {
            logger.severe(MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage()));
            throw new KeystoreActionsException();
        } finally{
            ResourceUtils.closeQuietly(conn);
            ResourceUtils.closeQuietly(stmt);
        }
        return sharedKey;
    }

    public void backupHsm() {
    }

    public void restoreHsm() {

    }



    public class KeystoreActionsException extends Exception {

        public KeystoreActionsException() {
        }

        public KeystoreActionsException(String message) {
            super(message);
        }

        public KeystoreActionsException(String message, Throwable cause) {
            super(message, cause);
        }

        public KeystoreActionsException(Throwable cause) {
            super(cause);
        }
    }
}
