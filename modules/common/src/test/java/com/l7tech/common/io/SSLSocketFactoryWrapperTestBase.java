package com.l7tech.common.io;

import com.l7tech.test.BugId;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

/**
 */
public abstract class SSLSocketFactoryWrapperTestBase {

    static final String[] SUPPORTED_CIPHERS = new String[] {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV
    };

    static final String[] SUPPORTED_PROTOCOLS = new String[] {
            "TLSv1.0",
            "TLSv1.1",
            "TLSv1.2"
    };

    @BugId("DE274604")
    @Test
    public void testForUnsupportedProtocolsAndCiphers() throws Exception {

        doTestForUnsupportedProtocolsAndCiphers(
                SUPPORTED_PROTOCOLS, Either.left(SUPPORTED_PROTOCOLS),
                SUPPORTED_CIPHERS, Either.left(SUPPORTED_CIPHERS)
        );

        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{SUPPORTED_PROTOCOLS[0]}, Either.left(new String[] {SUPPORTED_PROTOCOLS[0]}),
                new String[]{SUPPORTED_CIPHERS[0]}, Either.left(new String[] {SUPPORTED_CIPHERS[0]})
        );

        doTestForUnsupportedProtocolsAndCiphers(
                ArrayUtils.union(SUPPORTED_PROTOCOLS, new String[]{"UNSUPPORTED_PROTOCOL1"}), Either.left(SUPPORTED_PROTOCOLS),
                ArrayUtils.union(SUPPORTED_CIPHERS, new String[]{"UNSUPPORTED_CIPHER1"}), Either.left(SUPPORTED_CIPHERS)
        );

        doTestForUnsupportedProtocolsAndCiphers(
                ArrayUtils.union(new String[]{"UNSUPPORTED_PROTOCOL1"}, SUPPORTED_PROTOCOLS), Either.left(SUPPORTED_PROTOCOLS),
                ArrayUtils.union(new String[]{"UNSUPPORTED_CIPHER1"}, SUPPORTED_CIPHERS), Either.left(SUPPORTED_CIPHERS)
        );

        doTestForUnsupportedProtocolsAndCiphers(
                ArrayUtils.union(new String[]{"UNSUPPORTED_PROTOCOL1"}, ArrayUtils.union(SUPPORTED_PROTOCOLS, new String[]{"UNSUPPORTED_PROTOCOL2"})), Either.left(SUPPORTED_PROTOCOLS),
                ArrayUtils.union(new String[]{"UNSUPPORTED_CIPHER1"}, ArrayUtils.union(SUPPORTED_CIPHERS, new String[]{"UNSUPPORTED_CIPHER2"})), Either.left(SUPPORTED_CIPHERS)
        );

        // edge case if the desired cipher suites contains only TLS_EMPTY_RENEGOTIATION_INFO_SCSV then allow it as this is exactly what was requested
        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{SUPPORTED_PROTOCOLS[0]}, Either.left(new String[] {SUPPORTED_PROTOCOLS[0]}),
                new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV}, Either.left(new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV})
        );

        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{SUPPORTED_PROTOCOLS[0]}, Either.left(new String[] {SUPPORTED_PROTOCOLS[0]}),
                new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV, "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"}, Either.left(new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV, "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"})
        );

        // edge case if the filter results only with TLS_EMPTY_RENEGOTIATION_INFO_SCSV, but desired ciphers contain more, then fail
        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{SUPPORTED_PROTOCOLS[0]}, Either.left(new String[] {SUPPORTED_PROTOCOLS[0]}),
                new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV, "UNSUPPORTED_CIPHER1"}, Either.right(Pair.pair(UnsupportedTlsCiphersException.class, SslSocketUtil.NO_CIPHER_SUPPORTED_ERROR))
        );

        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{SUPPORTED_PROTOCOLS[0]}, Either.left(new String[] {SUPPORTED_PROTOCOLS[0]}),
                new String[]{"UNSUPPORTED_CIPHER1", "UNSUPPORTED_CIPHER2"}, Either.right(Pair.pair(UnsupportedTlsCiphersException.class, SslSocketUtil.NO_CIPHER_SUPPORTED_ERROR))
        );

        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{"UNSUPPORTED_PROTOCOL1", "UNSUPPORTED_PROTOCOL2"}, Either.right(Pair.pair(UnsupportedTlsVersionsException.class, SslSocketUtil.NO_TLS_VERSION_SUPPORTED_ERROR)),
                new String[]{SUPPORTED_CIPHERS[0]}, Either.left(new String[] {SUPPORTED_CIPHERS[0]})
        );

        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{"UNSUPPORTED_PROTOCOL1", "UNSUPPORTED_PROTOCOL2"}, Either.right(Pair.pair(UnsupportedTlsVersionsException.class, SslSocketUtil.NO_TLS_VERSION_SUPPORTED_ERROR)),
                new String[]{"UNSUPPORTED_CIPHER1", "UNSUPPORTED_CIPHER2"}, Either.right(Pair.pair(UnsupportedTlsCiphersException.class, SslSocketUtil.NO_CIPHER_SUPPORTED_ERROR))
        );

        // edge case if the desired tls protocols are empty, then allow it and let the underlying TLS provider decide what to do, rather than us handling or failing
        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{}, Either.left(new String[]{}),
                new String[]{SUPPORTED_CIPHERS[0]}, Either.left(new String[] {SUPPORTED_CIPHERS[0]})
        );

        // edge case if the desired ciphers are empty, then allow it and let the underlying TLS provider decide what to do, rather than us handling or failing
        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{SUPPORTED_PROTOCOLS[0]}, Either.left(new String[] {SUPPORTED_PROTOCOLS[0]}),
                new String[]{}, Either.left(new String[]{})
        );

        // edge case if both desired protocols and ciphers are empty, then allow it and let the underlying TLS provider decide what to do, rather than us handling or failing
        doTestForUnsupportedProtocolsAndCiphers(
                new String[]{}, Either.left(new String[]{}),
                new String[]{}, Either.left(new String[]{})
        );
    }

    protected abstract void doTestForUnsupportedProtocolsAndCiphers(
            final String[] desiredProtocols,
            final Either<String[], Pair<Class<? extends Exception>, String>> expectedProtocolsOrException,
            final String[] desiredCiphers,
            final Either<String[], Pair<Class<? extends Exception>, String>> expectedCiphersOrException
    ) throws Exception;

    static String[] extractArgument(final InvocationOnMock invocation) throws Exception {
        Assert.assertNotNull(invocation);
        Assert.assertThat(invocation.getArguments().length, Matchers.is(1));
        Assert.assertThat(invocation.getArguments()[0], Matchers.instanceOf(String[].class));
        final String[] arg = (String[])invocation.getArguments()[0];
        Assert.assertNotNull(arg);
        return arg;
    }
}
