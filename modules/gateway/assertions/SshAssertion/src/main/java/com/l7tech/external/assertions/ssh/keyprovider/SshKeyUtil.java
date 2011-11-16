package com.l7tech.external.assertions.ssh.keyprovider;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import static com.l7tech.util.TextUtils.matches;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * SSH key utility class.
 */
public class SshKeyUtil {
    public static final String PEM = "PEM";

    private static final Logger LOG = Logger.getLogger(SshKeyUtil.class.getName());
    private static final String ALGORITHM_DSA = "DSA";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String PEM_BEGIN = "-----BEGIN ";
    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile(
            "^[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:" +
            "[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:" +
            "[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:" +
            "[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]$");

    /**
     * Determine algorithm of a given private key.
     * @param pemPrivateKey a private key string in PEM format
     * @return an algorithm string for RSA or DSA
     */
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
     * @return a String for the provider name
     */
    public static String getSymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getProviderNameForService( "Cipher.AES" );
    }

    /**
     * Get the provider name for RSA service.
     * Note: Cipher.getInstance("RSA").getProvider().getName() throws error when using Bouncy Castle's PEMReader
     * @return a String for the provider name
     */
    public static String getAsymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getProviderNameForService( "Cipher." + optional(JceProvider.getInstance().getRsaNoPaddingCipherName()).orSome( "RSA/NONE/NoPadding" ) );
    }

    public static KeyPair doReadKeyPair(String privateKey) throws Exception {
        InputStream is = new ByteArrayInputStream(privateKey.getBytes());
        PEMReader r = new PEMReader(new InputStreamReader(is), null, getSymProvider(), getAsymProvider());
        return (KeyPair) r.readObject();
    }

    /**
     * Writes key in a PEM format but without any linebreaks.
     *
     * @param publicKey The key to write
     * @return The PEMish text or null
     */
    public static String writeKey(PublicKey publicKey) {
        OutputStream os = new ByteArrayOutputStream();
        try {
            PEMWriter w = new PEMWriter(new OutputStreamWriter(os));
            w.writeObject(publicKey);
            w.flush();
            return os.toString().replace( SyspropUtil.getProperty( "line.separator" ), "");
        } catch (Exception e) {
            LOG.log(Level.INFO, "Unable to write key: ", ExceptionUtils.getDebugException(e));
        } finally {
            ResourceUtils.closeQuietly(os);
        }
        return null;
    }

    /**
     * Validate the format of an SSH public key fingerprint
     *
     * @param fingerPrint SSH public key fingerprint format string
     * @return Returns an optional validation error message
     */
    public static Option<String> validateSshPublicKeyFingerprint( final String fingerPrint ) {
        final Option<String> message;

        // fingerprint must match or contain context variables
        if ( !optional(fingerPrint).exists(matches(FINGERPRINT_PATTERN)) &&
                Syntax.getReferencedNames(fingerPrint).length == 0 ) {
            message = some( "The SSH public key fingerprint entered is not valid." );
        } else {
            message = none();
        }

        return message;
    }

    private static String getProviderNameForService( final String service ) {
        final Provider provider = JceProvider.getInstance().getPreferredProvider( service );
        return provider == null ? null : provider.getName();
    }
}
