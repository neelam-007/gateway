package com.l7tech.common.io;

import com.l7tech.util.ArrayUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class to provide SSL socket utility methods, like filtering out unsupported protocols and ciphers
 */
public class SslSocketUtil {

    static final String TLS_EMPTY_RENEGOTIATION_INFO_SCSV = "TLS_EMPTY_RENEGOTIATION_INFO_SCSV";
    static final String NO_TLS_VERSION_SUPPORTED_ERROR = "None of the specified TLS versions are supported by the underlying TLS provider";
    static final String NO_CIPHER_SUPPORTED_ERROR = "None of the specified TLS ciphers are supported by the underlying TLS provider";

    /**
     * Removes protocols not contained inside {@code supportedTlsVersions}, or in other words returns an
     * intersection between {@code desiredTlsVersions} and {@code supportedTlsVersions}, or throws {@code UnsupportedTlsVersionsException}
     * when none of the desired protocols are contained inside {@code supportedTlsVersions}.
     *
     * @param desiredTlsVersions      Array of desired protocols.  Required and cannot be {@code null}.
     * @param supportedTlsVersions    Array of supported protocols.  Required and cannot be {@code null}.
     * @return an array containing only supported protocols, never {@code null}.
     * @throws UnsupportedTlsVersionsException when none of the desired protocols are supported i.e. are in the {@code supportedTlsVersions}.
     */
    @NotNull
    public static String[] filterUnsupportedTlsVersions(
            @NotNull final String[] desiredTlsVersions,
            @NotNull final String[] supportedTlsVersions
    ) throws UnsupportedTlsVersionsException {
        final String[] tlsVersions = ArrayUtils.intersection(desiredTlsVersions, supportedTlsVersions);
        // if desired protocols are empty then ALLOW it and let the underlying TLS provider decide what to do
        if (desiredTlsVersions.length > 0 && tlsVersions.length == 0) {
            throw new UnsupportedTlsVersionsException(NO_TLS_VERSION_SUPPORTED_ERROR);
        }
        return tlsVersions;
    }

    /**
     * Removes ciphers not contained inside {@code supportedCiphers}, or in other words returns an
     * intersection between {@code desiredCiphers} and {@code supportedCiphers}, or throws {@code UnsupportedTlsCiphersException}
     * when none of the desired protocols are contained inside {@code supportedCiphers}.
     *
     * @param desiredCiphers      Array of desired cipher suites.  Required and cannot be {@code null}.
     * @param supportedCiphers    Array of supported cipher suites.  Required and cannot be {@code null}.
     * @return an array containing only supported ciphers, never {@code null}.
     * @throws UnsupportedTlsCiphersException when none of the desired ciphers are supported i.e. are in the {@code supportedTlsVersions}
     * or when the only supported cipher after intersection is {@link #TLS_EMPTY_RENEGOTIATION_INFO_SCSV}
     */
    @NotNull
    public static String[] filterUnsupportedCiphers(
            @NotNull final String[] desiredCiphers,
            @NotNull final String[] supportedCiphers
    ) throws UnsupportedTlsCiphersException {
        final String[] tlsCipherSuites = ArrayUtils.intersection(desiredCiphers, supportedCiphers);
        if (
                // if desired ciphers are empty then ALLOW it and let the underlying TLS provider decide what to do
                (desiredCiphers.length > 0 && tlsCipherSuites.length == 0) ||
                (
                        // if desired ciphers contain only TLS_EMPTY_RENEGOTIATION_INFO_SCSV then ALLOW it and let the underlying TLS provider decide what to do
                        !(desiredCiphers.length == 1 && TLS_EMPTY_RENEGOTIATION_INFO_SCSV.equals(desiredCiphers[0])) &&
                        // otherwise if the intersection produces only TLS_EMPTY_RENEGOTIATION_INFO_SCSV then FAIL
                        tlsCipherSuites.length == 1 && TLS_EMPTY_RENEGOTIATION_INFO_SCSV.equals(tlsCipherSuites[0])
                )
        ) {
            throw new UnsupportedTlsCiphersException(NO_CIPHER_SUPPORTED_ERROR);
        }
        return tlsCipherSuites;
    }
}
