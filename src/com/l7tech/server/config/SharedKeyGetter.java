package com.l7tech.server.config;

import com.l7tech.common.util.EncryptionUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.exceptions.KeystoreActionsException;
import org.apache.commons.lang.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.sql.*;
import java.text.MessageFormat;

/**
 * User: megery
 * Date: Jul 19, 2007
 * Time: 2:53:55 PM
 */
public class SharedKeyGetter {
    public static final String SHARED_KEY_FILE_NAME = "sharedkey";

    byte[] output = null;

    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Not enough arguments to proceed.");
            System.out.println(usage());
            System.exit(2);
        }
        String ksPassword = args[0];
        String ksType = args[1];
        String ksFile = args[2];
        String dbHost = args[3];
        String dbName = args[4];
        String dbUsername = args[5];
        String dbPasssword = args[6];

        SharedKeyGetter getter = new SharedKeyGetter();
        int result = getter.retrieveSharedKeyFromDbAndStash(ksPassword, ksType, ksFile, dbHost, dbName, dbUsername, dbPasssword);
        System.out.print(new String(getter.getOutput()));
        System.exit(result);
    }

    private byte[] getOutput() {
        return output;
    }

    private static String usage() {
        return "keystorepassword, keystoretype, keystorefile, databaseHost, databaseName, databaseUsername, databasePassword";
    }

    public int retrieveSharedKeyFromDbAndStash(String ksPassword, String ksType, String keystoreFile, String dbHost, String dbName, String dbUsername, String dbPasssword) {
        int status = 0;
        KeystoreType whichKeystoreType = getKeystoreType(ksType);
        if (whichKeystoreType == KeystoreType.SCA6000_KEYSTORE_NAME &&
            ksPassword != null && !ksPassword.contains(":")) {
                ksPassword = "gateway:"+ksPassword;
        }

        KeyStore existingSslKeystore = null;
        byte[] sharedKey = null;
        String errMsg = null;
        try {
            KeystoreActions ka = new KeystoreActions(OSDetector.getOSSpecificFunctions());
            ka.prepareJvmForNewKeystoreType(whichKeystoreType);
            existingSslKeystore = KeyStore.getInstance(ksType);
            InputStream is = null;
            try {
                if (KeystoreType.SCA6000_KEYSTORE_NAME.shortTypeName().equals(ksType)) {
                    is = null;
                } else {
                    is = new FileInputStream(keystoreFile);
                }
                existingSslKeystore.load(is, ksPassword.toCharArray());
                DBInformation dbinfo = new DBInformation(dbHost, dbName, dbUsername, dbPasssword, "", "");
                sharedKey = fetchRawSharedKeyFromDatabase(dbinfo, existingSslKeystore, ksPassword.toCharArray());
                try {
                    obfuscateAndStashSharedKey(sharedKey);
                } catch (IOException e) {
                    errMsg = MessageFormat.format("Could not save the shared key. Cannot proceed because data loss will occur. [{0}]", e.getMessage());
                    status = 99;
                }
            } catch (FileNotFoundException e) {
                errMsg = MessageFormat.format("Could not find the file \"{0}\". Cannot open the keystore", keystoreFile);
                status = 2;
            } catch (NoSuchAlgorithmException e) {
                errMsg = MessageFormat.format("Could not load the keystore at \"{0}\". Unable to find an appropriate algorithm.", keystoreFile);
                status = 3;
            } catch (IOException e) {
                errMsg = "Could not load the keystore. Possibly the wrong password.";
                status = 1;
            } catch (CertificateException e) {
                errMsg = MessageFormat.format("Could not load the keystore at \"{0}\". At least one of the certificates is invalid. ({1})", keystoreFile, e.getMessage());
                status = 5;
            } catch (KeystoreActionsException e) {
                errMsg = MessageFormat.format("Could not retrieve the shared key ({0}).", e.getMessage());
                status = 14;
            } catch (UnrecoverableKeyException e) {
                errMsg = MessageFormat.format("Could not retrieve the shared key ({0}).", e.getMessage());
                status = 15;
            } finally{
                ResourceUtils.closeQuietly(is);
            }
        } catch (KeyStoreException e) {
            errMsg = MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage());
            status = 7;
        } catch (InvocationTargetException e) {
            errMsg = MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage());
            status = 8;
        } catch (ClassNotFoundException e) {
            errMsg = MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage());
            status = 9;
        } catch (NoSuchMethodException e) {
            errMsg = MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage());
            status = 10;
        } catch (FileNotFoundException e) {
            errMsg = MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage());
            status = 11;
        } catch (InstantiationException e) {
            errMsg = MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage());
            status = 12;
        } catch (IllegalAccessException e) {
            errMsg = MessageFormat.format("Error while accessing the keystore as type {0} : {1}", ksType, e.getMessage());
            status = 13;
        } finally {
            if (status != 0) {
                if (errMsg != null) System.err.println(errMsg);
            } else {
                output = sharedKey;
            }
        }
        
        return status;
    }

    private KeystoreType getKeystoreType(String ksType) {
        KeystoreType whichKeystoreType = KeystoreType.DEFAULT_KEYSTORE_NAME;
        if (KeystoreType.SCA6000_KEYSTORE_NAME.shortTypeName().equals(ksType)) {
            whichKeystoreType = KeystoreType.SCA6000_KEYSTORE_NAME;
        } else if (KeystoreType.DEFAULT_KEYSTORE_NAME.shortTypeName().equals(ksType)) {
            whichKeystoreType = KeystoreType.DEFAULT_KEYSTORE_NAME;
        } else if (KeystoreType.LUNA_KEYSTORE_NAME.shortTypeName().equals(ksType)) {
            whichKeystoreType = KeystoreType.LUNA_KEYSTORE_NAME;
        }
        return whichKeystoreType;
    }

    /**
     * Returns the raw shared key from the database. This has been decrypted using the private key in the keystore and
     * is not base64 encoded.
     * @param dbInfo Database information needed to connect
     * @param existingKeystore the keystore from which to fetch the relevant keypair
     * @param ksPassword the password used to protect the keystore/private key
     * @return the raw key data that has been decrypted using the private key in the keystore and
     * is not base64 encoded.
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws KeystoreActionsException
     */
    private byte[] fetchRawSharedKeyFromDatabase(DBInformation dbInfo, KeyStore existingKeystore, char[] ksPassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeystoreActionsException {
        byte[] sharedKey = null;
        Key privateKey = existingKeystore.getKey(KeyStoreConstants.SSL_ALIAS, ksPassword);
        java.security.cert.Certificate[] chain = existingKeystore.getCertificateChain(KeyStoreConstants.SSL_ALIAS);
        if (chain == null || chain.length ==0) {
            throw new KeystoreActionsException(MessageFormat.format("Attempt to fetch the certificate from the keystore failed. No {0} alias could be found", KeyStoreConstants.SSL_ALIAS));
        }
        PublicKey publicKey = chain[0].getPublicKey();
        String pubKeyId = EncryptionUtil.computeCustomRSAPubKeyID((RSAPublicKey) publicKey);
        Connection conn = null;
        PreparedStatement stmt = null;

        String errMsg = null;
        try {
            DBActions dba = new DBActions();
            conn = dba.getConnection(dbInfo);
            stmt = conn.prepareStatement("select b64edval from shared_keys where encodingid = ?");
            stmt.setString(1, pubKeyId);
            ResultSet rs = stmt.executeQuery();
            String keyData = null;
            while(rs.next()) {
                keyData = rs.getString("b64edval");
            }
            if (StringUtils.isEmpty(keyData)) {
                sharedKey = new byte[0];
            } else {
                //raw key data
                sharedKey = EncryptionUtil.deB64AndRsaDecrypt(keyData, privateKey);
            }
        } catch (ClassNotFoundException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (SQLException e) {
            errMsg = MessageFormat.format("Could not connect to the database to retrieve the encrypted shared key. Cannot proceed. ({0})", e.getMessage());
        } catch (BadPaddingException e) {
            errMsg = MessageFormat.format("Error while decrypting the shared key. Cannot proceed. ({0}:{1})", e.getClass().getName(), e.getMessage());
        } catch (IOException e) {
            errMsg = MessageFormat.format("Error while decrypting the shared key. Cannot proceed. ({0}:{1})", e.getClass().getName(), e.getMessage());
        } catch (IllegalBlockSizeException e) {
            errMsg = MessageFormat.format("Error while decrypting the shared key. Cannot proceed. ({0}:{1})", e.getClass().getName(), e.getMessage());
        } catch (InvalidKeyException e) {
            errMsg = MessageFormat.format("Error while decrypting the shared key. Cannot proceed. ({0}:{1})", e.getClass().getName(), e.getMessage());
        } catch (NoSuchPaddingException e) {
            errMsg = MessageFormat.format("Error while decrypting the shared key. Cannot proceed. ({0}:{1})", e.getClass().getName(), e.getMessage());
        } finally{
            ResourceUtils.closeQuietly(conn);
            ResourceUtils.closeQuietly(stmt);
        }

        if (errMsg != null) {
            throw new KeystoreActionsException(errMsg);
        }
        return sharedKey;
    }

    public byte[] getAndDecryptStashedSharedKey() throws IOException {
        //read the obfuscated shared key from the disk
        byte[] obfuscatedSharedKey = getStashedSharedKey();

        if (obfuscatedSharedKey == null) return null;
        //decrypt the sharedKey
        return deObfuscateSharedKey(obfuscatedSharedKey);
    }

    private byte[] getStashedSharedKey() throws IOException {
        File sharedKeyFile = new File(SHARED_KEY_FILE_NAME);
        byte[] sharedKeyBytes = null;
        if (sharedKeyFile.exists()) { //then there was a shared key that was backed up
            InputStream is = null;
            try {
                is = new FileInputStream(sharedKeyFile);
                sharedKeyBytes = HexUtils.slurpStream(is);
            } finally {
                ResourceUtils.closeQuietly(is);
            }
        }
        return sharedKeyBytes;
    }

    private byte[] deObfuscateSharedKey(byte[] obfuscatedSharedKey) {
        return obfuscatedSharedKey;
    }

    private void obfuscateAndStashSharedKey(byte[] sharedKey) throws IOException {
        if (sharedKey == null || sharedKey.length == 0) return;
        byte[] obfuscatedSharedKey = obfuscateSharedKey(sharedKey);
        stashSharedKey(obfuscatedSharedKey);
    }

    private byte[] obfuscateSharedKey(byte[] sharedKey) {
        return sharedKey;
    }

    private void stashSharedKey(byte[] sharedKeyBytes) throws IOException {
        File sharedKeyFile = new File(SHARED_KEY_FILE_NAME);
        OutputStream os = null;
        try {
            os = new FileOutputStream(sharedKeyFile);
            HexUtils.spewStream(sharedKeyBytes, os);
            os.flush();
        } finally {
            ResourceUtils.closeQuietly(os);
        }
    }
}
