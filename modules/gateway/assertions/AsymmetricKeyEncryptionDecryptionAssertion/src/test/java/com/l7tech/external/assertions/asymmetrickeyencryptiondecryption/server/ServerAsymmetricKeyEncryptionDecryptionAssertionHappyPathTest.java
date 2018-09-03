package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server;

import com.l7tech.common.TestKeys;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.HexUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Parameterized test for the ServerAsymmetricKeyEncryptionDecrytpionAssertion happy path
 */
@RunWith(Parameterized.class)
public class ServerAsymmetricKeyEncryptionDecryptionAssertionHappyPathTest {

    public static final String INPUT5 = "Enc/=";
    public static final String INPUT16 ="happy/PathTest1=";
    public static final String INPUT50 = "happy/PathTest1=happy/PathTest1=happy/PathTest1=ha";
    public static final String INPUT64 = "happy/PathTest1=happy/PathTest1=happy/PathTest1=happy/PathTest1=";
    public static final String INPUT96 = "happy/PathTest1=happy/PathTest1=happy/PathTest1=happy/PathTest1=happy/PathTest1=happy/PathTest1=";

    @Mock
    private SsgKeyStoreManager mockSsgKeyStoreManager;
    @Mock
    private TrustedCertManager mockTrustedCertManager;
    @Mock
    private TrustedCert mockTrustCert;
    @Mock
    private PolicyEnforcementContext mockPolicyContext;

    private X509Certificate certificate;
    private PrivateKey privateKey;
    private Goid keyGoid = new Goid(1, 0);
    private String keyName = "certKeyName";
    private Map<String, Object> varUsed = new HashMap<>();
    private String input;
    private String inputEncoded;
    private String algorithm;

    public ServerAsymmetricKeyEncryptionDecryptionAssertionHappyPathTest(X509Certificate certificate, PrivateKey privateKey, String input, String algorithm) {
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.input = input;
        this.algorithm = algorithm;
    }

    private AsymmetricKeyEncryptionDecryptionAssertion encryptAssertion;
    private AsymmetricKeyEncryptionDecryptionAssertion decryptAssertion;

    @Before
    public void setup() throws IOException, CertificateException, FindException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException {
        MockitoAnnotations.initMocks(this);
        inputEncoded = HexUtils.encodeBase64(input.getBytes());
        varUsed.put("input", inputEncoded);
        when(mockPolicyContext.getVariableMap(any(String[].class), any(Audit.class))).thenReturn(varUsed);

        //encrypt
        encryptAssertion = new AsymmetricKeyEncryptionDecryptionAssertion();
        encryptAssertion.setInputVariable("input");
        encryptAssertion.setOutputVariable("output");
        encryptAssertion.setMode(Cipher.ENCRYPT_MODE);
        encryptAssertion.setKeyName(keyName);
        encryptAssertion.setKeyGoid(keyGoid);

        when(mockTrustedCertManager.findByPrimaryKey(keyGoid)).thenReturn(mockTrustCert);
        when(mockTrustCert.getCertificate()).thenReturn(certificate);

        //decrypt
        decryptAssertion = new AsymmetricKeyEncryptionDecryptionAssertion();
        decryptAssertion.setInputVariable("input");
        decryptAssertion.setOutputVariable("output");
        decryptAssertion.setMode(Cipher.DECRYPT_MODE);
        decryptAssertion.setKeyName(keyName);
        decryptAssertion.setKeyGoid(keyGoid);

        SsgKeyEntry ssgKeyEntry = new SsgKeyEntry(keyGoid, "privateKey", new X509Certificate[]{certificate}, privateKey);
        when(mockSsgKeyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid)).thenReturn(ssgKeyEntry);
    }

    /**
     * Get the parameterized data
     * @return Collection of data for testing
     */
    @Parameterized.Parameters(name = "{index}:\n\tCert={0}\n\tPrivateKey={1}\n\tInput={2}\n\tAlgorithm={3}\n\tExpected Assertion Status=")
    public static Collection data() throws IOException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        return Arrays.asList(new Object[][]{

                // RSA/ECB/NoPadding
                // To reliably decrypt the key size affects what the input size should be because of NoPadding
                // 512 bits -> multiples of 64 bytes
                // 768 bits -> multiples of 96 bytes
                {TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_512_KEY_PKCS8_B64), INPUT64,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_NO_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64), INPUT96,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_NO_PADDING)},

                // RSA/ECB/PKCS1Padding
                // 512 bits -> max 53 bytes
                // 768 bits -> max 85 bytes
                {TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_512_KEY_PKCS8_B64), INPUT5,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_PKCS1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64), INPUT64,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_PKCS1_PADDING)},


                // RSA/ECB/OAEPWithMD5AndMGF1Padding
                // 512 bits -> max 30 bytes
                // 768 bits -> max 62 bytes
                {TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_512_KEY_PKCS8_B64), INPUT5,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_MD5_AND_MGF1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64), INPUT50,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_MD5_AND_MGF1_PADDING)},

                // RSA/ECB/OAEPWithSHA1AndMGF1Padding
                // 512 bits -> max 22 bytes
                // 768 bits -> max 54 bytes
                {TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_512_KEY_PKCS8_B64), INPUT5,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA1_AND_MGF1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_512_KEY_PKCS8_B64), INPUT16,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA1_AND_MGF1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64), INPUT50,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA1_AND_MGF1_PADDING)},


                // RSA/ECB/OAEPWithSHA224AndMGF1Padding
                {TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_512_KEY_PKCS8_B64), INPUT5,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA224_AND_MGF1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64), INPUT16,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA224_AND_MGF1_PADDING)},


                // RSA/ECB/OAEPWithSHA256AndMGF1Padding
                {TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64), INPUT5,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA256_AND_MGF1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_1024_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_1024_KEY_PKCS8_B64), INPUT50,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA256_AND_MGF1_PADDING)},


                // RSA/ECB/OAEPWithSHA384AndMGF1Padding
                {TestKeys.getCert(TestKeys.RSA_1024_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_1024_KEY_PKCS8_B64), INPUT5,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA384_AND_MGF1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_1024_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_1024_KEY_PKCS8_B64), INPUT16,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA384_AND_MGF1_PADDING)},


                // RSA/ECB/OAEPWithSHA512AndMGF1Padding
                {TestKeys.getCert(TestKeys.RSA_2048_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_2048_KEY_PKCS8_B64), INPUT5,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA512_AND_MGF1_PADDING)},
                {TestKeys.getCert(TestKeys.RSA_2048_CERT_X509_B64), TestKeys.getKey("RSA", TestKeys.RSA_2048_KEY_PKCS8_B64), INPUT64,
                        BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA512_AND_MGF1_PADDING)},

        });
    }

    /**
     * Test Encrypt with the input and decrypt with the output
     * Expect all tests to passed
     *
     * @throws Exception
     */
    @Test
    public void testEncryptDecrypt() throws Exception {
        //encrypt

        encryptAssertion.setAlgorithm(algorithm);

        ServerAsymmetricKeyEncryptionDecryptionAssertion encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setAlgorithm(algorithm);

        ServerAsymmetricKeyEncryptionDecryptionAssertion decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(inputEncoded, outputDec);
        Assert.assertEquals(input, outputDecDecoded);
    }

    /**
     * helper method to get the output value
     *
     * @return the value of the encrpt/decrypt output
     * @throws Exception
     */
    private String getOutputString() throws Exception {
        // Capture the value of the encrypt/decrypt output
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockPolicyContext, atLeast(1)).setVariable(eq("output"), argument.capture());
        return argument.getValue();
    }
}
