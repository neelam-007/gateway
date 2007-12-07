package com.l7tech.server.config;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.security.AesKey;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.sql.*;
import java.util.logging.Logger;
import java.security.*;
import java.text.ParseException;

/**
 * User: megery
 * Date: Dec 4, 2007
 * Time: 1:56:02 PM
 */
public class SilentConfigurator {

    private static final Logger logger = Logger.getLogger(SilentConfigurator.class.getName());

    public static final String SYS_PROP_CONFIGDATA_FILENAME = "com.l7tech.config.silentConfigFile";
    public static final String SYS_PROP_DONTENCRYPT_CONFIGDATA = "com.l7tech.config.silentConfigFile.dontencrypt";
    byte[] configBlob;

    String configPath = null;
    OSSpecificFunctions osf;

    public SilentConfigurator(OSSpecificFunctions osf) {
        this.osf = osf;
        configPath = System.getProperty(SYS_PROP_CONFIGDATA_FILENAME,"");
    }

    public byte[] loadConfigFromDb(DBInformation dbinfo) {
        logger.info("Connecting to Database using " + dbinfo.getUsername() + "@" + dbinfo.getHostname() + "/" + dbinfo.getDbName());
        InputStream is = null;

        byte[] configBytes = null;
        try {
            if (StringUtils.isNotEmpty(configPath)) {
                is = new FileInputStream(configPath);
                configBytes = HexUtils.slurpStream(is);
            } else {
                //read it from the db
                DBActions dba = new DBActions(osf);
                Connection conn = null;
                Statement stmt = null;
                ResultSet rs = null;

                try {
                    conn = dba.getConnection(dbinfo);
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery("select configdata from config_data");
                    while (rs.next()) {
                      configBytes = rs.getBytes("configdata");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    ResourceUtils.closeQuietly(rs);
                    ResourceUtils.closeQuietly(stmt);
                    ResourceUtils.closeQuietly(conn);
                }
            }
        } catch (FileNotFoundException e) {
            logger.severe("Could not load the configuration: " + ExceptionUtils.getMessage(e));
        } catch (ClassNotFoundException e) {
            logger.severe("Could not load the configuration: " + ExceptionUtils.getMessage(e));
        } catch (IOException e) {
            logger.severe("Could not load the configuration: " + ExceptionUtils.getMessage(e));
        }
        return configBytes;
    }

    public SilentConfigData decryptConfigSettings(char[] passphrase, String configData) throws IllegalBlockSizeException, IOException, InvalidKeyException, ParseException, BadPaddingException, InvalidAlgorithmParameterException {
        XMLDecoder xdec = null;
        SilentConfigData config = null;
        byte[] decryptedConfigBytes = null;
        boolean dontDecrypt = Boolean.getBoolean(SYS_PROP_DONTENCRYPT_CONFIGDATA);
        if (dontDecrypt) {
            decryptedConfigBytes = configData.getBytes("UTF-8");
        } else {
            decryptedConfigBytes = decryptWithPassphrase(configData, passphrase).getBytes("UTF-8");
        }
        xdec = new XMLDecoder(new ByteArrayInputStream(decryptedConfigBytes));
        Object obj = xdec.readObject();
        config = (SilentConfigData) obj;

        return config;
    }

    public boolean saveConfigToDb(DBInformation dbinfo, char[] passphrase, SilentConfigData configData) {
        boolean allIsWell = true;
        OutputStream os = null;
        XMLEncoder xenc = null;
        Throwable hadException = null;
        boolean dontEncrypt = Boolean.getBoolean(SYS_PROP_DONTENCRYPT_CONFIGDATA);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xenc = new XMLEncoder(baos);
            xenc.writeObject(configData);
            xenc.close();
            xenc = null;


            byte[] encryptedConfig = null;
            if (dontEncrypt) {
                logger.warning("*** The configuration data, which contains sensitive information, will not be encrypted. Ensure that the system property \"" + SYS_PROP_DONTENCRYPT_CONFIGDATA + "\" is not set ***");
                encryptedConfig = baos.toByteArray();
            } else {
                encryptedConfig = encryptWithPassphrase(baos.toByteArray(), passphrase).getBytes("UTF-8");
            }

            if (StringUtils.isNotEmpty(configPath)) {
                os = new FileOutputStream(configPath);
                HexUtils.spewStream(encryptedConfig , os);
            } else {
                Connection conn = null;
                PreparedStatement pStmt = null;
                Statement stmt = null;
                ResultSet rs = null;
                DBActions dba;
                try {
                    dba = new DBActions(osf);
                    conn = dba.getConnection(dbinfo);
                    //check if there is already a row there
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery("select * from config_data where objectid=1");
                    if (!rs.first()) {
                        //it's not there, so create it.
                        stmt.execute("insert into config_data values (1, null);");
                    } else {
                        logger.info("The configuration data table is not empty. The contents will be overwritten.");
                    }

                    pStmt = conn.prepareStatement("update config_data set configdata=? where objectid=1");
                    pStmt.setBinaryStream(1, new ByteArrayInputStream(encryptedConfig), encryptedConfig.length);
                    pStmt.addBatch();
                    int[] updateCounts = pStmt.executeBatch();
                    if (updateCounts.length == 0 || updateCounts[0] == 0) {
                        logger.severe("Updating the configuration data in the database failed.");
                    }
                } finally {
                    ResourceUtils.closeQuietly(stmt);
                    ResourceUtils.closeQuietly(rs);
                    ResourceUtils.closeQuietly(pStmt);
                    ResourceUtils.closeQuietly(conn);
                }
            }
        } catch (FileNotFoundException e) {
            hadException = e;
        } catch (IOException e) {
            hadException = e;
        } catch (IllegalBlockSizeException e) {
            hadException = e;
        } catch (InvalidKeyException e) {
            hadException = e;
        } catch (BadPaddingException e) {
            hadException = e;
        } catch (InvalidAlgorithmParameterException e) {
            hadException = e;
        } catch (ClassNotFoundException e) {
            hadException = e;
        } catch (SQLException e) {
            hadException = e;
        }
        finally {
            if (hadException != null) {
                logger.severe("There was an error while saving the configuration data: " + ExceptionUtils.getMessage(hadException));
                allIsWell = false;
            }
            if (xenc != null) xenc.close();
        }
        return allIsWell;
    }

