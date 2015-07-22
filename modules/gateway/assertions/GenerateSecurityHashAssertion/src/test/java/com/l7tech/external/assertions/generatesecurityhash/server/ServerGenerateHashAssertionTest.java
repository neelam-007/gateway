package com.l7tech.external.assertions.generatesecurityhash.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.generatesecurityhash.GenerateSecurityHashAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link ServerGenerateSecurityHashAssertion}.
 *
 * @author KDiep
 */
public class ServerGenerateHashAssertionTest {

    private static final String TEST_DATA_CONTEXT_VARIABLE = "input.data";
    private static final String TEST_KEY = "7dc51fcc-5103-4e5e-80df-525acaa4b8f4";
    private static final byte[] TEST_KEY_BINARY = TEST_KEY.getBytes( Charsets.UTF8 );
    private static final String TEST_DATA = "http://sarek/mediawiki/index.php?title=REST_API_Security_Using_HMAC";
    private static final String OUTPUT_VARIABLE = "hash.output";

    private static final Map<String, String> BINARY_DIGEST_TEST_DATA;
    private static final byte[] TEST_DATA_BINARY;
    static {
        try {
            TEST_DATA_BINARY = HexUtils.unHexDump( "0038BEFF00010477EFDD0000" );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        Map<String, String> m = new HashMap<String, String>();
        m.put( "MD5", HexUtils.encodeBase64( HexUtils.getMd5Digest( TEST_DATA_BINARY ), true ) );
        m.put( "SHA-1", HexUtils.encodeBase64( HexUtils.getSha1Digest( TEST_DATA_BINARY ), true ) );
        m.put( "SHA-256", HexUtils.encodeBase64( HexUtils.getSha256Digest( TEST_DATA_BINARY ), true ) );
        m.put( "SHA-384", "wF0j1DWao7wDHW1Tjto8HuiaGmYMukI6mY1s93aRdsr2iNwg+M6bV1S8lKOZPWwl" );
        m.put( "SHA-512", HexUtils.encodeBase64( HexUtils.getSha512Digest( TEST_DATA_BINARY ), true ) );
        BINARY_DIGEST_TEST_DATA = Collections.unmodifiableMap( m );
    }

    private static final Map<String, String> DIGEST_TEST_DATA;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("MD5", "rk2SOJhCmltGaM88hOhsaQ==");
        m.put("SHA-1", "//9wDRb9zxyC0IelpxZMxFNN184=");
        m.put("SHA-256", "+RVTTZdJENfVizXIQQFkBAAhNAlUaa/ouAumMG6nnJ0=");
        m.put("SHA-384", "D651pSSgkhL9MQeDrVpffHJibxDPkBV9hn9MSCAqZyFqQH++TuJ0D/TS2SQBgD0C");
        m.put("SHA-512", "EodHHxZAQ+2rQyx6MvgIg0bPkVuD8OPcGuOBLGCQPgE2n6GozBcCq6vXXj2l2FgTP9rLWfH1y2cgCvnFyXnMpg==");
        DIGEST_TEST_DATA = Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> HMAC_TEST_DATA;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("HMAC-SHA1", "RMCoOsTpDl/a8TMyufhEj6ZAgmY=");
        m.put("HMAC-SHA256", "Nk+RBtl7k9/4tpYIkuR0iIIhPYKq553npC9RW+FqDfk=");
        m.put("HMAC-SHA384", "SWba2s8fj6C4OcYjCcT7yIhFqwIc8eVZUEaxBFCDztY7gbAmuNEXE4EhotMshp1V");
        m.put("HMAC-SHA512", "7AP4/Qlj3H9ZPWjOkF3v4neKdug894/v7PUh0pzE8u/liPJBioLnizXnHWn7nF9rDGWGS2yocp1HUdoLf2l9Mg==");
        HMAC_TEST_DATA = Collections.unmodifiableMap(m);
    }

    private GenerateSecurityHashAssertion assertion;

    private PolicyEnforcementContext pec;

    private ServerGenerateSecurityHashAssertion serverAssertion;

