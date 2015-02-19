package com.l7tech.internal.omp;

import com.l7tech.server.util.PropertiesDecryptor;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ObfuscatedFileMasterPasswordFinder;

import java.util.Arrays;
import java.util.Properties;

/**
 * Tiny command line utility to decrypt the obfuscated master passphrase and (optionally) decrypt
 * any passwords within a property file (read from STDIN) that were encrypted with this master passphrase.
 *
 * Instructions:
 * <ul>
 * <li>(If needed) Compile this source with Gateway.jar in the compile time class path.
 * <li>Move OmpDecrypt.class to /opt/SecureSpan/Gateway/runtime
 * <li>The first command line argument is the obfuscated master passphrase, which looks similar to:
 *   <pre>$L7O$LTQ5NzIyMDIwMjgyMzgzNTI1ODI=$T8gt71DN</pre>
 * <li>The program will display the un-obfuscated master passphrase, then attempt to read a properties
 *     file from STDIN.  It will then print out the decrypted properties file.
 * </ul>
 * Typical use:
 * <pre>
 *   java -cp Gateway.jar:. OmpDecrypt `cat ../node/default/etc/conf/omp.dat` &lt; ../node/default/etc/conf/node.properties
 * </pre>
 */
public class OmpDecrypt {
    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            throw new IllegalArgumentException("Usage: java OmpDecrypt <omp>\n\nExample: java OmpDecrypt $L7O$LTQ5NzIyMDIwMjgyMzgzNTI1ODI=$T8gt71DN < node.properties");

        String omp = args[0];
        final byte[] masterPassphrase = ObfuscatedFileMasterPasswordFinder.unobfuscate(omp);
        System.out.println("Un-obfuscated master passphrase (as hex byte array): " + HexUtils.hexDump(masterPassphrase));
        System.out.println("Un-obfuscated master passphrase (interpreted as UTF-8 byte sequence): " + new String(masterPassphrase, Charsets.UTF8));

        System.out.println("Reading properties file to decrypt from STDIN");
        Properties properties = new Properties();
        properties.load(System.in);
        new PropertiesDecryptor(new MasterPasswordManager(new MasterPasswordManager.MasterPasswordFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return Arrays.copyOf( masterPassphrase, masterPassphrase.length );
            }
        })).decryptEncryptedPasswords(properties);

        properties.store(System.out, "Decrypted");
    }
}