    private String encryptWithPassphrase(byte[] configBytes, char[] passphrase) throws IOException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String salt = generateSalt();
        AesKey key = getKey(salt, passphrase);
        if (key != null) {
            byte[] saltBytes = HexUtils.decodeBase64(salt);

            Cipher aes = getAes();
            aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(saltBytes));
            byte[] ciphertextBytes = aes.doFinal(configBytes);

            return salt + "$" + HexUtils.encodeBase64(ciphertextBytes, true);
        }

        return new String(configBytes);
    }


    private String decryptWithPassphrase(String encryptedConfig, char[] passphrase) throws ParseException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String[] components = encryptedConfig.split("\\$");

        String salt = components[0];
        String ciphertextBase64 = components[1];
        if (salt.length() < 1)
            throw new ParseException("Encrypted data does not have correct format: no salt", 0);
        if (ciphertextBase64.length() < 1)
            throw new ParseException("Encrypted data does not have correct format: no ciphertext", 0);

        AesKey ourkey = getKey(salt, passphrase);
        if (ourkey == null) return encryptedConfig;

        byte[] saltBytes = HexUtils.decodeBase64(salt);
        byte[] ciphertextBytes = HexUtils.decodeBase64(ciphertextBase64);

        Cipher aes = getAes();
        aes.init(Cipher.DECRYPT_MODE, ourkey, new IvParameterSpec(saltBytes));
        byte[] plaintextBytes = aes.doFinal(ciphertextBytes);

        return new String(plaintextBytes, "UTF-8");

    }

    private AesKey getKey(String salt, char[] password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-512");
            if (password == null)
                return null;
            byte[] mpBytes = new String(password).getBytes("UTF-8");
            byte[] saltBytes = salt.getBytes("UTF-8");

            sha.reset();
            sha.update(mpBytes);
            sha.update(saltBytes);
            sha.update(mpBytes);
            byte[] stage1 = sha.digest();

            sha.reset();
            sha.update(mpBytes);
            sha.update(saltBytes);
            sha.update(stage1);
            byte[] keybytes = sha.digest();

            return new AesKey(keybytes, 256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No SHA-512 implementation configured", e); // shouldn't happen
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8 implementation configured", e); // can't happen
        }
    }

    private String generateSalt() {
        Cipher aes = getAes();
        int blocksize = aes.getBlockSize();
        byte[] saltBytes = new byte[blocksize];
        new SecureRandom().nextBytes(saltBytes);
        return HexUtils.encodeBase64(saltBytes, true);
    }

    private Cipher getAes() {
        try {
            return Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No AES implementation available", e); // can't happen
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("No AES implementation available with PKCS5Padding", e); // can't happen
        }
    }
}
