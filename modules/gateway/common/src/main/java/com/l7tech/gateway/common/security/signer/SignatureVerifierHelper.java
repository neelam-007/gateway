package com.l7tech.gateway.common.security.signer;

import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/**
 * Helper class for unwrapping signed data content if signature verification is successful
 */
public class SignatureVerifierHelper {

    @NotNull private final SignatureVerifier signatureVerifier;

    public SignatureVerifierHelper(@NotNull final SignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    @NotNull
    public SignerUtils.SignedZipContent verifyZip(@NotNull final File zipFile) throws SignatureException {
        try (final InputStream bis = new BufferedInputStream(new FileInputStream(zipFile))) {
            return verifyZip(bis);
        } catch (IOException e) {
            throw new SignatureException("Invalid signed Zip: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * The method will check if the stream (specified by {@code zipToVerify}) is signed and that the signer is trusted,
     * and then will unwrap the actual content.
     * <p/>
     * The caller is responsible for properly closing returned {@code SignedZipContent}.
     *
     * @param zipToVerify    input stream containing .ZIP file as produced by signZip.  Required and cannot be {@code null}.
     * @return a {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZipContent} object containing
     * signed data {@code InputStream}, calculated signed data digest and signature {@code InputStream}.  Never {@code null}.
     * @throws SignatureException if an error happens while reading signature properties, verifying signature or {@code zipToVerify} is not trusted.
     */
    @NotNull
    public SignerUtils.SignedZipContent verifyZip(@NotNull final InputStream zipToVerify) throws SignatureException {
        // read signed zip content
        final SignerUtils.SignedZipContent zipContent;
        try {
            zipContent = SignerUtils.readSignedZip(zipToVerify);
        } catch (final IOException e) {
            throw new SignatureException("Invalid signed Zip: " + ExceptionUtils.getMessage(e), e);
        } catch (final NoSuchAlgorithmException e) {
            throw new SignatureException("Failed to calculate digest" + ExceptionUtils.getMessage(e), e);
        }
        // verify signature
        this.signatureVerifier.verify(zipContent.getDataDigest(), zipContent.getSignaturePropertiesBytes());
        // finally return zipContent
        return zipContent;
    }
}