    @Before
    public void setUp(){
        assertion = new GenerateSecurityHashAssertion();
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<request />"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<response />"));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private void setTestData(final String algorithm, final String key, final String data, final String outputVariable ) {
        assertion.setAlgorithm(algorithm);
        assertion.setKeyText(key);
        assertion.setDataToSignText(data);
        assertion.setTargetOutputVariable(outputVariable);
        serverAssertion = new ServerGenerateSecurityHashAssertion(assertion);
    }

    @Test
    public void testMissingVariables(){
        try {
            setTestData("", "", "", "");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            assertEquals( AssertionStatus.FAILED, actual );
        } catch (Exception e) {
            Assert.fail("testMissingVariables() failed: " + e.getMessage());
        }
    }

    @Test
    public void testHmacMissingKeyVariable(){
        try {
            setTestData("HmacSHA384", "", "ggasdfg", "gggg");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            assertEquals( AssertionStatus.FAILED, actual );
        } catch (Exception e) {
            Assert.fail("testMissingKeyVariable() failed: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidAlgorithm(){
        try {
            setTestData("7layer", "mykey", "ggasdfg", "gggg");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            assertEquals( AssertionStatus.FAILED, actual );
        } catch (Exception e) {
            Assert.fail("testInvalidAlgorithm() failed: " + e.getMessage());
        }
    }

    @Test
    public void testDigest(){
        for(Map.Entry<String, String> ent : DIGEST_TEST_DATA.entrySet()){
            try {
                setTestData(ent.getKey(), "", TEST_DATA, OUTPUT_VARIABLE);
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " failed.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testDigest() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Test
    public void testDigestFromContextVariable(){
        for(Map.Entry<String, String> ent : DIGEST_TEST_DATA.entrySet()){
            try {
                pec.setVariable(TEST_DATA_CONTEXT_VARIABLE, TEST_DATA);
                setTestData(ent.getKey(), "", "${" + TEST_DATA_CONTEXT_VARIABLE + "}", OUTPUT_VARIABLE);
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " failed.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testDigestFromContextVariable() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testHmac(){
        for(Map.Entry<String, String> ent : HMAC_TEST_DATA.entrySet()){
            try {
                setTestData(ent.getKey(), TEST_KEY, TEST_DATA, OUTPUT_VARIABLE);
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " failed.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testHmac() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Test
    public void testHmac_binaryKey(){
        for(Map.Entry<String, String> ent : HMAC_TEST_DATA.entrySet()){
            try {
                assertion.setAlgorithm( ent.getKey() );
                assertion.setKeyText( "${binaryKey}" );
                assertion.setDataToSignText( TEST_DATA );
                assertion.setTargetOutputVariable( OUTPUT_VARIABLE );
                serverAssertion = new ServerGenerateSecurityHashAssertion(assertion);
                pec.setVariable( "binaryKey", TEST_KEY_BINARY );
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " failed.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testHmac() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Test
    public void testHmacFromContextVariable(){
        for(Map.Entry<String, String> ent : HMAC_TEST_DATA.entrySet()){
            try {
                pec.setVariable(TEST_DATA_CONTEXT_VARIABLE, TEST_DATA);
                setTestData(ent.getKey(), TEST_KEY, "${" + TEST_DATA_CONTEXT_VARIABLE + "}", OUTPUT_VARIABLE);
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " failed.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testHmacFromContextVariable() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Test
    @BugId( "SSG-9126" )
    public void testHashFromByteArrayContextVariable() {
        for(Map.Entry<String, String> ent : BINARY_DIGEST_TEST_DATA.entrySet()){
            try {
                byte[] dataToSignBytes = TEST_DATA_BINARY;
                pec.setVariable( TEST_DATA_CONTEXT_VARIABLE, dataToSignBytes );
                assertion.setAlgorithm( ent.getKey() );
                assertion.setKeyText( "" );
                assertion.setDataToSignText( "${" + TEST_DATA_CONTEXT_VARIABLE + "}" );
                assertion.setTargetOutputVariable( OUTPUT_VARIABLE );
                serverAssertion = new ServerGenerateSecurityHashAssertion(assertion);
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( ent.getKey() + " assertion failed.", AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " hash value mismatch.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testDigest() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Test
    @BugId( "SSG-9126" )
    public void testHashFromMessageContextVariable() {
        for(Map.Entry<String, String> ent : BINARY_DIGEST_TEST_DATA.entrySet()){
            try {
                Message dataToSignMessage = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT,
                        new ByteArrayInputStream( TEST_DATA_BINARY ) );
                pec.setVariable( TEST_DATA_CONTEXT_VARIABLE, dataToSignMessage );
                assertion.setAlgorithm( ent.getKey() );
                assertion.setKeyText( "" );
                assertion.setDataToSignText( "${" + TEST_DATA_CONTEXT_VARIABLE + "}" );
                assertion.setTargetOutputVariable( OUTPUT_VARIABLE );
                serverAssertion = new ServerGenerateSecurityHashAssertion(assertion);
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( ent.getKey() + " assertion failed.", AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " hash value mismatch.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testDigest() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Test
    @BugId( "SSG-9126" )
    public void testHashFromMessagePartContextVariable() {
        for(Map.Entry<String, String> ent : BINARY_DIGEST_TEST_DATA.entrySet()){
            try {
                Message dataToSignMessage = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT,
                        new ByteArrayInputStream( TEST_DATA_BINARY ) );
                PartInfo dataToSignMessagePart = dataToSignMessage.getMimeKnob().getFirstPart();
                pec.setVariable( TEST_DATA_CONTEXT_VARIABLE, dataToSignMessagePart );
                assertion.setAlgorithm( ent.getKey() );
                assertion.setKeyText( "" );
                assertion.setDataToSignText( "${" + TEST_DATA_CONTEXT_VARIABLE + "}" );
                assertion.setTargetOutputVariable( OUTPUT_VARIABLE );
                serverAssertion = new ServerGenerateSecurityHashAssertion(assertion);
                AssertionStatus actual = serverAssertion.checkRequest(pec);
                assertEquals( ent.getKey() + " assertion failed.", AssertionStatus.NONE, actual );
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                assertEquals( ent.getKey() + " hash value mismatch.", ent.getValue(), actualSignature );
            } catch (Exception e) {
                Assert.fail("testDigest() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Test
    @BugId( "SSG-11577" )
    public void testEmptyContextVariablesInConstructor() throws Exception {
        AssertionStatus result = new ServerGenerateSecurityHashAssertion( new GenerateSecurityHashAssertion() ).checkRequest( pec );
        assertEquals( AssertionStatus.FAILED, result );
    }

    @Test
    @BugId( "SSG-11577" )
    public void testEmptyKeyVariablesInConstructor() throws Exception {
        GenerateSecurityHashAssertion ass = new GenerateSecurityHashAssertion();
        ass.setDataToSignText( "blah" );
        ass.setAlgorithm( "HMAC-SHA256" );
        AssertionStatus result = new ServerGenerateSecurityHashAssertion( ass ).checkRequest( pec );
        assertEquals( AssertionStatus.FAILED, result );
    }
}
