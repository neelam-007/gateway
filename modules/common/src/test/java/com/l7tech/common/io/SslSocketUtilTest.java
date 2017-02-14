package com.l7tech.common.io;

import com.l7tech.test.BugId;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test {@link SslSocketUtil}
 */
@SuppressWarnings("Duplicates")
public class SslSocketUtilTest {

    private static final String[] SUPPORTED_CIPHERS = new String[] {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV
    };

    private static final String[] SUPPORTED_PROTOCOLS = new String[] {
            "TLSv1.0",
            "TLSv1.1",
            "TLSv1.2"
    };

    @Before
    public void setUp() throws Exception {

    }

    @BugId("DE274604")
    @Test
    public void testFilterUnsupportedTlsVersions() throws Exception {

        doTestFilterUnsupportedTlsVersions(
                SUPPORTED_PROTOCOLS,
                Either.left(SUPPORTED_PROTOCOLS)
        );

        doTestFilterUnsupportedTlsVersions(
                new String[]{SUPPORTED_PROTOCOLS[0]},
                Either.left(new String[] {SUPPORTED_PROTOCOLS[0]})
        );

        doTestFilterUnsupportedTlsVersions(
                ArrayUtils.union(SUPPORTED_PROTOCOLS, new String[]{"UNSUPPORTED_PROTOCOL1"}),
                Either.left(SUPPORTED_PROTOCOLS)
        );

        doTestFilterUnsupportedTlsVersions(
                ArrayUtils.union(new String[]{"UNSUPPORTED_PROTOCOL1"}, SUPPORTED_PROTOCOLS),
                Either.left(SUPPORTED_PROTOCOLS)
        );

        doTestFilterUnsupportedTlsVersions(
                ArrayUtils.union(new String[]{"UNSUPPORTED_PROTOCOL1"}, ArrayUtils.union(SUPPORTED_PROTOCOLS, new String[]{"UNSUPPORTED_PROTOCOL2"})),
                Either.left(SUPPORTED_PROTOCOLS)
        );

        doTestFilterUnsupportedTlsVersions(
                new String[]{"UNSUPPORTED_PROTOCOL1", "UNSUPPORTED_PROTOCOL2"},
                Either.right(Pair.pair(UnsupportedTlsVersionsException.class, SslSocketUtil.NO_TLS_VERSION_SUPPORTED_ERROR))
        );

        // edge case if the desired tls protocols are empty, then allow it and let the underlying TLS provider decide what to do, rather than us handling or failing
        doTestFilterUnsupportedTlsVersions(
                new String[]{},
                Either.left(new String[]{})
        );
    }

    private void doTestFilterUnsupportedTlsVersions(
            final String[] desiredTlsVersion,
            final Either<String[], Pair<Class<? extends Exception>, String>> expectedTlsVersionOrException
    ) throws Exception {
        Assert.assertNotNull(desiredTlsVersion);
        Assert.assertNotNull(expectedTlsVersionOrException);
        Assert.assertTrue(expectedTlsVersionOrException.isLeft() || expectedTlsVersionOrException.isRight());

        if (expectedTlsVersionOrException.isLeft()) {
            Assert.assertArrayEquals(
                    SslSocketUtil.filterUnsupportedTlsVersions(desiredTlsVersion, SUPPORTED_PROTOCOLS),
                    expectedTlsVersionOrException.left()
            );
        } else {
            try {
                SslSocketUtil.filterUnsupportedTlsVersions(desiredTlsVersion, SUPPORTED_PROTOCOLS);
                Assert.fail("filterUnsupportedTlsVersions should have failed");
            } catch (final Exception e) {
                Assert.assertThat(e, Matchers.instanceOf(expectedTlsVersionOrException.right().left));
                Assert.assertThat(e.getMessage(), Matchers.equalTo(expectedTlsVersionOrException.right().right));
            }
        }
    }

    @BugId("DE274604")
    @Test
    public void testFilterUnsupportedCiphers() throws Exception {

        doTestFilterUnsupportedCiphers(
                SUPPORTED_CIPHERS,
                Either.left(SUPPORTED_CIPHERS)
        );

        doTestFilterUnsupportedCiphers(
                new String[]{SUPPORTED_CIPHERS[0]},
                Either.left(new String[] {SUPPORTED_CIPHERS[0]})
        );

        doTestFilterUnsupportedCiphers(
                ArrayUtils.union(SUPPORTED_CIPHERS, new String[]{"UNSUPPORTED_CIPHER1"}),
                Either.left(SUPPORTED_CIPHERS)
        );

        doTestFilterUnsupportedCiphers(
                ArrayUtils.union(new String[]{"UNSUPPORTED_CIPHER1"}, SUPPORTED_CIPHERS),
                Either.left(SUPPORTED_CIPHERS)
        );

        doTestFilterUnsupportedCiphers(
                ArrayUtils.union(new String[]{"UNSUPPORTED_CIPHER1"}, ArrayUtils.union(SUPPORTED_CIPHERS, new String[]{"UNSUPPORTED_CIPHER2"})),
                Either.left(SUPPORTED_CIPHERS)
        );

        // edge case if the desired cipher suites contains only TLS_EMPTY_RENEGOTIATION_INFO_SCSV then allow it as this is exactly what was requested
        doTestFilterUnsupportedCiphers(
                new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV},
                Either.left(new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV})
        );

        doTestFilterUnsupportedCiphers(
                new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV, "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"},
                Either.left(new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV, "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"})
        );

        // edge case if the filter results only with TLS_EMPTY_RENEGOTIATION_INFO_SCSV, but desired ciphers contain more, then fail
        doTestFilterUnsupportedCiphers(
                new String[]{SslSocketUtil.TLS_EMPTY_RENEGOTIATION_INFO_SCSV, "UNSUPPORTED_CIPHER1"},
                Either.right(Pair.pair(UnsupportedTlsCiphersException.class, SslSocketUtil.NO_CIPHER_SUPPORTED_ERROR))
        );

        doTestFilterUnsupportedCiphers(
                new String[]{"UNSUPPORTED_CIPHER1", "UNSUPPORTED_CIPHER2"},
                Either.right(Pair.pair(UnsupportedTlsCiphersException.class, SslSocketUtil.NO_CIPHER_SUPPORTED_ERROR))
        );

        // edge case if the desired ciphers are empty, then allow it and let the underlying TLS provider decide what to do, rather than us handling or failing
        doTestFilterUnsupportedCiphers(
                new String[]{},
                Either.left(new String[]{})
        );
    }

    private void doTestFilterUnsupportedCiphers(
            final String[] desiredCiphers,
            final Either<String[], Pair<Class<? extends Exception>, String>> expectedCiphersOrException
    ) throws Exception {
        Assert.assertNotNull(desiredCiphers);
        Assert.assertNotNull(expectedCiphersOrException);
        Assert.assertTrue(expectedCiphersOrException.isLeft() || expectedCiphersOrException.isRight());

        if (expectedCiphersOrException.isLeft()) {
            Assert.assertArrayEquals(
                    SslSocketUtil.filterUnsupportedCiphers(desiredCiphers, SUPPORTED_CIPHERS),
                    expectedCiphersOrException.left()
            );
        } else {
            try {
                SslSocketUtil.filterUnsupportedCiphers(desiredCiphers, SUPPORTED_CIPHERS);
                Assert.fail("filterUnsupportedTlsVersions should have failed");
            } catch (final Exception e) {
                Assert.assertThat(e, Matchers.instanceOf(expectedCiphersOrException.right().left));
                Assert.assertThat(e.getMessage(), Matchers.equalTo(expectedCiphersOrException.right().right));
            }
        }
    }

}