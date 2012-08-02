package com.l7tech.external.assertions.generateoauthsignaturebasestring;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GenerateOAuthSignatureBaseStringTest {
    private GenerateOAuthSignatureBaseStringAssertion.NodeNameFactory nodeNameFactory;
    private GenerateOAuthSignatureBaseStringAssertion assertion;

    @Before
    public void setup(){
        nodeNameFactory = new GenerateOAuthSignatureBaseStringAssertion.NodeNameFactory();
        assertion = new GenerateOAuthSignatureBaseStringAssertion();
    }

    @Test
    public void getAssertionNameServer(){
        assertion.setUsageMode(GenerateOAuthSignatureBaseStringAssertion.UsageMode.SERVER);

        final String assertionName = nodeNameFactory.getAssertionName(assertion, true);

        assertEquals("Server Generate OAuth Signature Base String", assertionName);
    }

    @Test
    public void getAssertionNameClientAuthorizedRequestToken(){
        assertion.setUsageMode(GenerateOAuthSignatureBaseStringAssertion.UsageMode.CLIENT);
        assertion.setOauthToken("token");
        assertion.setOauthVerifier("verifier");

        final String assertionName = nodeNameFactory.getAssertionName(assertion, true);

        assertEquals("Client Generate OAuth Signature Base String: authorized request token", assertionName);
    }

    @Test
    public void getAssertionNameClientAccessToken(){
        assertion.setUsageMode(GenerateOAuthSignatureBaseStringAssertion.UsageMode.CLIENT);
        assertion.setOauthToken("token");
        assertion.setOauthVerifier(null);

        final String assertionName = nodeNameFactory.getAssertionName(assertion, true);

        assertEquals("Client Generate OAuth Signature Base String: access token", assertionName);
    }

    @Test
    public void getAssertionNameClienReqestToken(){
        assertion.setUsageMode(GenerateOAuthSignatureBaseStringAssertion.UsageMode.CLIENT);
        assertion.setOauthToken(null);
        assertion.setOauthVerifier(null);

        final String assertionName = nodeNameFactory.getAssertionName(assertion, true);

        assertEquals("Client Generate OAuth Signature Base String: request token", assertionName);
    }
}
