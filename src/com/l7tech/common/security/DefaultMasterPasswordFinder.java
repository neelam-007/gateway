package com.l7tech.common.security;

import com.l7tech.common.util.SyspropUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.FileUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Random;

/**
 * A MasterPasswordFinder that knows how to read an obfuscated master password from a config file.
 * This finder is uncached -- it will always open and read the config file whenever someone asks for
 * the master password.
 */
public class DefaultMasterPasswordFinder implements MasterPasswordFinder {
    public static final String PROP_MASTER_PASSWORD_PATH = "com.l7tech.masterPasswordPath";
    public static final String BASE_PATH = SyspropUtil.getString("com.l7tech.server.home", "/ssg");
    public static final String PROP_MASTER_PASSWORD_PATH_DEFAULT = new File(BASE_PATH, "etc/conf/omp.dat").getAbsolutePath();

    private static final String OBFUSCATION_PREFIX = "$L7O$"; // do not change this, for backward compat.  can add new schemes, though
    private static long OBFUSCATION_SEED = 171717L; // do not change this, for backward compat.  can add new schemes, though

    /**
     * Obfuscate a password.
     * This is a utility method provided for use by MasterPasswordFinder implementations.
     *
     * @param clear the cleartext string that is to be obfuscated.  Required.
     * @return the obfuscated form of the password.
     */
    public static String obfuscate(String clear) {
        try {
            long salt = new Random().nextLong();
            Random rand = new Random(OBFUSCATION_SEED + salt);
            byte[] in = clear.getBytes("UTF-8");
            byte[] out = new byte[in.length];
            for (int i = 0; i < in.length; i++) {
                byte b = in[i];
                b ^= rand.nextInt();
                out[i] = b;
            }
            byte[] saltBytes = String.valueOf(salt).getBytes("UTF-8");
            return OBFUSCATION_PREFIX + HexUtils.encodeBase64(saltBytes, true) + "$" + HexUtils.encodeBase64(out, true);
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
     * @throws java.io.IOException if the obfuscated password is not correctly formatted base-64 or does not result in valid utf-8
     */
    public static String unobfuscate(String obfuscated) throws IOException {
        if (!obfuscated.startsWith(OBFUSCATION_PREFIX) || obfuscated.length() <= OBFUSCATION_PREFIX.length())
            throw new IOException("Invalid obfuscated password");
        obfuscated = obfuscated.substring(OBFUSCATION_PREFIX.length());
        int dollarPos = obfuscated.indexOf('$');
        if (dollarPos < 1 || dollarPos >= obfuscated.length() - 1)
            throw new IOException("Invalid obfuscated password");
        String saltString = obfuscated.substring(0, dollarPos);
        long salt = Long.parseLong(new String(HexUtils.decodeBase64(saltString), "UTF-8"));
        obfuscated = obfuscated.substring(dollarPos + 1);
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

    public File getMasterPasswordFile() {
        return new File(SyspropUtil.getString(PROP_MASTER_PASSWORD_PATH, PROP_MASTER_PASSWORD_PATH_DEFAULT));        
    }

    public char[] findMasterPassword() {
        File filePath = getMasterPasswordFile();
        try {
            byte[] bytes = HexUtils.slurpFile(filePath);
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
