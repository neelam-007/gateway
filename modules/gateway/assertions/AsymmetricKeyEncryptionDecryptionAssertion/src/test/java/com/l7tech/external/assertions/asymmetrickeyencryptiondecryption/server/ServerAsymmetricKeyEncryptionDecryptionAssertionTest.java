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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test the AsymmetricKeyEncryptionDecryptionAssertion.
 * For Encryption: the AsymmetricKeyEncryptionDecryptionAssertion takes a certificate, a base64 encoded input, and a padding mode. It will output a base64 encoded encryption
 * For Decryption: the AsymmetricKeyEncryptionDecryptionAssertion takes a private key, a base64 encoded input, and a padding mode. It will output a base64 encoded decryption
 * <p/>
 * RSA encryption with no padding is a well defined algorithm. The The Key length, the input length as well as the padding mode makes a difference in the output.
 * For other encryption, there is an input length limit.
 * The test cases below cover tests values with various input length, mode and key size
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerAsymmetricKeyEncryptionDecryptionAssertionTest {

    @Mock
    private SsgKeyStoreManager mockSsgKeyStoreManager;
    @Mock
    private TrustedCertManager mockTrustedCertManager;
    @Mock
    private TrustedCert mockTrustCert;
    @Mock
    private PolicyEnforcementContext mockPolicyContext;

    SsgKeyEntry ssgKeyEntry;
    X509Certificate certificate;
    PrivateKey privateKey;
    Goid keyGoid = new Goid(1, 0);
    String keyName = "certKeyName";
    Map<String, Object> varUsed = new HashMap<>();

    String input5Encode = HexUtils.encodeBase64("Enc/=".getBytes());
    String input16Encode = HexUtils.encodeBase64("happy/PathTest1=".getBytes());
    String input50Encode = HexUtils.encodeBase64("happy/PathTest1=happy/PathTest1=happy/PathTest1=ha".getBytes());
    String input64Encode = HexUtils.encodeBase64("happy/PathTest1=happy/PathTest1=happy/PathTest1=happy/PathTest1=".getBytes());

    private ServerAsymmetricKeyEncryptionDecryptionAssertion decryptServer;
    private ServerAsymmetricKeyEncryptionDecryptionAssertion encryptServer;

    private AsymmetricKeyEncryptionDecryptionAssertion encryptAssertion;
    private AsymmetricKeyEncryptionDecryptionAssertion decryptAssertion;

    @Before
    public void setup() throws FindException, KeyStoreException, UnrecoverableKeyException, IOException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        when(mockPolicyContext.getVariableMap(any(String[].class), any(Audit.class))).thenReturn(varUsed);

        //encrypt
        encryptAssertion = new AsymmetricKeyEncryptionDecryptionAssertion();
        encryptAssertion.setInputVariable("input");
        encryptAssertion.setOutputVariable("output");
        encryptAssertion.setMode(Cipher.ENCRYPT_MODE);
        encryptAssertion.setKeyName(keyName);
        encryptAssertion.setKeyGoid(keyGoid);

        certificate = TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64);
        when(mockTrustedCertManager.findByPrimaryKey(keyGoid)).thenReturn(mockTrustCert);
        when(mockTrustCert.getCertificate()).thenReturn(certificate);

        //decrypt
        decryptAssertion = new AsymmetricKeyEncryptionDecryptionAssertion();
        decryptAssertion.setInputVariable("input");
        decryptAssertion.setOutputVariable("output");
        decryptAssertion.setMode(Cipher.DECRYPT_MODE);
        decryptAssertion.setKeyName(keyName);
        decryptAssertion.setKeyGoid(keyGoid);

        privateKey = TestKeys.getKey("RSA", TestKeys.RSA_512_KEY_PKCS8_B64);
        ssgKeyEntry = new SsgKeyEntry(keyGoid, "privateKey", new X509Certificate[]{certificate}, privateKey);
        when(mockSsgKeyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid)).thenReturn(ssgKeyEntry);
    }

    /**
     * Mode: None
     * Input Length: 5
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulNoModeNoPaddingWith5LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input5Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);

        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input5Encode, outputDec);
        Assert.assertEquals(5, outputDecDecoded.length());
    }

    /**
     * Mode: ECB PKCS1 Padding
     * Input Length: 16
     * Key Size: 512
     * Using Raw RSA keys - no Cert
     * @throws Exception
     */
    @Test
    public void testSuccessfulWithRsaKey() throws Exception {
        //encrypt
        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);

         final String RSA_512_PRIVATE_KEY_PEM =
               "-----BEGIN RSA PRIVATE KEY-----\n" +
                       "MIIBOwIBAAJBANEGhtwiM7Oiz7ZMjqczRpAokQlZ1GPCrenCRXM5dLTQwtcsVU/A\n" +
                       "MwZeZVfZekP4JIKpPdd83EJDPwZgFhSbXVsCAwEAAQJAEXzVTZeC8dV+QUc4bB6r\n" +
                       "GaZ7M+gTD+GawULiopg8/l+OKJMV8VdXICZj9mhk5bQufel5+LvW8S3Z71vV9iMx\n" +
                       "eQIhAOd9UHAsk51jN1ORGnPKuYuxWlVdtn7L1riXoiylhq9XAiEA5yhPSbviF7Eu\n" +
                       "oL/1o1i+qGhZ4rkYUh25F00W10XYs50CIA6qacYxjMiT2JV6w+pCFa879TUjUsSF\n" +
                       "tXzMXoHlmrrRAiEAtsW1o5xuUcNkFfCSHf0ui2QvJkiqRUuBLT5kAAUXKjUCIQCi\n" +
                       "doi8zK8G2nsI4rIw0T3YxwpIKLB4JbQ5zWW870xsZQ==\n" +
                       "-----END RSA PRIVATE KEY-----";

        // PEM without '-----BEGIN PUBLIC KEY-----', '-----END PUBLIC KEY-----' header.
        final String RSA_512_PUBLIC_KEY_B64 = "-----BEGIN PUBLIC KEY-----" +
                "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANEGhtwiM7Oiz7ZMjqczRpAokQlZ1GPC\n" +
                "renCRXM5dLTQwtcsVU/AMwZeZVfZekP4JIKpPdd83EJDPwZgFhSbXVsCAwEAAQ==" +
                "-----END PUBLIC KEY-----";

        encryptAssertion.setKeySource(AsymmetricKeyEncryptionDecryptionAssertion.KeySource.FROM_VALUE);
        encryptAssertion.setRsaKeyValue(RSA_512_PUBLIC_KEY_B64);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        varUsed.put("rsa", RSA_512_PRIVATE_KEY_PEM);

        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);
        decryptAssertion.setKeySource(AsymmetricKeyEncryptionDecryptionAssertion.KeySource.FROM_VALUE);
        decryptAssertion.setRsaKeyValue("${rsa}");
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input16Encode, outputDec);
        Assert.assertEquals(16, outputDecDecoded.length());
    }

    @Test
    public void testSuccessfulWithRsaPKCS8FormatKey() throws Exception {
        //encrypt
        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);

        final String RSA_2048_PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n" +
                TestKeys.RSA_2048_KEY_PKCS8_B64 +
                "\n-----END PRIVATE KEY-----";

        final String RSA_2048_PUBLIC_KEY = "-----BEGIN CERTIFICATE-----\n" +
                TestKeys.RSA_2048_CERT_X509_B64 +
                "\n-----END CERTIFICATE-----";

        varUsed.put("rsa", RSA_2048_PUBLIC_KEY);

        encryptAssertion.setKeySource(AsymmetricKeyEncryptionDecryptionAssertion.KeySource.FROM_VALUE);
        encryptAssertion.setRsaKeyValue("${rsa}");

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        varUsed.put("rsa", RSA_2048_PRIVATE_KEY_PEM);

        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);
        decryptAssertion.setKeySource(AsymmetricKeyEncryptionDecryptionAssertion.KeySource.FROM_VALUE);
        decryptAssertion.setRsaKeyValue("${rsa}");
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(16, outputDecDecoded.length());
    }

    /**
     * Mode: PKCS1
     * Input Length: 5
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulPKCS1PaddingWith5LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input5Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input5Encode, outputDec);
        Assert.assertEquals(5, outputDecDecoded.length());
    }

    /**
     * Mode: SHA1 and MDG1
     * Input Length: 5
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSHA1PaddingWith5LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input5Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input5Encode, outputDec);
        Assert.assertEquals(5, outputDecDecoded.length());
    }

    /**
     * Mode: None
     * Input Length: 16
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulNoModeNoPaddingWith16LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input16Encode, outputDec);
        Assert.assertEquals(16, outputDecDecoded.length());
    }

    /**
     * Mode: No ECB Padding
     * Input Length: 16
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulECBNoPaddingWith16LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(64, outputDecDecoded.length()); //keysize: 512 bits  (64 bytes),  number of input bytes: multiples of 64
    }

    /**
     * Mode: PKCS1
     * Input Length: 16
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulPKCS1PaddingWith16LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input16Encode, outputDec);
        Assert.assertEquals(16, outputDecDecoded.length());
    }

    /**
     * Mode: SHA1 and MDG1
     * Input Length: 16
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSHA1PaddingWith16LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input16Encode, outputDec);
        Assert.assertEquals(16, outputDecDecoded.length());
    }

    /**
     * Mode: None
     * Input Length: 50
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulNoModeNoPaddingWith50LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input50Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input50Encode, outputDec);
        Assert.assertEquals(50, outputDecDecoded.length());
    }

    /**
     * Mode: No ECB Padding
     * Input Length: 50
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulECBNoPaddingWith50LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input50Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(64, outputDecDecoded.length());
    }

    /**
     * Mode: PKCS1 Padding
     * Input Length: 50
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulPKCS1PaddingWith50LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input50Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input50Encode, outputDec);
        Assert.assertEquals(50, outputDecDecoded.length());
    }

    /**
     * Mode: No ECB Padding
     * Input Length: 64
     * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulECBNoPaddingWith64LengthInputAnd512Key() throws Exception {
        //encrypt
        varUsed.put("input", input64Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input64Encode, outputDec);
        Assert.assertEquals(64, outputDecDecoded.length()); //no padding
    }

    /**
     * The following test case will ensure that the assertion will fail when the input is longer than the mdoe allows
     * 1.      * Mode: None      * Input Length: 64      * Key Size: 512
     * <p/>
     * 2.      * Mode: PKCS1      * Input Length: 64      * Key Size: 512
     * <p/>
     * 3.      * Mode: SHA1 and MDG1      * Input Length: 64      * Key Size: 512
     * <p/>
     * 4.      * Mode: SHA1 and MDG1      * Input Length: 50      * Key Size: 512
     *
     * @throws Exception
     */
    @Test
    public void testFailWithLongInputAnd512Key() throws Exception {
        //64 character input
        //No mode No padding mode
        varUsed.put("input", input64Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);

        Assert.assertEquals(AssertionStatus.FAILED, status);

        //64 character input
        //PKCS1 padding mode
        varUsed.put("input", input64Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        status = encryptServer.checkRequest(mockPolicyContext);

        Assert.assertEquals(AssertionStatus.FAILED, status);

        //64 character input
        //OAEP padding mode
        varUsed.put("input", input64Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        status = encryptServer.checkRequest(mockPolicyContext);

        Assert.assertEquals(AssertionStatus.FAILED, status);


        //50 character input
        //OAEP padding mode
        varUsed.put("input", input50Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        status = encryptServer.checkRequest(mockPolicyContext);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    /**
     * Mode: None
     * Input Length: 16
     * Key Size: 768
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulNoModeNoPaddingWith16LengthInputAnd768Key() throws Exception {
        //encrypt
        certificate = TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64);
        when(mockTrustCert.getCertificate()).thenReturn(certificate);

        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        privateKey = TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64);
        ssgKeyEntry = new SsgKeyEntry(keyGoid, "privateKey", new X509Certificate[]{certificate}, privateKey);
        when(mockSsgKeyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid)).thenReturn(ssgKeyEntry);

        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input16Encode, outputDec);
        Assert.assertEquals(16, outputDecDecoded.length());
    }

    /**
     * Mode: No ECB padding
     * Input Length: 16
     * Key Size: 768
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulECBNoPaddingWith16LengthInputAnd768Key() throws Exception {
        //encrypt
        certificate = TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64);
        when(mockTrustCert.getCertificate()).thenReturn(certificate);

        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        privateKey = TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64);
        ssgKeyEntry = new SsgKeyEntry(keyGoid, "privateKey", new X509Certificate[]{certificate}, privateKey);
        when(mockSsgKeyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid)).thenReturn(ssgKeyEntry);

        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_NO_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(96, outputDecDecoded.length()); //keysize: 768 bits  (96 bytes),  number of input bytes: multiples of 96
    }

    /**
     * Mode: PKCS1
     * Input Length: 16
     * Key Size: 768
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulPKCS1PaddingWith16LengthInputAnd768Key() throws Exception {
        //encrypt
        certificate = TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64);
        when(mockTrustCert.getCertificate()).thenReturn(certificate);

        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        privateKey = TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64);
        ssgKeyEntry = new SsgKeyEntry(keyGoid, "privateKey", new X509Certificate[]{certificate}, privateKey);
        when(mockSsgKeyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid)).thenReturn(ssgKeyEntry);

        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECB_PKCS1_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input16Encode, outputDec);
        Assert.assertEquals(16, outputDecDecoded.length());

    }

    /**
     * Mode: SHA1 and MDG1
     * Input Length: 16
     * Key Size: 768
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSHA1PaddingWith16LengthInputAnd768Key() throws Exception {
        //encrypt
        certificate = TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64);
        when(mockTrustCert.getCertificate()).thenReturn(certificate);

        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);
        String outputEnc = getOutputString();

        Assert.assertEquals(AssertionStatus.NONE, status);

        //decrypt
        privateKey = TestKeys.getKey("RSA", TestKeys.RSA_768_KEY_PKCS8_B64);
        ssgKeyEntry = new SsgKeyEntry(keyGoid, "privateKey", new X509Certificate[]{certificate}, privateKey);
        when(mockSsgKeyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid)).thenReturn(ssgKeyEntry);

        varUsed.clear();
        varUsed.put("input", outputEnc);
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING);
        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);

        status = decryptServer.checkRequest(mockPolicyContext);
        String outputDec = getOutputString();
        String outputDecDecoded = new String(HexUtils.decodeBase64(outputDec));

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(input16Encode, outputDec);
        Assert.assertEquals(16, outputDecDecoded.length());
    }

    /**
     * Test assertion fails when provided key is not of RSA type
     *
     * @throws Exception
     */
    @Test
    public void testFailNotRSAKey() throws Exception {
        varUsed.put("input", input16Encode);
        certificate = TestKeys.getCert(TestKeys.RSA_768_CERT_X509_B64);
        privateKey = mock(PrivateKey.class);
        ssgKeyEntry = new SsgKeyEntry(keyGoid, "privateKey", new X509Certificate[]{certificate}, privateKey);
        when(mockSsgKeyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid)).thenReturn(ssgKeyEntry);
        when(privateKey.getAlgorithm()).thenReturn("somethingelse");
        decryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);

        decryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(decryptAssertion, null);
        decryptServer.setKeyStoreManager(mockSsgKeyStoreManager);
        AssertionStatus status = decryptServer.checkRequest(mockPolicyContext);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    /**
     * test assertion fails when certificate is null
     *
     * @throws Exception
     */
    @Test
    public void testFailNullCert() throws Exception {
        varUsed.put("input", input16Encode);
        encryptAssertion.setModePaddingOption(RsaModePaddingOption.NO_MODE_NO_PADDING);
        when(mockTrustCert.getCertificate()).thenReturn(null);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    /**
     * test assertion fails when mode is not encrypt or decrypt
     *
     * @throws Exception
     */
    @Test
    public void testFailWithIncorrectMode() throws Exception {
        varUsed.put("input", input16Encode);
        encryptAssertion.setMode(0);

        encryptServer = new ServerAsymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, null);
        encryptServer.setTrustedCertManager(mockTrustedCertManager);
        AssertionStatus status = encryptServer.checkRequest(mockPolicyContext);

        Assert.assertEquals(AssertionStatus.FAILED, status);
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