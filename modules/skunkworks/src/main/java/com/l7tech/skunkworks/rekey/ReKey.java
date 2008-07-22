package com.l7tech.skunkworks.rekey;

import com.l7tech.util.HexUtils;
import com.l7tech.util.EncryptionUtil;
import com.l7tech.util.CausedIOException;
import com.l7tech.common.io.IOUtils;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.MasterPasswordManager;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.PrivateKey;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;

/**
 * Utility to replace SSL cert with a cartel version.
 *
 * To build the ReKey utility:
 * ./build.sh compile-tests
 * pushd build/test-classes; jar -cmf ../../tests/com/l7tech/skunkworks/rekey/ReKey.mf ../../ReKey.jar $(ls com/l7tech/skunkworks/rekey/*); popd
 *
 * To run, you need to create an input file:
 *
 *   mysql ssg -p -N -B -e "select encodingid, replace(b64edval,'\n', '') from shared_keys" > keys_in.txt
 *
 * This util will write a new ssl.cer and ssl.ks to /tmp along with an ssl.sql script that needs to be
 * run to update the database.
 *
 * @author steve
 */
public class ReKey {

    //- PUBLIC

    public static void main (final String[] args) throws Exception {
        if ( args.length != 6 ) {
            System.out.println( "Usage:\n\tReKey <dbkeys.txt> <ssl.ks> <sslpwd> <sslalias> <sslkeypwd> <partition-dir>" );
        } else {
            Logger.getLogger("").setLevel( Level.WARNING );

            String dbKeysFile = args[0];
            String sskKeystoreFile = args[1];
            String sslPassphrase = args[2];
            String sslAlias = args[3];
            String sslKeyPassphrase = args[4];
            String partitionDirectory = args[5];

            File partitionDir = new File(partitionDirectory);
            File mpFile = new File(partitionDir, MASTER_PASSPHRASE_FILE);
            System.out.println("Using master passphrase from '"+mpFile.getAbsolutePath()+"'.");
            String obfuscatedPwd = readPassword(mpFile);
            String passphrase = unobfuscate(obfuscatedPwd);

            System.setProperty( "com.l7tech.server.partitionName", partitionDir.getName() );
            MasterPasswordManager passwordManager = new MasterPasswordManager(passphrase.toCharArray());
            KeystoreUtils utils = new KeystoreUtils( ServerConfig.getInstance(), passwordManager);
            X509Certificate cert = utils.getSslCert();
            PrivateKey key = utils.getSSLPrivateKey();
            System.out.println( "Current SSL key/cert: " + cert.getSubjectDN().getName() + ", issued by: " + cert.getIssuerDN().getName());

            String[] sharedKeys = new String( IOUtils.slurpFile( new File(dbKeysFile) )).split( "\n" );
            System.out.println( "Loaded " + sharedKeys.length + " shared keys from file.");
            String calcid = EncryptionUtil.computeCustomRSAPubKeyID((RSAPublicKey)(cert.getPublicKey()));

            System.out.println( "Calculated identifier for current SSL certificate: " + calcid );
            String valueb64 = null;
            for ( String keyLine : sharedKeys ) {
                if ( keyLine.startsWith( calcid ) ) {
                    valueb64 = keyLine.split( "\t", 2 )[1].trim();
                }
            }
            System.out.println( "Found encrypted shared key for current SSL key: " + valueb64 );

            byte[] decrypted = EncryptionUtil.deB64AndRsaDecrypt(valueb64, key);
            Object[] keyAndCert = readPkcs12( new File(sskKeystoreFile), sslPassphrase.toCharArray(), sslAlias, sslKeyPassphrase.toCharArray());
            PrivateKey nuKey = (PrivateKey) keyAndCert[0];
            X509Certificate nuCert = (X509Certificate) keyAndCert[1];
            System.out.println( "New SSL key/cert: " + nuCert.getSubjectDN().getName() + ", issued by: " + nuCert.getIssuerDN().getName());

            String nuFingerPrint = EncryptionUtil.computeCustomRSAPubKeyID( (RSAPublicKey)nuCert.getPublicKey() );
            String nuEncyptedKey = HexUtils.encodeBase64( HexUtils.decodeBase64( EncryptionUtil.rsaEncAndB64( decrypted, nuCert.getPublicKey() )), true);

            String dbupdate = "INSERT INTO shared_keys (encodingid, b64edval) values ('"+nuFingerPrint+"', '"+nuEncyptedKey+"') ON DUPLICATE KEY UPDATE b64edval='"+nuEncyptedKey+"';\n";
            FileOutputStream sout = new FileOutputStream("/tmp/ssl.sql");
            sout.write( dbupdate.getBytes() );
            sout.flush();
            sout.close();
            System.out.println( "Created DB update script: /tmp/ssl.sql" );

            KeyStore nuKeyStore = KeyStore.getInstance("PKCS12");
            nuKeyStore.load( null, null );
            char[] nuKsPass = passwordManager.decryptPasswordIfEncrypted(utils.getSslKeystorePasswd());
            nuKeyStore.setKeyEntry( utils.getSSLAlias(), nuKey, nuKsPass, new Certificate[]{nuCert} );
            nuKeyStore.store( new FileOutputStream("/tmp/ssl.ks"), nuKsPass );
            System.out.println( "Created keystore        : /tmp/ssl.ks" );

            FileOutputStream cout = new FileOutputStream("/tmp/ssl.cer");
            cout.write( nuCert.getEncoded() );
            cout.flush();
            cout.close();
            System.out.println( "Created certificate     : /tmp/ssl.cer" );
        }
    }

    //- PRIVATE

    private static final String MASTER_PASSPHRASE_FILE = "omp.dat";
    private static final String OBFUSCATION_PREFIX = "$L7O$"; // do not change this, for backward compat.  can add new schemes, though
    private static final long OBFUSCATION_SEED = 171717L; // do not change this, for backward compat.  can add new schemes, though

    private static String readPassword( final File file ) throws IOException {
        return new String(IOUtils.slurpFile( file ));
    }

    private static Object[] readPkcs12(final File file, final char[] password, String alias, final char[] keyPassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        keyStore.load(new FileInputStream(file), password);

        List aliases = Collections.list(keyStore.aliases());
        if (aliases.isEmpty())
            throw new CausedIOException("PKCS12 has no entries (must be at least 1)");

        if (aliases.size() == 1) { // then ignore the passed in alias
            alias = (String) aliases.get( 0 );
        }

        return new Object[]{keyStore.getKey(alias, keyPassword), keyStore.getCertificate(alias)};        
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
        return Long.parseLong(new String( HexUtils.decodeBase64(saltString), "UTF-8"));
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
}
