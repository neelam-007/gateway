package com.l7tech.external.assertions.symmetrickeyencryptiondecryption.server;

import com.l7tech.external.assertions.symmetrickeyencryptiondecryption.SymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.test.BugId;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the SymmetricKeyEncryptionDecryptionAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSymmetricKeyEncryptionDecryptionAssertionTest {

    private static final Logger log = Logger.getLogger(ServerSymmetricKeyEncryptionDecryptionAssertionTest.class.getName());

    @Mock
    ApplicationContext mockApplicationContext;

    @Mock
    PolicyEnforcementContext mockPolicyEnforcementContext;

    @Before
    public void setUp() {

        System.setProperty("com.l7tech.security.prov.rsa.libpath.nonfips", "USECLASSPATH");
        JceProvider.init();
    }

    @Test
    public void testSanityTest() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "happypathtest";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "AES/CBC/PKCS5Padding";
        String _128bitkey = "thisour128bitkey";
        String _128bitkeyB64 = HexUtils.encodeBase64(_128bitkey.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, _128bitkeyB64, variableName, Algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, _128bitkeyB64, decryptVariableName, Algorithm, false, "");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);


        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);
    }

    @Test
    public void testAES192Test() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "happypathtest";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "AES/CBC/PKCS5Padding";
        String _192bitkey = "thisour192bitkey192keybi";
        String _192bitkeyB64 = HexUtils.encodeBase64(_192bitkey.getBytes(Charsets.UTF8));
        String variableName = "192bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, _192bitkeyB64, variableName, Algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decryptVariableName = "192bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, _192bitkeyB64, decryptVariableName, Algorithm, false,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);


        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);
    }

    @Test
    public void testAES256Test() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "happypathtest";
        String textb64 = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "AES/CBC/PKCS5Padding";
        String _256bitkey = "thisour192bitkey192keybit256bitk";
        String _256bitkeyB64 = HexUtils.encodeBase64(_256bitkey.getBytes(Charsets.UTF8));
        String variableName = "192bitencryptoutputtest";
        setUpAssertion(encryptassertion, textb64, _256bitkeyB64, variableName, Algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decryptVariableName = "192bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, _256bitkeyB64, decryptVariableName, Algorithm, false,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, textb64);
    }

    @Test
    public void testDES() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "happypathtest";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "DES/CBC/PKCS5Padding";
        String _56bitkey = "thisour1";
        String _56bitkeyB64 = HexUtils.encodeBase64(_56bitkey.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, _56bitkeyB64, variableName, Algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, _56bitkeyB64, decryptVariableName, Algorithm, false,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);
    }

    @Test
    public void test3DES() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "happypathtest";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "DESede/CBC/PKCS5Padding";
        String _key = JceProvider.getInstance().isFips140ModeEnabled()? "thisour1thisour1thisour1" : "thisour1";
        String _keyB64 = HexUtils.encodeBase64(_key.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, _keyB64, variableName, Algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, _keyB64, decryptVariableName, Algorithm, false,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);
    }

    @Test
    public void testPGP_Default() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "This is what I am encrypt";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "PGP";
        String passphrase = "my7layer";
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, "", variableName, Algorithm, true,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        System.out.println("interimn Output: " + output);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, "", decryptVariableName, Algorithm, false,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);

        System.out.println("Final Output: " + finaloutput + "   B64Decoded: " + new String(HexUtils.decodeBase64(finaloutput)));
    }

    //helper method used for PGP Public Key Encryption
    private ServerSymmetricKeyEncryptionDecryptionAssertion pgpPublicKeyEncryptionHelper(String msg, String msgVar, boolean isAsciiArmourEnabled) throws Exception {
         final String algorithm = "PGP";

        //the following public key was created using gpg 1.4.5 (https://blogs.oracle.com/wssfc/how-to-generate-pgp-keys-using-gpg-145-on-linux)
         final String publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
                "Version: BCPG C# v1.6.1.0\n" +
                "\n" +
                "mQENBFmB670BCACMBdid8VQoUCNZYu9h/cdLf15sKIzYl9vJoLqWwTwW5uoOwkuZ\n" +
                "u8SnnJBODVfL7QX9YLev8ipGD8zGXobFEPiIdCtBMpecxDKkzCEDpJm5NyCOaSCu\n" +
                "mv06nqbpViEEh6+ecOjWeK/UP5zFyqmISF1JMFkrIlcISKjWTBD7evf0m2TjbSN+\n" +
                "wC4eToL14Hvk+l9g0hGR2JB/QHKDiKmgeJuPgk+bQFwMawRWd1zxJQsBYC8vRI7h\n" +
                "5AxyC+D/ufwiELbALyZHj0KZvnltBBph8lpd1k3OiLX04RRYT3DJmN0XpPzaaAys\n" +
                "lUq/uY7xNlDLn61quHrKIMXeTGpAd5wb2CzbABEBAAG0AIkBHAQQAQIABgUCWYHr\n" +
                "vQAKCRB0dcaNnb8TxskRB/92KphE1KPX8LWPNcuaAKeFA8FNjUhrK5RIhv7i/mF6\n" +
                "29nJGk7Ey9XYrvycFjMT2VkOo3WWw6xwGsopgcR7LnfUmk/otrx7fHHJpLbLKOQv\n" +
                "7lhOvaEHoUTrBlkgHeb0GkA7jkLe2novd2J3w9NaFL7WNYQ79BNH+Owdvgr9jaUH\n" +
                "E4hYACiiUUHaUng1SoUuwUjO3up9B1xhgQAliWK73kUBXLLnoTdf7yEUqGSVDSLq\n" +
                "TYZz8M7UVODsyVzlap0HBRLQUY1bKWuNWWS9sL7+g/q7bnOiBuK1PAJdABigt4sP\n" +
                "xsiD2Vx90tBcjpLlcnTQl3jwkpMO4n7mHH7uN6Gprhth\n" +
                "=3o0r\n" +
                "-----END PGP PUBLIC KEY BLOCK-----";
         final String publicKeyb64 = HexUtils.encodeBase64(publicKey.getBytes(Charsets.UTF8));

        SymmetricKeyEncryptionDecryptionAssertion encryptAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(encryptAssertion, msg, publicKeyb64, msgVar, algorithm, true,null, true, isAsciiArmourEnabled);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptServerAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, mockApplicationContext);

        return encryptServerAssertion;
    }

    //helper method used for PGP Public Key Decryption
    private ServerSymmetricKeyEncryptionDecryptionAssertion pgpPublicKeyDecryptionHelper(String msg, String msgVar) throws PolicyAssertionException {
        final String algorithm = "PGP";

        final  String passphrase = "7layer";
        final  String passphraseb64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));

        //the following private key was created using gpg 1.4.5 (https://blogs.oracle.com/wssfc/how-to-generate-pgp-keys-using-gpg-145-on-linux)
        final String PRIVATE_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                "Version: BCPG C# v1.6.1.0\n" +
                "\n" +
                "lQOsBFmB670BCACMBdid8VQoUCNZYu9h/cdLf15sKIzYl9vJoLqWwTwW5uoOwkuZ\n" +
                "u8SnnJBODVfL7QX9YLev8ipGD8zGXobFEPiIdCtBMpecxDKkzCEDpJm5NyCOaSCu\n" +
                "mv06nqbpViEEh6+ecOjWeK/UP5zFyqmISF1JMFkrIlcISKjWTBD7evf0m2TjbSN+\n" +
                "wC4eToL14Hvk+l9g0hGR2JB/QHKDiKmgeJuPgk+bQFwMawRWd1zxJQsBYC8vRI7h\n" +
                "5AxyC+D/ufwiELbALyZHj0KZvnltBBph8lpd1k3OiLX04RRYT3DJmN0XpPzaaAys\n" +
                "lUq/uY7xNlDLn61quHrKIMXeTGpAd5wb2CzbABEBAAH/AwMCoo5UhK7zrsxginQv\n" +
                "jQs6cnu5PhhqYeMiLhwyjnzbdiWdfFW8SfcG32UHUo38DpZi38Oa1xowxzi4Gsdv\n" +
                "XGTWQdrNBYNkKQRuWGADboisVCNrFPCPwDpaK6xyG7oTeqmUts5qOupnqnx0rxgc\n" +
                "0zy6s7/xQZnswDYbaW5LzeVE7VfCyYMpHUKMU65XoPa4OW1vKTMWgwK1rHhQUXn7\n" +
                "b1mRraeMLMmjVkPGg0rztwXPMMvCUI2ZfAy6pUVgvAg1QIi6P+BjfEyiFdv1irv8\n" +
                "6SWs7OJaE1Er/8/hSkvgsShNH8ZO+l1hKZLEVuNz8+AaEllfaZoG8uYCyYWbIl5p\n" +
                "dny5onbP3lITg4wURny7Pz+aJ70qrIQr21r+ZyOZiJVPl0gBmtP03Jqji3gaCsjD\n" +
                "iySViLrlQ4tU8VFIPRJxqkvMbtWT9bCKfk8E5+3ni8Q+IVKytNW9kfgWiuJwBnzx\n" +
                "jivCrJ1zu+mqJYtnxMvfXFNfmtnvmYEcPiVeohvaXdz7dGIT/Q+mQEiSxVDaQ/AW\n" +
                "kqlczrXqoR9IPT9Sx/+Tuzd+uFoNpFowodMl9UxB/1vo44wRxiMuaKKW7O/pi3Bl\n" +
                "g80Gx68eCi3Tvas4gvwcm855+spPdbJmYge0+1LcRioMmp2E9oyr+VRFeIBqxL3T\n" +
                "HR++ylDvlTBeke49Q5n79AyNkr0zdMURcidsZp3Eu2bi28slsb/zCg7yN1Zhmxx8\n" +
                "eXKWif0MOjfYdJxaIdbS5zBd0tW5+VSadhkGr5ilqecvETV0h9383e8Qe5OY+VCb\n" +
                "xeLly5jn8GMUWsEoua0wrfJy8Av7mXRZA7Y5v2l7raIq8vs0Bea3N2b2DjQs1f3t\n" +
                "rKcCjGfDEpdc3EWZ2GsgvL/ufgH/PtBrnPv2DtYLx7QAiQEcBBABAgAGBQJZgeu9\n" +
                "AAoJEHR1xo2dvxPGyREH/3YqmETUo9fwtY81y5oAp4UDwU2NSGsrlEiG/uL+YXrb\n" +
                "2ckaTsTL1diu/JwWMxPZWQ6jdZbDrHAayimBxHsud9SaT+i2vHt8ccmktsso5C/u\n" +
                "WE69oQehROsGWSAd5vQaQDuOQt7aei93YnfD01oUvtY1hDv0E0f47B2+Cv2NpQcT\n" +
                "iFgAKKJRQdpSeDVKhS7BSM7e6n0HXGGBACWJYrveRQFcsuehN1/vIRSoZJUNIupN\n" +
                "hnPwztRU4OzJXOVqnQcFEtBRjVspa41ZZL2wvv6D+rtuc6IG4rU8Al0AGKC3iw/G\n" +
                "yIPZXH3S0FyOkuVydNCXePCSkw7ifuYcfu43oamuG2E=\n" +
                "=MKxg\n" +
                "-----END PGP PRIVATE KEY BLOCK-----";

        final String privateKeyb64 = HexUtils.encodeBase64(PRIVATE_KEY.getBytes(Charsets.UTF8));


        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, msg, privateKeyb64, msgVar, algorithm, false, passphraseb64);

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptServerAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);

        return decryptServerAssertion;

    }

    @Test
    public void testPgpPublicKeyEncryption() throws Exception{
        final String encryptVar = "encryptMsg";

        final String input = "This is what I am encrypting using PGP public key";
        final String inputb64 = HexUtils.encodeBase64(input.getBytes(Charsets.UTF8));


        ServerSymmetricKeyEncryptionDecryptionAssertion encryptServerAssertion = pgpPublicKeyEncryptionHelper(inputb64, encryptVar, false);
        AssertionStatus status = encryptServerAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(encryptVar);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        //decrypt the message that was encrypted earlier
        final String decryptVar = "decryptMsg";

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptServerAssertion = pgpPublicKeyDecryptionHelper(output, decryptVar);
        status = decryptServerAssertion.checkRequest(mockPolicyEnforcementContext);
        output = getOutputString(decryptVar);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        Assert.assertTrue(inputb64.equals(output));
    }

    @Test
    public void testPgpPublicKeyEncryptionWithAsciiArmour() throws Exception{

        //encrypt the input using PGP public key
        final String encryptVar = "encryptMsg";

        final String input = "This is what I am encrypting using PGP public key";
        final String inputb64 = HexUtils.encodeBase64(input.getBytes(Charsets.UTF8));

        final String pgpHeader ="-----BEGIN PGP MESSAGE-----\n";
        final String pgpFooter="-----END PGP MESSAGE-----\n";

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptServerAssertion = pgpPublicKeyEncryptionHelper(inputb64, encryptVar, true);
        AssertionStatus status = encryptServerAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(encryptVar);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decodedMessage = new String (HexUtils.decodeBase64(output));
        Assert.assertTrue(decodedMessage.startsWith(pgpHeader));
        Assert.assertTrue(decodedMessage.endsWith(pgpFooter));

        //decrypt the message that was encrypted earlier
        final String decryptVar = "decryptMsg";

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptServerAssertion = pgpPublicKeyDecryptionHelper(output, decryptVar);
        status = decryptServerAssertion.checkRequest(mockPolicyEnforcementContext);
        output = getOutputString(decryptVar);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        Assert.assertTrue(inputb64.equals(output));
    }
    @Test
    public void testPGPDecrypt() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String b64encodedText = "jA0ECQMCWUpuzb85qoNg0mEBu6Sk8IHaGC3bZKuP+o0GS2dGRYNgO2B/eAbE5BNoa9PEi2Kk6dsK\n" +
                "64OUkYRIYM6R4cvG5Ram0fwINpf+dQRLkZArWFcFsDyKpu8lHt1FoVlRq+L0q/65TSJpHMWR/1T+";
        String Algorithm = "PGP";
        String passphrase = "my7layer";
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, "", variableName, Algorithm, false,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);
        System.out.println("B64 Output: " + output + " B64 Decoded: " + (new String(HexUtils.decodeBase64(output))));

    }

    @Test
     public void testPGPDecryptPrivateKey() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        final String dataStrb64 = "hQEOAzzce8JDCxjSEAP9G0olpdeB2bRO3w77FbI+xBxHA7JUZ20rH7P92kCePDMU8+E8AGoxdMnV\n" +
                "7xE7aoRUp2rlb11LIZrTVrMBX8BBZcOz8WlDOQG/yofavHVJdfbI/cxfPCvAbLVn9rPUZx9fFbAh\n" +
                "sVhpgy23v+ktauGrXoXDrZ//l94ERBUGkRHvaLgD/RetLySYv0oInM61R+iOd+Sx4ke9M1PfyTzR\n" +
                "+bVaonrxIAXxVwkrtnY2sWxssBD0mU8LGyVcm/Gw7Cyo3s67X3tcjxoK4PJzv1U+akBgXjHCzVRn\n" +
                "xCeV0z+DhNvBD6lR4Ctu6DhDy2vIqInKs1EL2ee/uAdQqiPPbG4Jqiv1F12B0lUBmLTK+76hO9ia\n" +
                "hpHBGbI/nhEK7Y55XdQwTOUSMczZYGIEgjLSZYFsHOhl9d96ymC683FCKKvZ+jSvtL6Ca+MKAHyf\n" +
                "/PrkRZjdKAApoZM6uN1fCdrB";
        final String privatekey =
                "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                        "Version: GnuPG v1.2.6 (GNU/Linux)\n" +
                        " \n" +
                        "lQHhBFAQH7QRBACjnCv14z9QGSlCmIAgTa+4jnPdCUPW2ZXfAz8/pqCuH1P28jqE\n" +
                        "5Bb+l2hVsAk6TZDx3eFXZm7uPWCsvqCY32eyo/LTsMvGGaFrYkA47qBzGoSzeOYy\n" +
                        "hv3bEukZfu9+UPEIAtfW1w3nMbORcwpu/JRoHYUE4xPdopuCjtif9akIEwCg22Un\n" +
                        "YpQ+AOcH0db0M0gMBXXy0NcD/i2lwVrbwq4wW0ppeEJ8bF+2+axv9NqDKSq8zU5d\n" +
                        "LsFk9v8JL6aIRRlWgQkSrPzIiM6bgTVWXYr9zPyP059vP5FMkbcz0wn8wYDi+WHR\n" +
                        "YQJB4qoX9NIU7fXT2O3Atkm0lwvT8YHBPcdjkwYRsN/zY0hZEcX2SayduGS21s2l\n" +
                        "05HdBACMblactqIpQvebmVvPHYMLHD/OewIt1nPPJ9llmurHAnYJAIM1qpNBvDWD\n" +
                        "k4WFiwJiAjiC+vdyL+H00/1fcyyr2HUgSaGnY62NVqPtet/DHwJHncrlm9m3Rdtd\n" +
                        "AKHurpsn0jwbwVJgo0GXwhSMGDLTS9AluecdfGaLiPVTVNrpK/4DAwIx/5QqPY2D\n" +
                        "r2B3MJzmNzj7cOk9/9KnfSZXSD4EV98C1H/PkDZ7AncmvMzzGEyTrZmHMr6Uw1cZ\n" +
                        "IlGmrbQsVGVzdCBLZXkgKHRlc3QpIDxwcmludGRldkBhZmZpbmlvbmdyb3VwLmNv\n" +
                        "bT6IXgQTEQIAHgUCUBAftAIbAwYLCQgHAwIDFQIDAxYCAQIeAQIXgAAKCRAxle0m\n" +
                        "0/SSGLnkAJ4lzuwpPBc2NxQIe0kl6u1KvzXRWwCgri9l9TnGzv0qRc1Hx5my4MHL\n" +
                        "4wadAVgEUBAftRAEANhwMV7AGicNtmmB9zGSitpvmnX5VVv3aKzrJeB9lwhmAQ4M\n" +
                        "DtThdXrKPTfGwhpvjEYedqnABOMS4gml3QQer4lrP9dtmFkXdt0Znk+Kh6YOeL+G\n" +
                        "II/WeGo2GHr9GgTeIjmSDJIS8NiqzDMoUPB5I7n3dLxbBK4S+UOKPDnuHiLLAAMF\n" +
                        "A/0Y+pef4uxXLKndTT7Hs/jpmSGE5Bj+ECYeYlpUVTkaW7LyBcunct2MbwmsVSuF\n" +
                        "adVWPV709elbplaLNsjS0QHBvOktSnU9zyqojLEy5O+SRcl4h0HswciStefHxyes\n" +
                        "83ikM91APZ4ubkQSijLrYPJLvBGrmA+NwfJCA4zvEej0k/4DAwIx/5QqPY2Dr2DH\n" +
                        "k75OXTcV2OeEr3uASTZDh5beholJ+J9v5YuLXRwRcXAzCvI+Mk8Saaj1w15kNTFF\n" +
                        "9b9X5jBZsyjsecao1ohJBBgRAgAJBQJQEB+1AhsMAAoJEDGV7SbT9JIYjssAoKnK\n" +
                        "z0lVgZ/sF3L6saD79MF8r6SIAKDAsrtCxlsVFWt29Kc8Qfv7yzTkHg==\n" +
                        "=RvO1\n" +
                        "-----END PGP PRIVATE KEY BLOCK-----";
        String passphrase = "testkey";
        String Algorithm = "PGP";
        String keyB64 = HexUtils.encodeBase64(privatekey.getBytes(Charsets.UTF8));
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, dataStrb64, keyB64, variableName, Algorithm, false,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);
        System.out.println("B64 Output: " + output + " B64 Decoded: " + (new String(HexUtils.decodeBase64(output))));

    }

    @Test
    public void testPGPEncrypt() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "This is what I am encrypting";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "PGP";
        String passphrase = "my7layer";
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";

        setUpAssertion(encryptassertion, b64encodedText, "", variableName, Algorithm, true,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);
        System.out.println("B64 Output: " + output + "-----completed");

    }

    @Test
    public void testBreakPGPDecrypt_badpassphrase() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String b64encodedText = "jA0ECQMCWUpuzb85qoNg0mEBu6Sk8IHaGC3bZKuP+o0GS2dGRYNgO2B/eAbE5BNoa9PEi2Kk6dsK\n" +
                "64OUkYRIYM6R4cvG5Ram0fwINpf+dQRLkZArWFcFsDyKpu8lHt1FoVlRq+L0q/65TSJpHMWR/1T+";
        String Algorithm = "PGP";
        String passphrase = "my7laye";
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, "", variableName, Algorithm, false,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        //String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.FAILED.getMessage(), status.getMessage());
    }

    @Test
    public void testBreakPGPDecrypt_badtext() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String b64encodedText = "j0ECQMCWUpuzb85qoNg0mEBu6Sk8IHaGC3bZKuP+o0GS2dGRYNgO2B/eAbE5BNoa9PEi2Kk6dsK\n" +
                "64OUkYRIYM6R4cvG5Ram0fwINpf+dQRLkZArWFcFsDyKpu8lHt1FoVlRq+L0q/65TSJpHMWR/1T+";
        String Algorithm = "PGP";
        String passphrase = "my7layer";
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, "", variableName, Algorithm, false,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.FAILED.getMessage(), status.getMessage());
    }

    private void setUpAssertion(SymmetricKeyEncryptionDecryptionAssertion assertion, String data, String key, String outputVariable, String algorithm, boolean isEncrypt, String pgpPassPhrase, boolean isPgpPublicEncryption, boolean isAsciiArmourEnabled) {
        assertion.setText(data);
        assertion.setKey(key);
        assertion.setOutputVariableName(outputVariable);
        assertion.setAlgorithm(algorithm);
        assertion.setIsEncrypt(isEncrypt);
        assertion.setPgpPassPhrase(pgpPassPhrase);
        assertion.setIsPgpKeyEncryption(isPgpPublicEncryption);
        assertion.setAsciiArmourEnabled(isAsciiArmourEnabled);
    }

    private void setUpAssertion(SymmetricKeyEncryptionDecryptionAssertion assertion, String data, String key, String outputVariable, String algorithm, boolean isEncrypt, String pgpPassPhrase) {
        setUpAssertion(assertion, data, key, outputVariable, algorithm, isEncrypt, pgpPassPhrase, false, false);
    }

    @Ignore
    public void testRoundTrip() throws Exception {
        doRoundTrip(false, false);
        doRoundTrip(false, true);
        doRoundTrip(true, false);
        doRoundTrip(true, true);
    }


    /*public void testBadEncryptedData() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream( );
        PgpUtil.decrypt(
                new ByteArrayInputStream( "plain text data".getBytes( Charsets.UTF8 ) ),
                out,
                "password".toCharArray()
                );
    } */
    @Ignore
    //@org.junit.Test(expected = PgpUtil.PgpException.class)
    public void doRoundTrip(final boolean asciiArmoured, final boolean integrityProtected) throws IOException, PgpUtil.PgpException {
        final String dataStr = "Test text, test text, test text, test text, test text";
        final byte[] data = dataStr.getBytes(Charsets.UTF8);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        PgpUtil.encrypt(
                new ByteArrayInputStream(data),
                out,
                "file.txt",
                123000, // milliseconds not preserved
                PGPEncryptedData.AES_256,
                "password".toCharArray(),
                asciiArmoured,
                integrityProtected,
                null
        );

        final ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        final PgpUtil.DecryptionMetadata meta = PgpUtil.decrypt(
                new ByteArrayInputStream(out.toByteArray()),
                out2,
                "password".toCharArray()
        );

        assertArrayEquals("clear data", data, out2.toByteArray());
        assertEquals("Integrity protected", integrityProtected, meta.isIntegrityChecked());
        assertEquals("Armoured", asciiArmoured, meta.isAsciiArmoured());
        assertEquals("Filename", "file.txt", meta.getFilename());
        assertEquals("File modified", 123000, meta.getFileModified());
    }

    @Test
    public void testDecryptPrivateKey() throws IOException, PgpUtil.PgpException {
        final String dataStrb64 = "hQEOAzzce8JDCxjSEAP9G0olpdeB2bRO3w77FbI+xBxHA7JUZ20rH7P92kCePDMU8+E8AGoxdMnV\n" +
                "7xE7aoRUp2rlb11LIZrTVrMBX8BBZcOz8WlDOQG/yofavHVJdfbI/cxfPCvAbLVn9rPUZx9fFbAh\n" +
                "sVhpgy23v+ktauGrXoXDrZ//l94ERBUGkRHvaLgD/RetLySYv0oInM61R+iOd+Sx4ke9M1PfyTzR\n" +
                "+bVaonrxIAXxVwkrtnY2sWxssBD0mU8LGyVcm/Gw7Cyo3s67X3tcjxoK4PJzv1U+akBgXjHCzVRn\n" +
                "xCeV0z+DhNvBD6lR4Ctu6DhDy2vIqInKs1EL2ee/uAdQqiPPbG4Jqiv1F12B0lUBmLTK+76hO9ia\n" +
                "hpHBGbI/nhEK7Y55XdQwTOUSMczZYGIEgjLSZYFsHOhl9d96ymC683FCKKvZ+jSvtL6Ca+MKAHyf\n" +
                "/PrkRZjdKAApoZM6uN1fCdrB";
        final String privatekey =
                "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                        "Version: GnuPG v1.2.6 (GNU/Linux)\n" +
                        " \n" +
                        "lQHhBFAQH7QRBACjnCv14z9QGSlCmIAgTa+4jnPdCUPW2ZXfAz8/pqCuH1P28jqE\n" +
                        "5Bb+l2hVsAk6TZDx3eFXZm7uPWCsvqCY32eyo/LTsMvGGaFrYkA47qBzGoSzeOYy\n" +
                        "hv3bEukZfu9+UPEIAtfW1w3nMbORcwpu/JRoHYUE4xPdopuCjtif9akIEwCg22Un\n" +
                        "YpQ+AOcH0db0M0gMBXXy0NcD/i2lwVrbwq4wW0ppeEJ8bF+2+axv9NqDKSq8zU5d\n" +
                        "LsFk9v8JL6aIRRlWgQkSrPzIiM6bgTVWXYr9zPyP059vP5FMkbcz0wn8wYDi+WHR\n" +
                        "YQJB4qoX9NIU7fXT2O3Atkm0lwvT8YHBPcdjkwYRsN/zY0hZEcX2SayduGS21s2l\n" +
                        "05HdBACMblactqIpQvebmVvPHYMLHD/OewIt1nPPJ9llmurHAnYJAIM1qpNBvDWD\n" +
                        "k4WFiwJiAjiC+vdyL+H00/1fcyyr2HUgSaGnY62NVqPtet/DHwJHncrlm9m3Rdtd\n" +
                        "AKHurpsn0jwbwVJgo0GXwhSMGDLTS9AluecdfGaLiPVTVNrpK/4DAwIx/5QqPY2D\n" +
                        "r2B3MJzmNzj7cOk9/9KnfSZXSD4EV98C1H/PkDZ7AncmvMzzGEyTrZmHMr6Uw1cZ\n" +
                        "IlGmrbQsVGVzdCBLZXkgKHRlc3QpIDxwcmludGRldkBhZmZpbmlvbmdyb3VwLmNv\n" +
                        "bT6IXgQTEQIAHgUCUBAftAIbAwYLCQgHAwIDFQIDAxYCAQIeAQIXgAAKCRAxle0m\n" +
                        "0/SSGLnkAJ4lzuwpPBc2NxQIe0kl6u1KvzXRWwCgri9l9TnGzv0qRc1Hx5my4MHL\n" +
                        "4wadAVgEUBAftRAEANhwMV7AGicNtmmB9zGSitpvmnX5VVv3aKzrJeB9lwhmAQ4M\n" +
                        "DtThdXrKPTfGwhpvjEYedqnABOMS4gml3QQer4lrP9dtmFkXdt0Znk+Kh6YOeL+G\n" +
                        "II/WeGo2GHr9GgTeIjmSDJIS8NiqzDMoUPB5I7n3dLxbBK4S+UOKPDnuHiLLAAMF\n" +
                        "A/0Y+pef4uxXLKndTT7Hs/jpmSGE5Bj+ECYeYlpUVTkaW7LyBcunct2MbwmsVSuF\n" +
                        "adVWPV709elbplaLNsjS0QHBvOktSnU9zyqojLEy5O+SRcl4h0HswciStefHxyes\n" +
                        "83ikM91APZ4ubkQSijLrYPJLvBGrmA+NwfJCA4zvEej0k/4DAwIx/5QqPY2Dr2DH\n" +
                        "k75OXTcV2OeEr3uASTZDh5beholJ+J9v5YuLXRwRcXAzCvI+Mk8Saaj1w15kNTFF\n" +
                        "9b9X5jBZsyjsecao1ohJBBgRAgAJBQJQEB+1AhsMAAoJEDGV7SbT9JIYjssAoKnK\n" +
                        "z0lVgZ/sF3L6saD79MF8r6SIAKDAsrtCxlsVFWt29Kc8Qfv7yzTkHg==\n" +
                        "=RvO1\n" +
                        "-----END PGP PRIVATE KEY BLOCK-----";
        String passphrase = "testkey";
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final PgpUtil.DecryptionMetadata meta = PgpUtil.decrypt(
                new ByteArrayInputStream(HexUtils.decodeBase64(dataStrb64)),
                new ByteArrayInputStream(privatekey.getBytes(Charsets.UTF8)),
                out,
                passphrase.toCharArray()
        );

        System.out.println("Hoorray for output: " + new String(out.toByteArray()));

    }

    // These tests are for new functionality added to the assertion, where the key AND IV can be explicitly defined
    // for decrypting purposes.
    // The Key and the IV are base 64 encoded strings.
    @Test
    public void testKeyIVDecryptionDirectly() throws Exception {
        // This is the test straight out of bugzilla (@bug 12998), and why this assertion was updated, along with the
        // creation of HexStringToBinaryBase64 Assertion...
        final String testData = "5tj9hw5sfVhpcc3x/v//QFlLw3vheTPzPyFO0uDXwVk=";
        final String compareData = "Encryption test\n";
        final String key = "tMIctaGqY9d1L/0+XFMWEQ=="; //"B4C21CB5A1AA63D7752FFD3E5C531611" as bytes and base64 encoded.
        final String iv = "kX+nuVNGUZCIlsmPZHS37A=="; // "917FA7B9534651908896C98F6474B7EC" as bytes and base64 encoded.
        final String encryption = "AES/CBC/PKCS5Padding";

        SymmetricKeyEncryptionDecryptionAssertion assertion = new SymmetricKeyEncryptionDecryptionAssertion();
        assertion.setAlgorithm(encryption);
        assertion.setIsEncrypt(false);
        assertion.setIv(iv);
        assertion.setKey(key);
        assertion.setText(testData);
        assertion.setOutputVariableName("testOutput");

        ServerSymmetricKeyEncryptionDecryptionAssertion serverAssertion = null;
        try {
            serverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(assertion, mockApplicationContext);
            AssertionStatus status = serverAssertion.checkRequest(mockPolicyEnforcementContext);
            String output = getOutputString("testOutput");

            // Remember, the output is still base64 encoded.  Decode it, as we know it contains a message.
            String decodedOutput = new String(HexUtils.decodeBase64(output));

            if (status != AssertionStatus.NONE) {
                fail("Assertion has failed internally");
            }

            if (!decodedOutput.equals(compareData)) {
                fail("Decryption with key and IV did not produce the original data pre-encryption\n"+
                        "Should Be: ["+HexUtils.hexDump(compareData.getBytes())+"]\n"+
                        "  But Got: ["+HexUtils.hexDump(decodedOutput.getBytes())+"]");
            }
        } catch (PolicyAssertionException e) {
            fail("Unable to initialize server assertion for testing");
        } catch (NoSuchVariableException e) {
            fail("Test has failed, unable to find output variable in PEC");
        } catch (IOException e) {
            fail("An exception has been thrown which prevents this test from continuing");
        }
    }

    @Ignore
    @Test
    public void testKeyIVDecryptionViaVariable() throws Exception {
        // This is the test straight out of bugzilla (@bug 12998), and why this assertion was updated, along with the
        // creation of HexStringToBinaryBase64 Assertion...
        final String testData = "5tj9hw5sfVhpcc3x/v//QFlLw3vheTPzPyFO0uDXwVk=";
        final String compareData = "Encryption test\n";
        final String key = "tMIctaGqY9d1L/0+XFMWEQ=="; //"B4C21CB5A1AA63D7752FFD3E5C531611" as bytes and base64 encoded.
        final String iv = "kX+nuVNGUZCIlsmPZHS37A=="; // "917FA7B9534651908896C98F6474B7EC" as bytes and base64 encoded.
        final String encryption = "AES/CBC/PKCS5Padding";

        Map<String,Object> variableMap = new HashMap<>();
        variableMap.put("key", key);
        variableMap.put("iv", iv);
        variableMap.put("testData",testData);

        when(mockPolicyEnforcementContext.getVariableMap(any(String[].class), any(Audit.class))).thenReturn(variableMap);

        //mockPolicyEnforcementContext.setVariable("testData", testData);
        //mockPolicyEnforcementContext.setVariable("key", key);
        //mockPolicyEnforcementContext.setVariable("iv", iv);

        SymmetricKeyEncryptionDecryptionAssertion assertion = new SymmetricKeyEncryptionDecryptionAssertion();
        assertion.setAlgorithm(encryption);
        assertion.setIsEncrypt(false);
        assertion.setIv("${iv}");
        assertion.setKey("${key}");
        assertion.setText("testData");
        assertion.setOutputVariableName("testOutput");

        ServerSymmetricKeyEncryptionDecryptionAssertion serverAssertion = null;
        try {
            serverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(assertion, mockApplicationContext);
            AssertionStatus status = serverAssertion.checkRequest(mockPolicyEnforcementContext);
            String output = getOutputString("testOutput");
            //(String) mockPolicyEnforcementContext.getVariable("testOutput");

            // Remember, the output is still base64 encoded.  Decode it, as we know it contains a message.
            String decodedOutput = new String(HexUtils.decodeBase64(output));

            if (status != AssertionStatus.NONE) {
                fail("Assertion has failed internally");
            }

            if (!decodedOutput.equals(compareData)) {
                fail("Decryption with key and IV did not produce the original data pre-encryption\n"+
                        "Should Be: ["+HexUtils.hexDump(compareData.getBytes())+"]\n"+
                        "  But Got: ["+HexUtils.hexDump(decodedOutput.getBytes())+"]");
            }
        } catch (PolicyAssertionException e) {
            fail("Unable to initialize server assertion for testing");
        } catch (NoSuchVariableException e) {
            fail("Test has failed, unable to find output variable in PEC");
        } catch (IOException e) {
            fail("An exception has been thrown which prevents this test from continuing");
        }
    }

    @Test
    public void testAesGcmNoPaddingRoundTrip() throws Exception {
        // Setup test data.
        String textToSign = "hello world";
        byte[] key = new byte[128/8]; // 128 bit key.
        new Random().nextBytes(key);
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_GCM_NOPADDING;
        String outVariableName = "encrypted";

        // Encrypt.
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(textToSign.getBytes(Charsets.UTF8)), HexUtils.encodeBase64(key), outVariableName, algorithm, true, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Encryption failed.", AssertionStatus.NONE, status);

        String encryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Cipher text is empty.", encryptedB64.length() > 0);

        // Decrypt
        outVariableName = "decrypted";
        ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, encryptedB64, HexUtils.encodeBase64(key), outVariableName, algorithm, false, null);

        sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Decryption failed.", AssertionStatus.NONE, status);

        String decryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Decrypted text is empty.", decryptedB64.length() > 0);

        // Check decrypted text matches original text.
        String decryptedText = new String(HexUtils.decodeBase64(decryptedB64));
        Assert.assertEquals(textToSign, decryptedText);
    }

    @Test
    public void testAesGcmNoPaddingRoundTripIvProvided() throws Exception {
        // Setup test data.
        String textToSign = "hello world";
        byte[] key = new byte[128/8]; // 128 bit key.
        new Random().nextBytes(key);
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_GCM_NOPADDING;
        String outVariableName = "encrypted";

        // Encrypt.
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(textToSign.getBytes(Charsets.UTF8)), HexUtils.encodeBase64(key), outVariableName, algorithm, true, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Encryption failed.", AssertionStatus.NONE, status);

        String encryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Cipher text is empty.", encryptedB64.length() > 0);
        byte[] encryptedBytes = HexUtils.decodeBase64(encryptedB64);

        // Split IV and cipher text from encrypted result.
        // By default, IV is 12 bytes for GCM block mode.
        int ivBytesSize = 12;
        Assert.assertTrue("Cipher text is too short.", encryptedBytes.length > ivBytesSize);

        byte[] ivBytes = new byte[ivBytesSize];
        System.arraycopy(encryptedBytes, 0, ivBytes, 0, ivBytesSize);
        byte[] cipherTextBytes = new byte[encryptedBytes.length - ivBytesSize];
        System.arraycopy(encryptedBytes, ivBytesSize, cipherTextBytes, 0, encryptedBytes.length - ivBytesSize);

        // Decrypt using provided IV
        outVariableName = "decrypted";
        ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(cipherTextBytes), HexUtils.encodeBase64(key), outVariableName, algorithm, false, null);
        ass.setIv(HexUtils.encodeBase64(ivBytes));

        sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Decryption failed.", AssertionStatus.NONE, status);

        String decryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Decrypted text is empty.", decryptedB64.length() > 0);

        // Check decrypted text matches original text.
        String decryptedText = new String(HexUtils.decodeBase64(decryptedB64));
        Assert.assertEquals(textToSign, decryptedText);
    }

    /*
     * Attempt to decrypt using wrong key.
     */
    @Test
    public void testAesGcmNoPaddingRoundTripFailure() throws Exception {
        // Setup test data.
        String textToSign = "hello world";
        byte[] key = new byte[128/8]; // 128 bit key.
        new Random().nextBytes(key);
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_GCM_NOPADDING;
        String outVariableName = "encrypted";

        // Encrypt.
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(textToSign.getBytes(Charsets.UTF8)), HexUtils.encodeBase64(key), outVariableName, algorithm, true, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Encryption failed.", AssertionStatus.NONE, status);

        String encryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Cipher text is empty.", encryptedB64.length() > 0);

        // Decrypt using a wrong key
        new Random().nextBytes(key);
        outVariableName = "decrypted";
        ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, encryptedB64, HexUtils.encodeBase64(key), outVariableName, algorithm, false, null);

        sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Decryption should fail!.", AssertionStatus.FAILED, status);
    }

    /*
     * Attempt to decrypt using wrong IV.
     */
    @Test
    public void testAesGcmNoPaddingRoundTripIvProvidedFailure() throws Exception {
        // Setup test data.
        String textToSign = "hello world";
        byte[] key = new byte[128/8]; // 128 bit key.
        new Random().nextBytes(key);
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_GCM_NOPADDING;
        String outVariableName = "encrypted";

        // Encrypt.
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(textToSign.getBytes(Charsets.UTF8)), HexUtils.encodeBase64(key), outVariableName, algorithm, true, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Encryption failed.", AssertionStatus.NONE, status);

        String encryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Cipher text is empty.", encryptedB64.length() > 0);
        byte[] encryptedBytes = HexUtils.decodeBase64(encryptedB64);

        // Split IV and cipher text from encrypted result.
        // By default, IV is 12 bytes for GCM block mode.
        int ivBytesSize = 12;
        Assert.assertTrue("Cipher text is too short.", encryptedBytes.length > ivBytesSize);

        // Decrypt using a wrong IV
        byte[] ivBytes = new byte[ivBytesSize];
        new Random().nextBytes(ivBytes);
        byte[] cipherTextBytes = new byte[encryptedBytes.length - ivBytesSize];
        System.arraycopy(encryptedBytes, ivBytesSize, cipherTextBytes, 0, encryptedBytes.length - ivBytesSize);

        // Decrypt using provided IV
        outVariableName = "decrypted";
        ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(cipherTextBytes), HexUtils.encodeBase64(key), outVariableName, algorithm, false, null);
        ass.setIv(HexUtils.encodeBase64(ivBytes));

        sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Decryption should fail!.", AssertionStatus.FAILED, status);
    }

    @Test
    public void testAesEcbPkcs5Padding() throws Exception {
        // Setup test data.
        String textToSign = "hello world";
        byte[] key = new byte[128/8]; // 128 bit key.
        new Random().nextBytes(key);
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_ECB_PKCS5PADDING;
        String outVariableName = "encrypted";

        // Encrypt.
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(textToSign.getBytes(Charsets.UTF8)), HexUtils.encodeBase64(key), outVariableName, algorithm, true, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Encryption failed.", AssertionStatus.NONE, status);

        String encryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Cipher text is empty.", encryptedB64.length() > 0);

        // Decrypt
        outVariableName = "decrypted";
        ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, encryptedB64, HexUtils.encodeBase64(key), outVariableName, algorithm, false, null);

        sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Decryption failed.", AssertionStatus.NONE, status);

        String decryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Decrypted text is empty.", decryptedB64.length() > 0);

        // Check decrypted text matches original text.
        String decryptedText = new String(HexUtils.decodeBase64(decryptedB64));
        Assert.assertEquals(textToSign, decryptedText);
    }

    @Test
    public void testAesEcbPkcs7Padding() throws Exception {
        // Setup test data.
        String textToSign = "hello world";
        byte[] key = new byte[128/8]; // 128 bit key.
        new Random().nextBytes(key);
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_ECB_PKCS7PADDING;
        String outVariableName = "encrypted";

        // Encrypt.
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(textToSign.getBytes(Charsets.UTF8)), HexUtils.encodeBase64(key), outVariableName, algorithm, true, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Encryption failed.", AssertionStatus.NONE, status);

        String encryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Cipher text is empty.", encryptedB64.length() > 0);

        // Decrypt
        outVariableName = "decrypted";
        ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, encryptedB64, HexUtils.encodeBase64(key), outVariableName, algorithm, false, null);

        sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Decryption failed.", AssertionStatus.NONE, status);

        String decryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Decrypted text is empty.", decryptedB64.length() > 0);

        // Check decrypted text matches original text.
        String decryptedText = new String(HexUtils.decodeBase64(decryptedB64));
        Assert.assertEquals(textToSign, decryptedText);
    }

    @Test
    @BugId("DE360294")
    public void testAESECB192Test() throws Exception {
        final String TEXT = "This is my text to encrypt and decrypt using the AES/ECB/PKCS5Padding";
        final String ALGORITHM = "AES/ECB/PKCS5Padding";
        final String KEY_192BIT = "thisour192bitkey192keybi";

        final String ENCRYPT_VAR = "192encryptoutputtest";
        final String DECRYPT_VAR = "192decryptoutputtest";

        aesTestHelper(TEXT, ALGORITHM, KEY_192BIT, ENCRYPT_VAR, DECRYPT_VAR);
    }

    @Test
    @BugId("DE360294")
    public void testAESECB256Test() throws Exception {
        final String TEXT = "This is my text to encrypt and decrypt using the AES/ECB/PKCS5Padding";
        final String ALGORITHM = "AES/ECB/PKCS5Padding";
        final String KEY_256BIT = "thisour192bitkey192keybit256bitk";

        final String ENCRYPT_VAR = "256encryptoutputtest";
        final String DECRYPT_VAR = "256decryptoutputtest";

        aesTestHelper(TEXT, ALGORITHM, KEY_256BIT, ENCRYPT_VAR, DECRYPT_VAR);
    }


    private void aesTestHelper(String text, String algorithm, String key, String encryptVariable, String decryptVariable) throws Exception {
        SymmetricKeyEncryptionDecryptionAssertion encryptAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String keyB64 = HexUtils.encodeBase64(key.getBytes(Charsets.UTF8));
        setUpAssertion(encryptAssertion, b64encodedText, keyB64, encryptVariable, algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptServerAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptAssertion, mockApplicationContext);
        AssertionStatus status = encryptServerAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(encryptVariable);
        Assert.assertNotNull(status);
        Assert.assertEquals("Assertion Status should be NONE", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Ensure cipher is produced", output.length() > 0);

        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, keyB64, decryptVariable, algorithm, false,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptServerAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptServerAssertion.checkRequest(mockPolicyEnforcementContext);
        String finalOutput = getOutputString(decryptVariable);

        Assert.assertNotNull(status);
        Assert.assertEquals("Assertion Status should be NONE", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Ensure output matches the original text", finalOutput, b64encodedText);
    }

    @Test
    public void testAesEcbPkcs5PaddingDecrypt() throws Exception {
        // Setup test data.
        byte[] key = "PUdp6bksv1DngBRK".getBytes();
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_ECB_PKCS5PADDING;
        String outVariableName = "encrypted";
        String encryptedB64 = "kcZAP7iML4t/K/kSoKe6sEX/rMzHmLbHvl/MwrSate0=";

        // Decrypt
        outVariableName = "decrypted";
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, encryptedB64, HexUtils.encodeBase64(key), outVariableName, algorithm, false, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass =
                new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Decryption failed.", AssertionStatus.NONE, status);

        String decryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Decrypted text is empty.", decryptedB64.length() > 0);

        // Check decrypted text matches original text.
        String decryptedText = new String(HexUtils.decodeBase64(decryptedB64));
        Assert.assertEquals("This is my text to encrypt", decryptedText);
    }

    @Test
    public void testAesEcbPkcs5PaddingEncrypt() throws Exception {
        // Setup test data.
        byte[] key = "PUdp6bksv1DngBRK".getBytes();
        String algorithm = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_ECB_PKCS5PADDING;
        String outVariableName = "encrypted";
        String textToSign = "This is my text to encrypt";

        // Encrypt.
        SymmetricKeyEncryptionDecryptionAssertion ass = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(ass, HexUtils.encodeBase64(textToSign.getBytes(Charsets.UTF8)), HexUtils.encodeBase64(key), outVariableName, algorithm, true, null);

        ServerSymmetricKeyEncryptionDecryptionAssertion sass = new ServerSymmetricKeyEncryptionDecryptionAssertion(ass, mockApplicationContext);
        AssertionStatus status = sass.checkRequest(mockPolicyEnforcementContext);
        Assert.assertEquals("Encryption failed.", AssertionStatus.NONE, status);

        String encryptedB64 = getOutputString(outVariableName);
        Assert.assertTrue("Cipher text is empty.", encryptedB64.length() > 0);

        String decrypted = decrypt(encryptedB64, HexUtils.encodeBase64(key));
        Assert.assertEquals("Encrypt validation failed.", textToSign, decrypted);
    }

    @Test
    public void testAES_CBC_PKCS7Padding() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "happypathtest";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "AES/CBC/PKCS7Padding";
        String _128bitkey = "thisour128bitkey";
        String _128bitkeyB64 = HexUtils.encodeBase64(_128bitkey.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, _128bitkeyB64, variableName, Algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, _128bitkeyB64, decryptVariableName, Algorithm, false, "");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);


        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);
    }

    @Test
    public void testPGP_AES256() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "This is what I am encrypt";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "PGP/AES256";
        String passphrase = "my7layer";
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, "", variableName, Algorithm, true,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        System.out.println("interimn Output: " + output);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, "", decryptVariableName, Algorithm, false,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);

        System.out.println("Final Output: " + finaloutput + "   B64Decoded: " + new String(HexUtils.decodeBase64(finaloutput)));
    }

    @Test
    public void testPGP_CAST5() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "This is what I am encrypt";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "PGP/CAST5";
        String passphrase = "my7layer";
        String passphraseB64 = HexUtils.encodeBase64(passphrase.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, "", variableName, Algorithm, true,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        System.out.println("interimn Output: " + output);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, "", decryptVariableName, Algorithm, false,passphraseB64);

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);

        System.out.println("Final Output: " + finaloutput + "   B64Decoded: " + new String(HexUtils.decodeBase64(finaloutput)));
    }

    @Test
    public void test3DES_ECBMode() throws Exception {

        SymmetricKeyEncryptionDecryptionAssertion encryptassertion = new SymmetricKeyEncryptionDecryptionAssertion();
        String text = "happypathtest";
        String b64encodedText = HexUtils.encodeBase64(text.getBytes(Charsets.UTF8));
        String Algorithm = "DESede/ECB/PKCS5Padding";
        String _key = JceProvider.getInstance().isFips140ModeEnabled()? "thisour1thisour1thisour1" : "thisour1";
        String _keyB64 = HexUtils.encodeBase64(_key.getBytes(Charsets.UTF8));
        String variableName = "128bitencryptoutputtest";
        setUpAssertion(encryptassertion, b64encodedText, _keyB64, variableName, Algorithm, true,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion encryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(encryptassertion, mockApplicationContext);
        AssertionStatus status = encryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String output = getOutputString(variableName);
        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertTrue("Second Check - Make sure a cipher is produced", (output).length() > 0);

        String decryptVariableName = "128bitdecryptoutputtest";
        SymmetricKeyEncryptionDecryptionAssertion decryptionAssertion = new SymmetricKeyEncryptionDecryptionAssertion();
        setUpAssertion(decryptionAssertion, output, _keyB64, decryptVariableName, Algorithm, false,"");

        ServerSymmetricKeyEncryptionDecryptionAssertion decryptserverAssertion = new ServerSymmetricKeyEncryptionDecryptionAssertion(decryptionAssertion, mockApplicationContext);
        status = decryptserverAssertion.checkRequest(mockPolicyEnforcementContext);
        String finaloutput = getOutputString(decryptVariableName);

        Assert.assertNotNull(status);
        Assert.assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());
        Assert.assertEquals("Second Check  - Make sure output matches the original text", finaloutput, b64encodedText);
    }

    private String decrypt(String input, String key) {
        byte[] output = null;
        try {
            SecretKeySpec skey = new SecretKeySpec(HexUtils.decodeBase64(key), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skey);
            output = cipher.doFinal(HexUtils.decodeBase64(input));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return new String(output);
    }

    /**
     * Helper method to retrieve the output target from the context after it's been set
     * @return String
     * @throws Exception
     */
    private String getOutputString(String var) throws Exception {
        // Capture the output of the serverAssertion processing.
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockPolicyEnforcementContext).setVariable(eq(var), argument.capture());
        return argument.getValue();
    }

}
