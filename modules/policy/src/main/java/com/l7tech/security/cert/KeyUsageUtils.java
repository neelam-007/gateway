package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Key usage utility methods
 */
public class KeyUsageUtils {

    //- PUBLIC

    /**
     * Verify a certificate is valid for use as an SSL/TLS server certificate.
     *
     * <p>Currently this only checks RSA certificates for the the requried
     * key usage.</p>
     *
     * @param cert The certificate to check.
     * @return True if the certificate is acceptable for SSL/TLS usage
     */
    public static boolean isCertSslCapable( final X509Certificate cert ) {
        if ( cert.getPublicKey() != null && "RSA".equals( cert.getPublicKey().getAlgorithm() ) ) {
            try {
                return new KeyUsageChecker(makeRsaSslServerKeyUsagePolicy(), KeyUsageChecker.ENFORCEMENT_MODE_ALWAYS)
                        .permitsActivity(KeyUsageActivity.sslServerRemote, cert);
            } catch (CertificateParsingException e) {
                logger.log( Level.WARNING, "Unable to check SSL capability for certificate: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException(e));
                return false;
            }
        } else {
            return true;
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( KeyUsageUtils.class.getName() );

    private static KeyUsagePolicy makeRsaSslServerKeyUsagePolicy() {
        final Map<KeyUsageActivity, List<KeyUsagePermitRule>> kuPermits = new HashMap<KeyUsageActivity, List<KeyUsagePermitRule>>();
        final KeyUsagePermitRule permitRule = new KeyUsagePermitRule(
                KeyUsageActivity.sslServerRemote,
                CertUtils.KEY_USAGE_BITS_BY_NAME.get("keyEncipherment"));
        kuPermits.put(KeyUsageActivity.sslServerRemote, Collections.singletonList( permitRule ));
        return KeyUsagePolicy.fromRules(null, kuPermits, null);
    }
}
