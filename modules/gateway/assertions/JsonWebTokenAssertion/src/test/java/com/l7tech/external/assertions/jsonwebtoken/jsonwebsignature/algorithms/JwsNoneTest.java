package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JsonWebToken;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.json.InvalidJsonException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: rseminoff
 * Date: 03/12/12
 */
public class JwsNoneTest {

    private JsonWebToken jwt;
    private byte[] token;

    @Before
    public void setup() throws IllegalJwtSignatureException {
        jwt = JsonWebToken.initEmptyToken();   // Used for validation only.
        jwt.setSignatureAlgorithmByName(JwsNone.jwsAlgorithmName);
        jwt.setSecret((String) null, false);

        token = null;
    }

    // This test will perform a normal signing with a header and payload.
    // Secret isn't passed as it's not used anyhow
    // Then the signed data is validated.  It should pass as it has all
    // proper values
    @Test
    public void testSigning_1() throws IllegalJwtSignatureException {

        // The token creation shouldn't fail, as all parts are set.
        try {
            jwt.setPayload("{\"testPayload\":\"aS91Dkf912_9123asdjcasfFL(283LADwe94j)\"}");
            token = jwt.getToken();
        } catch (InvalidJsonException e) {
            fail("The payload is not valid JSON, but it is legal JSON.  This is a failure case.");
        }

        // This test is largely symbolic and should be used as part of a template
        // for testing other signature types.
        // The None Signature doesn't validate, as there's no signature to validate.

        // We 'cheat' here by using the JsonWebToken validateReceivedToken method
        // and pass the token.
        // This is done because the code to split and decode the token is contained
        // within JsonWebToken, and doesn't need to be duplicated here.

        // The validateReceivedToken method calls the validation within the main module
        // anyhow, so if this was to fail, this is a great way to show it does.
        assertTrue("The token just created and signed has failed to validate. (Proper Token)", jwt.validateReceivedTokenNoSecret(token));
    }

    // This test will perform a normal signing with a header but no payload
    // Secret isn't passed as it's not used anyhow
    @Test(expected = IllegalArgumentException.class)
    public void testSigning_2() throws InvalidJsonException {

        // This test "forgets" to pass a payload.  It should fail.
        // In this case, we pass a space as the JsonWebToken doesn't allow
        // Null or empty strings for payloads.
        jwt.setPayload(" ");
        fail("The payload was set to an empty payload.  Fail");
    }

    // This test will perform a normal signing with a header but no payload
    // Secret isn't passed as it's not used anyhow
    @Test(expected = IllegalJwtSignatureException.class)
    public void testSigning_3() throws IllegalJwtSignatureException {

        // This test "forgets" to set a signature type.  It should fail.
        jwt.setSignatureAlgorithmByName("");
        fail("An empty string signature algorithm was passed and accepted. (No Signature Algorithm)");
    }

}
