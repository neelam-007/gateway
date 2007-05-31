package com.l7tech.server.config;

import com.l7tech.common.util.EncryptionUtil;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.ProcResult;
import com.l7tech.common.util.ProcUtils;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.beans.KeystoreConfigBean;
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
import java.util.List;
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

    public KeyStore loadExistingKeyStore(File keystoreFile, boolean shouldTryAgain, KeystoreActionsListener listener) throws KeystoreActionsException {
        String ksPassword = null;
        String ksType = null;

        if (listener != null) {
            List<String> answers = listener.promptForKeystoreTypeAndPassword();
            ksType = answers.get(1);
            ksPassword = answers.get(0);
            if (KeystoreType.SCA6000_KEYSTORE_NAME.shortTypeName().equals(ksType) && ksPassword != null) {
                if (!ksPassword.contains(":"))
                    ksPassword = "gateway:"+ksPassword;
            }
        }

        KeyStore existingSslKeystore = null;
        try {
            existingSslKeystore = KeyStore.getInstance(ksType);
            InputStream is = null;
            try {
                if (KeystoreType.SCA6000_KEYSTORE_NAME.shortTypeName().equals(ksType)) {
                    is = null;
                } else {
                    is = new FileInputStream(keystoreFile);
                }
                existingSslKeystore.load(is, ksPassword.toCharArray());
            } catch (FileNotFoundException e) {
                throw new KeystoreActionsException(MessageFormat.format("Could not find the file \"{0}\". Cannot open the keystore", keystoreFile.getAbsolutePath()), e);
            } catch (NoSuchAlgorithmException e) {
                throw new KeystoreActionsException(MessageFormat.format("Could not load the keystore at \"{0}\". Unable to find an appropriate algorithm.", keystoreFile.getAbsolutePath()), e);
            } catch (IOException e) {
                if (e.getCause() instanceof UnrecoverableKeyException) {
                    logger.warning("Could not load the keystore. Possibly the wrong password.");
                    if (shouldTryAgain) {
                        if (listener != null) {
                            existingSslKeystore = loadExistingKeyStore(keystoreFile, false, listener);
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
            KeyStore existingKeystore = loadExistingKeyStore(new File(ksDir, ksFilename), true, listener);
            try {
                sharedKey = fetchDecryptedSharedKeyFromDatabase(SharedWizardInfo.getInstance().getDbinfo(), existingKeystore, ksPassword);
            } catch (NoSuchAlgorithmException e) {
                logger.severe("Error loading existing keystore: " + e.getMessage());
                throw new KeystoreActionsException("Error loading existing keystore: " + e.getMessage());
            } catch (UnrecoverableKeyException e) {
                logger.severe("Error loading existing keystore: " + e.getMessage());
                throw new KeystoreActionsException("Error loading existing keystore: " + e.getMessage());
            } catch (KeyStoreException e) {
                logger.severe("Error loading existing keystore: " + e.getMessage());
                throw new KeystoreActionsException("Error loading existing keystore: " + e.getMessage());
            }
        }

        return sharedKey;
    }

    private byte[] fetchDecryptedSharedKeyFromDatabase(DBInformation dbInfo, KeyStore existingKeystore, char[] ksPassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeystoreActionsException {
        byte[] sharedKey = null;
        Key privateKey = existingKeystore.getKey(KeyStoreConstants.SSL_ALIAS, ksPassword);
        java.security.cert.Certificate[] chain = existingKeystore.getCertificateChain(KeyStoreConstants.SSL_ALIAS);
        if (chain == null || chain.length ==0) {
            throw new KeystoreActionsException(MessageFormat.format("Attempt to fetch the certificate from the keystore failed. No {0} alias could be found", KeyStoreConstants.SSL_ALIAS));
        }
        PublicKey publicKey = chain[0].getPublicKey();
        String pubKeyId = EncryptionUtil.computeCustomRSAPubKeyID((RSAPublicKey) publicKey);
        Connection conn = null;
        Statement stmt = null;

        String errMsg = null;
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
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (SQLException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (BadPaddingException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (IOException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (IllegalBlockSizeException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (InvalidKeyException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (NoSuchPaddingException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } finally{
            ResourceUtils.closeQuietly(conn);
            ResourceUtils.closeQuietly(stmt);
        }

        if (errMsg != null) {
            logger.severe(errMsg);
            throw new KeystoreActionsException(errMsg);
        }
        return sharedKey;
    }

    public void backupHsm() {
    }

    public void restoreHsm() {

    }

    public void probeUSBBackupDevice() throws KeystoreActionsException {
        String prober = osFunctions.getSsgInstallRoot() + KeystoreConfigBean.MASTERKEY_MANAGE_SCRIPT;
        try {
            ProcResult result = ProcUtils.exec(null, new File(prober), ProcUtils.args("probe"), null, true);
            if (result.getExitStatus() == 0) {
                //all is well, USB stick is there
                logger.info("Detected a supported backup storage device.");
            } else {
                String message = "A supported backup storage device was not found.";
                logger.warning(MessageFormat.format(message + " Return code = {0}, Message={1}", result.getExitStatus(), new String(result.getOutput())));
                throw new KeystoreActionsException(message);
            }
        } catch (IOException e) {
            throw new KeystoreActionsException("An error occurred while attempting to find a supported backup storage device: " + e.getMessage());
        }
    }

    public class KeystoreActionsException extends Exception {
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
