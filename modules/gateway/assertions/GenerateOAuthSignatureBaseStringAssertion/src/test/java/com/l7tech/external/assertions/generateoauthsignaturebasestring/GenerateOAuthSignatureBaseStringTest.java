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
    public void getAssertionNameDoNotDecorate(){
        assertion.setUsageMode(GenerateOAuthSignatureBaseStringAssertion.UsageMode.SERVER);

        final String assertionName = nodeNameFactory.getAssertionName(assertion, false);

        assertEquals("Generate OAuth Signature Base String", assertionName);
    }

    @Test
    public void getAssertionNameServer(){
        assertion.setUsageMode(GenerateOAuthSignatureBaseStringAssertion.UsageMode.SERVER);

        final String assertionName = nodeNameFactory.getAssertionName(assertion, true);

        assertEquals("Server Generate OAuth Signature Base String", assertionName);
    }

    @Test
    public void getAssertionNameClient(){
        assertion.setUsageMode(GenerateOAuthSignatureBaseStringAssertion.UsageMode.CLIENT);

        final String assertionName = nodeNameFactory.getAssertionName(assertion, true);

        assertEquals("Client Generate OAuth Signature Base String", assertionName);
    }
}
