package com.l7tech.gateway.config.manager;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.gateway.config.manager.db.DBActions;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.sql.*;
import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that can extract/decrypt/save/encrypt shared keys for a given database config using and supplied cluster
 * passphrase.
 */

public class ClusterPassphraseManager {
    private static final Logger logger = Logger.getLogger(ClusterPassphraseManager.class.getName());
    private static final String CIPHER = "PBEWithSHA1AndDESede";

    private final DatabaseConfig dbConfig;
    private byte[] sharedKey = null;
    private boolean useAdminUser;

    public ClusterPassphraseManager(final DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
        //TODO assume that dbconfig contains a decrypted password?
    }

    /**
     * Extracts the shared key from the database and decrypts it using the given cluster passphrase.
     *
     * @param clusterPassphrase cluster passphrase to be validated
     * @return a byte array containing the shared key, or null if the cluster passphrase was incorrect
     * @throws IOException if there is an error retrieving or decrypting the shared key
     */
    public byte[] getDecryptedSharedKey(String clusterPassphrase) throws IOException {
        // Read shared key from DB
        String sharedKeyB64 = readSharedKeyB64(dbConfig);
        if (sharedKeyB64 == null) {
            throw new CausedIOException("Shared key not found.");
        }

        byte[] sk = null;
        SecretKey sharedKeyDecryptionKey;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER);
            sharedKeyDecryptionKey = skf.generateSecret(new PBEKeySpec(clusterPassphrase.toCharArray()));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to initialize decryption key for cluster shared key: " + ExceptionUtils.getMessage(e), e);
        }

        try {
            sk = decryptKey(sharedKeyB64, sharedKeyDecryptionKey);
        } catch (GeneralSecurityException gse) {
            logger.log(Level.INFO, "Unable to decrypt shared key (incorrect password?)", ExceptionUtils.getDebugException(gse));
        }

        setSharedKey(sk);
        return sk;
    }

    public void setSharedKey(byte[] sk) {
        sharedKey = sk;
    }

    /**
     * Encrypts the shared key with the given cluster passphrase and saves it to the database.
     *
     * @param newPassphrase the new cluster passphrase
     * @throws CausedIOException if there is an error saving the shared key or
     */
    public void saveAndEncryptSharedKey(final String newPassphrase) throws CausedIOException {
        //TODO assume shared key has been retrived and is not == null?
        SecretKey newSharedKeyDecryptionKey;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER);
            newSharedKeyDecryptionKey = skf.generateSecret(new PBEKeySpec(newPassphrase.toCharArray()));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to initialize encryption key for cluster shared key: " + ExceptionUtils.getMessage(e), e);
        }

        // Save shared key to DB
        try {
            saveSharedKeyB64(dbConfig, encryptKey(sharedKey, newSharedKeyDecryptionKey));
        } catch (GeneralSecurityException gse) {
            throw new CausedIOException("Unable to encrypt shared key", gse);
        }
    }


    public boolean isUseAdminUser() {
        return useAdminUser;
    }

    public void setUseAdminUser(boolean useAdminUser) {
        this.useAdminUser = useAdminUser;
    }

    private String readSharedKeyB64(final DatabaseConfig config) throws CausedIOException {
        String sharedKeyB64 = null;

        DBActions dbactions = new DBActions();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbactions.getConnection(config, useAdminUser, false);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select b64edval from shared_keys where shared_keys.encodingid = '%ClusterWidePBE%'");
            if (resultSet.next()) {
                sharedKeyB64 = resultSet.getString(1);
            }
        } catch (SQLException sqle) {
            throw new CausedIOException("Could not read shared key: " + sqle.getMessage(), sqle);
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }

        return sharedKeyB64;
    }

    private void saveSharedKeyB64(final DatabaseConfig config, final String sharedKeyB64) throws CausedIOException {
        // Read shared key from DB
        DBActions dbactions = new DBActions();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbactions.getConnection(config, false);
            statement = connection.prepareStatement("update shared_keys set b64edval = ? where shared_keys.encodingid = '%ClusterWidePBE%'");
            statement.setString(1, sharedKeyB64);
            int result = statement.executeUpdate();
            if (result != 1) {
                throw new CausedIOException("Could not save shared key.");
            }
        } catch (SQLException sqle) {
            throw new CausedIOException("Could not save shared key.", sqle);
        } finally {
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }
    }

    private String encryptKey(final byte[] toEncrypt, final SecretKey sharedKeyDecryptionKey)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, sharedKeyDecryptionKey);
        byte[] cipherBytes = cipher.doFinal(toEncrypt);
        PBEParameterSpec pbeSpec = cipher.getParameters().getParameterSpec(PBEParameterSpec.class);
        byte[] salt = pbeSpec.getSalt();
        int itc = pbeSpec.getIterationCount();
        return "$PBE1$" + HexUtils.encodeBase64(salt, true) + "$" + itc + "$" + HexUtils.encodeBase64(cipherBytes, true) + "$";
    }

    private byte[] decryptKey(final String b64edEncKey, final SecretKey sharedKeyDecryptionKey)
            throws IOException, GeneralSecurityException {
        Pattern pattern = Pattern.compile("^\\$PBE1\\$([^$]+)\\$(\\d+)\\$([^$]+)\\$$");
        Matcher matcher = pattern.matcher(b64edEncKey);
        if (!matcher.matches())
            throw new IOException("Invalid shared key format: " + b64edEncKey);
        String b64edSalt = matcher.group(1);
        String strIterationCount = matcher.group(2);
        String b64edCiphertext = matcher.group(3);

        byte[] saltbytes;
        int iterationCount;
        byte[] cipherbytes;
        try {
            saltbytes = HexUtils.decodeBase64(b64edSalt);
            iterationCount = Integer.parseInt(strIterationCount);
            cipherbytes = HexUtils.decodeBase64(b64edCiphertext);
        } catch (NumberFormatException nfe) {
            throw new IOException("Invalid shared key format: " + b64edEncKey, nfe);
        }

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, sharedKeyDecryptionKey, new PBEParameterSpec(saltbytes, iterationCount));
        return cipher.doFinal(cipherbytes);
    }
}
