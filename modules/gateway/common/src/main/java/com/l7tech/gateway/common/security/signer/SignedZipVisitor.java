package com.l7tech.gateway.common.security.signer;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * A visitor of the signed zip file entries.
 * An implementation of this interface is provided to the
 * {@link com.l7tech.gateway.common.security.signer.SignerUtils#walkSignedZip SignerUtils.walkSignedZip} methods to
 * visit each relevant zip-entry in a signed zip file tree.
 */
public interface SignedZipVisitor<Data, SigProps> {
    /**
     * Invoked for the signed data zip-entry in the signed zip file.
     *
     * @param inputStream    the signed zip-entry data stream.  Required and cannot be {@code null}.
     * @throws IOException if an error happens while reading the stream
     */
    Data visitData(@NotNull InputStream inputStream) throws IOException;

    /**
     * Invoked for the signature properties zip-entry in the signed zip file.
     *
     * @param inputStream    the signature properties stream.  Required and cannot be {@code null}.
     * @throws IOException if an error happens while reading the stream
     */
    SigProps visitSignature(@NotNull InputStream inputStream) throws IOException;
}
