package com.l7tech.external.assertions.generatehash.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.generatehash.GenerateHashAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test cases for {@link ServerGenerateHashAssertion}.
 *
 * @author KDiep
 */
public class ServerGenerateHashAssertionTest {

    private static final String TEST_DATA_CONTEXT_VARIABLE = "input.data";
    private static final String TEST_KEY = "7dc51fcc-5103-4e5e-80df-525acaa4b8f4";
    private static final String TEST_DATA = "http://sarek/mediawiki/index.php?title=REST_API_Security_Using_HMAC";
    private static final String OUTPUT_VARIABLE = "hash.output";

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
        m.put("HmacSHA1", "RMCoOsTpDl/a8TMyufhEj6ZAgmY=");
        m.put("HmacSHA256", "Nk+RBtl7k9/4tpYIkuR0iIIhPYKq553npC9RW+FqDfk=");
        m.put("HmacSHA384", "SWba2s8fj6C4OcYjCcT7yIhFqwIc8eVZUEaxBFCDztY7gbAmuNEXE4EhotMshp1V");
        m.put("HmacSHA512", "7AP4/Qlj3H9ZPWjOkF3v4neKdug894/v7PUh0pzE8u/liPJBioLnizXnHWn7nF9rDGWGS2yocp1HUdoLf2l9Mg==");
        HMAC_TEST_DATA = Collections.unmodifiableMap(m);
    }

    private GenerateHashAssertion assertion;

    private PolicyEnforcementContext pec;

    private ServerGenerateHashAssertion serverAssertion;

    @Before
    public void setUp(){
        assertion = new GenerateHashAssertion();
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<request />"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<response />"));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        serverAssertion = new ServerGenerateHashAssertion(assertion);
    }

    private void setTestData(final String algorithm, final String key, final String data, final String outputVariable ) {
        assertion.setAlgorithm(algorithm);
        assertion.setKeyText(key);
        assertion.setDataToSignText(data);
        assertion.setTargetOutputVariable(outputVariable);
    }

    @Test
    public void testMissingVariables(){
        try {
            setTestData("", "", "", "");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, actual);
        } catch (Exception e) {
            Assert.fail("testMissingVariables() failed: " + e.getMessage());
        }
    }

    @Test
    public void testHmacMissingKeyVariable(){
        try {
            setTestData("HmacSHA384", "", "ggasdfg", "gggg");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, actual);
        } catch (Exception e) {
            Assert.fail("testMissingKeyVariable() failed: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidAlgorithm(){
        try {
            setTestData("7layer", "mykey", "ggasdfg", "gggg");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, actual);
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
                Assert.assertEquals(AssertionStatus.NONE, actual);
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                Assert.assertEquals(ent.getKey() + " failed.", ent.getValue(), actualSignature);
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
                Assert.assertEquals(AssertionStatus.NONE, actual);
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                Assert.assertEquals(ent.getKey() + " failed.", ent.getValue(), actualSignature);
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
                Assert.assertEquals(AssertionStatus.NONE, actual);
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                Assert.assertEquals(ent.getKey() + " failed.", ent.getValue(), actualSignature);
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
                Assert.assertEquals(AssertionStatus.NONE, actual);
                String actualSignature = pec.getVariable(OUTPUT_VARIABLE).toString();
                Assert.assertEquals(ent.getKey() + " failed.", ent.getValue(), actualSignature);
            } catch (Exception e) {
                Assert.fail("testHmacFromContextVariable() failed (" + ent.getKey() + "): " + e.getMessage());
            }
        }
    }
}
