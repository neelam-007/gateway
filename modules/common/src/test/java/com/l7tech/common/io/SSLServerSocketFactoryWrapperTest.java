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

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SSLServerSocketFactoryWrapperTest extends SSLSocketFactoryWrapperTestBase {

    @Mock
    SSLServerSocketFactory socketFactory;
    @Mock
    SSLServerSocket sslServerSocket;

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLServerSocketFactory.class, SSLServerSocketFactoryWrapper.class );
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

        Mockito.doReturn(sslServerSocket).when(socketFactory).createServerSocket();
        Mockito.doReturn(SUPPORTED_PROTOCOLS).when(sslServerSocket).getSupportedProtocols();
        Mockito.doReturn(SUPPORTED_CIPHERS).when(sslServerSocket).getSupportedCipherSuites();

        final SSLServerSocketFactoryWrapper wrapper = SSLServerSocketFactoryWrapper.wrapAndSetTlsVersionAndCipherSuites(socketFactory, desiredProtocols, desiredCiphers);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final String[] ciphers = extractArgument(invocation);
                Assert.assertTrue(expectedCiphersOrException.isLeft());
                Assert.assertArrayEquals(ciphers, expectedCiphersOrException.left());
                return null;
            }
        }).when(sslServerSocket).setEnabledCipherSuites(Mockito.any(String[].class));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final String[] protocols = extractArgument(invocation);
                Assert.assertTrue(expectedProtocolsOrException.isLeft());
                Assert.assertArrayEquals(protocols, expectedProtocolsOrException.left());
                return null;
            }
        }).when(sslServerSocket).setEnabledProtocols(Mockito.any(String[].class));

        if (expectedCiphersOrException.isRight() || expectedProtocolsOrException.isRight()) {
            try {
                wrapper.createServerSocket();
                Assert.fail("createServerSocket should have failed");
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
            wrapper.createServerSocket();
        }
    }
}
