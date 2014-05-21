package com.l7tech.server.security;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.ext.security.Signer;
import com.l7tech.policy.assertion.ext.security.SignerException;
import com.l7tech.policy.assertion.ext.security.SignerServices;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link com.l7tech.server.security.SignerServicesImpl} and {@link com.l7tech.server.security.SignerImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class SignerServicesTest {

    private SignerServicesImpl signerServices;

    @Mock
    private SsgKeyStoreManager ssgKeyStoreManager;

    @Mock
    private SsgKeyFinder ssgKeyFinder;

    @Mock
    private SsgKeyStore ssgKeyStore;

    @Mock
    private SsgKeyEntry defaultSslKeyEntry;

    @Mock
    private PrivateKey defaultSslPrivateKey;

    @Before
    public void setup() throws Exception {
        DefaultKey defaultKey = new TestDefaultKey();
        signerServices = new SignerServicesImpl(ssgKeyStoreManager, defaultKey);
    }

    @Test(expected = SignerException.class)
    public void testCreateSignerNullKeyId() throws Exception {
        signerServices.createSigner(null);
    }

    @Test(expected = SignerException.class)
    public void testCreateSignerEmptyKeyId() throws Exception {
        signerServices.createSigner(" ");
    }

    @Test(expected = SignerException.class)
    public void testCreateSignerInvalidKeyId() throws Exception {
        // Valid format is "<keystore_goid>:<alias>".
        signerServices.createSigner("invalid_key");
    }

    @Test
    public void testCreateSignerKeyIdNotFoundAllKeystores() throws Exception {
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("alias_not_found", Goid.DEFAULT_GOID)).thenThrow(new FindException("Mocking Exception"));
        String keyId = Goid.DEFAULT_GOID.toString() + ":" + "alias_not_found";
        Signer signer = signerServices.createSigner(keyId);

        // verify
        Assert.assertNull(signer);
        verify(ssgKeyStoreManager, times(1)).lookupKeyByKeyAlias("alias_not_found", Goid.DEFAULT_GOID);
        verify(ssgKeyStoreManager, times(0)).findAll();
    }

    @Test
    public void testCreateSignerKeyIdNotFoundSpecifiedKeystore() throws Exception {
        when(ssgKeyStoreManager.findAll()).thenReturn(Arrays.asList(ssgKeyFinder));
        when(ssgKeyFinder.getKeyStore()).thenReturn(ssgKeyStore);
        when(ssgKeyFinder.getCertificateChain("alias_not_found")).thenThrow(new ObjectNotFoundException("Mocking Exception"));
        when(ssgKeyStore.getGoid()).thenReturn(new Goid(0,12345L));

        String keyId = new Goid(0,12345L).toString() + ":" + "alias_not_found";
        Signer signer = signerServices.createSigner(keyId);

        // verify
        Assert.assertNull(signer);
        verify(ssgKeyStoreManager, times(0)).lookupKeyByKeyAlias(anyString(), any(Goid.class));
        verify(ssgKeyStoreManager, times(1)).findAll();
    }

    @Test
    public void testCreateSignatureNullHashAlgorithm() throws Exception {
        Signer signer = signerServices.createSigner(SignerServices.KEY_ID_SSL);

        // verify
        Assert.assertNotNull(signer);
        byte[] signature = signer.createSignature(null, new ByteArrayInputStream("hello".getBytes()));
        Assert.assertNotNull(signature);
    }

    @Test
    public void testCreateSignatureEmptyHashAlgorithm() throws Exception {
        Signer signer = signerServices.createSigner(SignerServices.KEY_ID_SSL);

        // verify
        Assert.assertNotNull(signer);
        byte[] signature = signer.createSignature(" ", new ByteArrayInputStream("hello".getBytes()));
        Assert.assertNotNull(signature);
    }

    @Test
    public void testCreateSignatureDefaultSslKey() throws Exception {
        Signer signer = signerServices.createSigner(SignerServices.KEY_ID_SSL);

        // verify
        Assert.assertNotNull(signer);
        byte[] signature = signer.createSignature("SHA-256", new ByteArrayInputStream("hello".getBytes()));
        Assert.assertNotNull(signature);
    }

    @Test
    public void testCreateSignatureKeyIdFoundAllKeystores() throws Exception {
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("ssl", Goid.DEFAULT_GOID)).thenReturn(
            new SsgKeyEntry(
                new Goid(0, 2000L),
                "ssl",
                new X509Certificate[]{new TestCertificateGenerator().subject("CN=test1").generate()},
                new TestCertificateGenerator().getPrivateKey())
        );

        String keyId = "-1:ssl";
        Signer signer = signerServices.createSigner(keyId);

        // verify
        Assert.assertNotNull(signer);
        verify(ssgKeyStoreManager, times(1)).lookupKeyByKeyAlias("ssl", Goid.DEFAULT_GOID);
        verify(ssgKeyStoreManager, times(0)).findAll();
        byte[] signature = signer.createSignature("SHA-256", new ByteArrayInputStream("hello".getBytes()));
        Assert.assertNotNull(signature);
    }

    @Test
    public void testCreateSignatureKeyIdFoundSpecifiedKeystore() throws Exception {
        when(ssgKeyStoreManager.findAll()).thenReturn(Arrays.asList(ssgKeyFinder));
        when(ssgKeyFinder.getKeyStore()).thenReturn(ssgKeyStore);
        when(ssgKeyFinder.getCertificateChain("ssl")).thenReturn(
            new SsgKeyEntry(
                new Goid(0, 2000L),
                "ssl",
                new X509Certificate[]{new TestCertificateGenerator().subject("CN=test1").generate()},
                new TestCertificateGenerator().getPrivateKey()));
        when(ssgKeyStore.getGoid()).thenReturn(new Goid(0,12345L));

        String keyId = new Goid(0,12345L).toString() + ":" + "ssl";
        Signer signer = signerServices.createSigner(keyId);

        // verify
        Assert.assertNotNull(signer);
        verify(ssgKeyStoreManager, times(0)).lookupKeyByKeyAlias(anyString(), any(Goid.class));
        verify(ssgKeyStoreManager, times(1)).findAll();
        byte[] signature = signer.createSignature("SHA-256", new ByteArrayInputStream("hello".getBytes()));
        Assert.assertNotNull(signature);
    }
}