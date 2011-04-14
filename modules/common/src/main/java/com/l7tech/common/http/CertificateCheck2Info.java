package com.l7tech.common.http;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.password.Sha512Crypt;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.protocol.SecureSpanConstants.HttpHeaders.CERT_CHECK2_PREFIX;

/**
 * Represents a digest that can be used to check if a certificate is valid.
 */
public class CertificateCheck2Info {
    private static final Logger logger = Logger.getLogger(CertificateCheck2Info.class.getName());
    private static final int CHECK2_PREFIX_LENGTH = CERT_CHECK2_PREFIX.length();
    private static final String CERT_CHECK2_PREFIX_LOWERCASE = CERT_CHECK2_PREFIX.toLowerCase();

    private final String idProviderOid;
    private final String serverNonceHex;
    private final String checkHashHex;
    private final String userSaltHex;

    public CertificateCheck2Info(String idProviderOid, String serverNonceHex, String checkHashHex, String userSaltHex) {
        this.idProviderOid = idProviderOid;
        this.serverNonceHex = serverNonceHex;
        this.checkHashHex = checkHashHex;
        this.userSaltHex = userSaltHex;
    }

    public static CertificateCheck2Info parseHttpHeader(HttpHeader checkInfoHeader) {
        String headerName = checkInfoHeader.getName();
        String headerValue = checkInfoHeader.getFullValue();
        if (headerName == null || !headerName.toLowerCase().startsWith(CERT_CHECK2_PREFIX_LOWERCASE))
            return null;

        String idProviderOid = headerName.substring(CHECK2_PREFIX_LENGTH);

        String[] vals = headerValue.split("\\s*;\\s*");
        if (vals.length != 3) {
            logger.fine("Ignoring cert Check2 header that does not contain three semicolon-delimited terms");
            return null;
        }

        // SERVERNONCEHEX; CHECKHEX; SALTHEX
        String serverNonceHex = vals[0].trim();
        String checkHashHex = vals[1].trim();
        String userSaltHex = vals[2].trim();

        return new CertificateCheck2Info(idProviderOid, serverNonceHex, checkHashHex, userSaltHex);
    }

    /**
     * Encode this CertificateCheckInfo as an HTTP header.
     *
     * @return this CertificateCheckInfo encoded as an HTTP header.
     */
    public HttpHeader asHttpHeader() {
        final String name = CERT_CHECK2_PREFIX + idProviderOid.trim();
        final String value = serverNonceHex + "; " + checkHashHex + "; " + userSaltHex;
        return new GenericHttpHeader(name, value);
    }

    /**
     * Check the specified certificate against this CertificateCheckInfo.
     *
     * @param certBytes  encoded certificate bytes.  required.
     * @param password   password.  required.
     * @param clientNonce      client nonce.  required.
     * @return true if the specified certificate bytes hashed with the current oid and realm and with the
     *              specified username, password and nonce results in a digest identical to the current digest.
     */
    public boolean checkCert(byte[] certBytes, char[] password, byte[] clientNonce) {
        if (isNoPass())
            return false; // can't check "NOPASS" check headers.

        try {
            byte[] userSalt = HexUtils.unHexDump(userSaltHex);
            String hashedPass = Sha512Crypt.crypt(MessageDigest.getInstance("SHA-512"), MessageDigest.getInstance("SHA-512"), new String(password).getBytes(Charsets.UTF8), new String(userSalt, Charsets.UTF8));
            byte[] serverNonce = HexUtils.unHexDump(serverNonceHex);
            byte[] expectedVerifier = CertUtils.getVerifierBytes(hashedPass.getBytes(Charsets.UTF8), clientNonce, serverNonce, certBytes);
            byte[] receivedVerifier = HexUtils.unHexDump(checkHashHex);
            return Arrays.equals(expectedVerifier, receivedVerifier);

        } catch (IOException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Unable to decode cert Check2 verifier from server: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        } catch (NoSuchAlgorithmException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.SEVERE, "Unable to decode cert Check2 verifier from server: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        }
    }

    /**
     * Check if this check header's digest could not be computed by the originator because they did not have access
     * to the password.
     *
     * @return true if the current digest is NOPASS.
     */
    public boolean isNoPass() {
        return SecureSpanConstants.NOPASS.equals(checkHashHex);
    }
}

