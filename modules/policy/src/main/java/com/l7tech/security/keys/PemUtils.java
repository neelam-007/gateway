package com.l7tech.security.keys;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import static com.l7tech.util.Option.optional;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for working with PEM keys.
 *
 * <p>Use of these utility methods may cause a JceProvider to be initialized.</p>
 */
public class PemUtils {
    private static final Logger LOG = Logger.getLogger(PemUtils.class.getName());

    private static final String ALGORITHM_DSA = "DSA";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String PEM_BEGIN = "-----BEGIN ";

    /**
     * Determine algorithm of a given private key.
     *
     * @param pemPrivateKey a private key string in PEM format (must not be null)
     * @return an algorithm string for RSA or DSA or null if unknown
     */
    @Nullable
    public static String getPemPrivateKeyAlgorithm(String pemPrivateKey) {
        if ( pemPrivateKey.contains( PEM_BEGIN + ALGORITHM_RSA ) ) {
            return ALGORITHM_RSA;
        } else if ( pemPrivateKey.contains( PEM_BEGIN + ALGORITHM_DSA ) ) {
            return ALGORITHM_DSA;
        } else {
            return null;
        }
    }

    /**
     * Get the provider name for AES service.
     *
     * @return a String for the provider name
     */
    @Nullable
    public static String getSymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getProviderNameForService( "Cipher.AES" );
    }

    /**
     * Get the provider name for RSA service.
     * Note: Cipher.getInstance("RSA").getProvider().getName() throws error when using Bouncy Castle's PEMReader
     * @return a String for the provider name
     */
    @Nullable
    public static String getAsymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getProviderNameForService( "Cipher." + optional( JceProvider.getInstance().getRsaNoPaddingCipherName()).orSome( "RSA/NONE/NoPadding" ) );
    }

    /**
     * Read a key pair from a PEM format.
     *
     * @param privateKey The key to read (required)
     * @return The key pair
     * @throws Exception If an error occurs
     */
    @NotNull
    public static KeyPair doReadKeyPair( final String privateKey ) throws Exception {
        InputStream is = new ByteArrayInputStream(privateKey.getBytes());
        PEMReader r = new PEMReader(new InputStreamReader(is), null, getSymProvider(), getAsymProvider());
        return (KeyPair) r.readObject();
    }

    /**
     * Write the given private key as PEM.
     *
     * <p>Note that the output is a PEM private key but the corresponding
     * <code>doReadKeyPair</code> method should be used to read the output.</p>
     *
     * @param privateKey The private key to write (required)
     * @return The PEM private key
     * @throws Exception If an error occurs
     */
    @NotNull
    public static String doWriteKeyPair( final PrivateKey privateKey ) throws Exception {
        final StringWriter writer = new StringWriter();
        final PEMWriter pemWriter = new PEMWriter( writer, getAsymProvider());
        pemWriter.writeObject( privateKey );
        pemWriter.close();
        return writer.toString();
    }

    /**
     * Writes key in a PEM format with or without linebreaks.
     *
     * @param publicKey The key to write (required)
     * @param newlines True to include newlines
     * @return The PEMish text or null
     */
    @Nullable
    public static String writeKey( final PublicKey publicKey, final boolean newlines ) {
        OutputStream os = new ByteArrayOutputStream();
        try {
            PEMWriter w = new PEMWriter(new OutputStreamWriter(os));
            w.writeObject(publicKey);
            w.flush();
            String pemKey = os.toString();
            if ( !newlines ) {
                pemKey = pemKey.replace( SyspropUtil.getProperty( "line.separator" ), "");
            }
            return pemKey;
        } catch (Exception e) {
            LOG.log( Level.INFO, "Unable to write key: ", ExceptionUtils.getDebugException( e ));
        } finally {
            ResourceUtils.closeQuietly( os );
        }
        return null;
    }

    /**
     * Writes key in a PEM format but without any linebreaks.
     *
     * @param publicKey The key to write (required)
     * @return The PEMish text or null
     */
    @Nullable
    public static String writeKey( final PublicKey publicKey ) {
        return writeKey( publicKey, false );
    }

    @Nullable
    public static String getProviderNameForService( final String service ) {
        final Provider provider = JceProvider.getInstance().getPreferredProvider( service );
        return provider == null ? null : provider.getName();
    }

    /**
     * Determine if the PEM private key passed in is encrypted:
     *
     * @param pem The key to write (required)
     * @return boolean
     */
    public static boolean isEncryptedPem(String pem) {
        final Pattern pattern = Pattern.compile(".*\nProc-Type:.*ENCRYPTED.*",Pattern.DOTALL|Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(pem);
        return matcher.matches();
    }
}
