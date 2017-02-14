package com.l7tech.common.io;

import com.l7tech.test.util.TestUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SSLSocketWrapperTest extends SSLSocketFactoryWrapperTestBase {

    @Mock
    SSLSocketFactory socketFactory;
    @Mock
    SSLSocket sslSocket;

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLSocket.class, SSLSocketWrapper.class );
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void doTestForUnsupportedProtocolsAndCiphers(
            final String[] desiredProtocols,
            final Either<String[], Pair<Class<? extends Exception>, String>> expectedProtocolsOrException,
            final String[] desiredCiphers,
            final Either<String[], Pair<Class<? extends Exception>, String>> expectedCiphersOrException
    ) throws Exception {
        Assert.assertNotNull(desiredProtocols);
        Assert.assertNotNull(expectedProtocolsOrException);
        Assert.assertNotNull(desiredCiphers);
        Assert.assertNotNull(expectedCiphersOrException);

        Mockito.doReturn(sslSocket).when(socketFactory).createSocket();
        Mockito.doReturn(SUPPORTED_PROTOCOLS).when(sslSocket).getSupportedProtocols();
        Mockito.doReturn(SUPPORTED_CIPHERS).when(sslSocket).getSupportedCipherSuites();

        final SSLSocketFactoryWrapper wrapper = SSLSocketFactoryWrapper.wrapAndSetTlsVersionAndCipherSuites(socketFactory, desiredProtocols, desiredCiphers);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final String[] ciphers = extractArgument(invocation);
                Assert.assertTrue(expectedCiphersOrException.isLeft());
                Assert.assertArrayEquals(ciphers, expectedCiphersOrException.left());
                return null;
            }
        }).when(sslSocket).setEnabledCipherSuites(Mockito.any(String[].class));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final String[] protocols = extractArgument(invocation);
                Assert.assertTrue(expectedProtocolsOrException.isLeft());
                Assert.assertArrayEquals(protocols, expectedProtocolsOrException.left());
                return null;
            }
        }).when(sslSocket).setEnabledProtocols(Mockito.any(String[].class));

        if (expectedCiphersOrException.isRight() || expectedProtocolsOrException.isRight()) {
            try {
                wrapper.createSocket();
                Assert.fail("createSocket should have failed");
            } catch (final Exception e) {
                if (expectedCiphersOrException.isRight() && expectedProtocolsOrException.isRight()) {
                    Assert.assertThat(e, Matchers.either(Matchers.instanceOf(expectedCiphersOrException.right().left)).or(Matchers.instanceOf(expectedProtocolsOrException.right().left)));
                    Assert.assertThat(e.getMessage(), Matchers.either(Matchers.equalTo(expectedCiphersOrException.right().right)).or(Matchers.equalTo(expectedProtocolsOrException.right().right)));
                } else if (expectedCiphersOrException.isRight()) {
                    Assert.assertThat(e, Matchers.instanceOf(expectedCiphersOrException.right().left));
                    Assert.assertThat(e.getMessage(), Matchers.equalTo(expectedCiphersOrException.right().right));
                } else if (expectedProtocolsOrException.isRight()) {
                    Assert.assertThat(e, Matchers.instanceOf(expectedProtocolsOrException.right().left));
                    Assert.assertThat(e.getMessage(), Matchers.equalTo(expectedProtocolsOrException.right().right));
                } else {
                    Assert.fail("Invalid state");
                }
            }
        } else {
            wrapper.createSocket();
        }

    }
}
