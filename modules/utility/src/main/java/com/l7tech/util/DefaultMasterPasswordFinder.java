package com.l7tech.util;

import com.l7tech.util.IOUtils;

import java.io.*;
import java.security.SecureRandom;
import java.util.Random;

/**
 * A MasterPasswordFinder that knows how to read an obfuscated master password from a config file.
 * 
 * <p>This finder is uncached -- it will always open and read the config file whenever someone asks for
 * the master password.</p>
 */
public class DefaultMasterPasswordFinder implements MasterPasswordManager.MasterPasswordFinder {
    public static final String PROP_MASTER_PASSWORD_PATH = "com.l7tech.masterPasswordPath";

    private static final String OBFUSCATION_PREFIX = "$L7O$"; // do not change this, for backward compat.  can add new schemes, though
    private static final long OBFUSCATION_SEED = 171717L; // do not change this, for backward compat.  can add new schemes, though
    private static final Random random = new SecureRandom();

    private final File masterPasswordFile;

    /**
     * Create a MasterPasswordFinder that will look for the master password in the file specified by
     * the system property com.l7tech.masterPasswordPath.
     *
     * <p>When using this you must set the system property</p>
     */
    public DefaultMasterPasswordFinder() {
         masterPasswordFile = null;
    }

    /**
     * Create a MasterPasswordFinder that will always read the obfuscated master password from the
     * specified file.
     *
     * @param masterPasswordFile the file containing nothing but the obfuscated master password
     */
    public DefaultMasterPasswordFinder(File masterPasswordFile) {
        this.masterPasswordFile = masterPasswordFile;
    }

    /**
     * Obfuscate a password using a randomly-chosen salt.
     * This is a utility method provided for use by MasterPasswordFinder implementations.
     *
     * @param clear the cleartext string that is to be obfuscated.  Required.
     * @return the obfuscated form of the password.
     */
    public static String obfuscate(String clear) {
        long salt = random.nextLong();
        return obfuscate(clear, salt);
    }

    /**
     * Obfuscate a password using the specified salt.
     * This is a utility method provided for use by MasterPasswordFinder implementations.
     *
     * @param clear the cleartext string that is to be obfuscated.  Required.
     * @param salt a specific salt to use for the obfuscation
     * @return the obfuscated form of the password.
     */
    public static String obfuscate(String clear, long salt) {
        try {
            //noinspection UnsecureRandomNumberGeneration
            Random rand = new Random(OBFUSCATION_SEED + salt);
            byte[] in = clear.getBytes("UTF-8");
            byte[] out = new byte[in.length];
            for (int i = 0; i < in.length; i++) {
                byte b = in[i];
                b ^= rand.nextInt();
                out[i] = b;
            }
            byte[] saltBytes = String.valueOf(salt).getBytes("UTF-8");
            return OBFUSCATION_PREFIX + HexUtils.encodeBase64(saltBytes, true) + '$' + HexUtils.encodeBase64(out, true);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unobfuscate a password.
     * This is a utility method provided for use by MasterPasswordFinder implementations.
     * If this method is changed in the future, it must retain the ability to unobfuscate
     * passwords saved in the original $L7O$ format.
     *
     * @param obfuscated the obfuscated password.  Required.
     * @return the unobfuscated string
     * @throws IOException if the obfuscated password is not correctly formatted base-64 or does not result in valid utf-8
     */
    public static String unobfuscate(String obfuscated) throws IOException {
        long salt = getSalt(obfuscated);
        obfuscated = obfuscated.substring(OBFUSCATION_PREFIX.length());
        int dollarPos = obfuscated.indexOf('$');
        if (dollarPos < 1 || dollarPos >= obfuscated.length() - 1)
            throw new IOException("Invalid obfuscated password");
        obfuscated = obfuscated.substring(dollarPos + 1);
        //noinspection UnsecureRandomNumberGeneration
        Random rand = new Random(OBFUSCATION_SEED + salt);
        byte[] in = HexUtils.decodeBase64(obfuscated);
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            byte b = in[i];
            b ^= rand.nextInt();
            out[i] = b;
        }
        return new String(out, "UTF-8");
    }

    /**
     * Get the salt used to obfuscate the specified obfuscated password.
     *
     * @param obfuscated  the obfuscated password to examine. Required.
     * @return the salt that was used to obfuscate it.
     * @throws IOException if the obfuscated password is not correctly formatted base-64 or does not result in valid utf-8
     */
    public static long getSalt(String obfuscated) throws IOException {
        if (!obfuscated.startsWith(OBFUSCATION_PREFIX) || obfuscated.length() <= OBFUSCATION_PREFIX.length())
            throw new IOException("Invalid obfuscated password");
        obfuscated = obfuscated.substring(OBFUSCATION_PREFIX.length());
        int dollarPos = obfuscated.indexOf('$');
        if (dollarPos < 1 || dollarPos >= obfuscated.length() - 1)
            throw new IOException("Invalid obfuscated password");
        String saltString = obfuscated.substring(0, dollarPos);
        return Long.parseLong(new String(HexUtils.decodeBase64(saltString), "UTF-8"));
    }

    public File getMasterPasswordFile() {
        if (masterPasswordFile != null)
            return masterPasswordFile;

        String path = SyspropUtil.getProperty(PROP_MASTER_PASSWORD_PATH);
        if ( path == null ) {
            throw new IllegalStateException("Master password file was not specified and is not set using the system property '"+PROP_MASTER_PASSWORD_PATH+"'.");            
        }

        return new File(path);
    }

    public char[] findMasterPassword() {
        File filePath = getMasterPasswordFile();
        try {
            byte[] bytes = IOUtils.slurpFile(filePath);
            String obfuscated = new String(bytes);
            return unobfuscate(obfuscated).toCharArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + filePath + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Attempt to save a new master password to the Master Password file, overwriting any previous value.
     *
     * @param newMasterPassword the new master password to save.  Required.
     * @throws IOException if there is a problem saving the new value.
     */
    public void saveMasterPassword(char[] newMasterPassword) throws IOException {
        final String obfuscated = obfuscate(new String(newMasterPassword));
        FileUtils.saveFileSafely(getMasterPasswordFile().getPath(), new FileUtils.Saver() {
            public void doSave(FileOutputStream fos) throws IOException {
                fos.write(obfuscated.getBytes());
            }
        });
    }
}
