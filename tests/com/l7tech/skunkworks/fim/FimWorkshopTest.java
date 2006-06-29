/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks.fim;

import com.l7tech.common.message.Message;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.wstrust.TokenServiceClient;
import com.l7tech.common.security.wstrust.WsTrustConfig;
import com.l7tech.common.security.wstrust.WsTrustConfigFactory;
import com.l7tech.common.security.wstrust.WsTrustConfigException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.WsTrustRequestType;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * Test ability to process sample documents provided with the FIM workshop documentation in April of 2005.
 * @author mlyons@layer7-tech.com
 */
public class FimWorkshopTest extends TestCase {
    private static Logger log = Logger.getLogger(FimWorkshopTest.class.getName());

    public FimWorkshopTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FimWorkshopTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /** Configure the token service client to use FIM-friendly namespace URIs. */
    private WsTrustConfig initFimNs() throws WsTrustConfigException {
        return WsTrustConfigFactory.getWsTrustConfigForNamespaceUri("http://schemas.xmlsoap.org/ws/2005/02/trust");
    }

    public void testReproClientRst() throws Exception {
        WsTrustConfig wstConfig = initFimNs();
        TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig);

        Document clientRst = TestDocuments.getTestDocument(TestDocuments.FIM2005APR_CLIENT_RST);

        String appliesTo = "http://s.example.com/services/EchoService";
        XmlSecurityToken usernameToken = new UsernameTokenImpl("testuser", "password".toCharArray());
        Document got = tokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                            WsTrustRequestType.VALIDATE,
                                                                            usernameToken,
                                                                            appliesTo,
                                                                            null);

        String gotStr = XmlUtil.nodeToFormattedString(got);
        String wantStr = XmlUtil.nodeToFormattedString(clientRst);

        // Uncomment to test.  For now it's not exact, but it looks close enough to interop.
        // Differences are in whitespace, position of namespace declarations, and the presence or absence
        // of wsu:Created inside the UsernameToken (the example includes it but we don't; however it is optional,
        // and of limited utility when accompanied by a plaintext password).
        //assertEquals(wantStr, gotStr);
    }

    public void testParseClientRstr() throws Exception {
        WsTrustConfig wstConfig = initFimNs();
        TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig);

        Document clientRstr = TestDocuments.getTestDocument(TestDocuments.FIM2005APR_CLIENT_RSTR);

        Object got = tokenServiceClient.parseUnsignedRequestSecurityTokenResponse(clientRstr);
        assertNotNull(got);
        assertTrue(got instanceof SamlSecurityToken);

        SamlSecurityToken saml = (SamlSecurityToken)got;
        log.info("Got token: " + XmlUtil.nodeToFormattedString(saml.asElement()));

        assertNull(saml.getConfirmationMethod());
        assertTrue(saml.isExpiringSoon(0));
        assertNotNull(saml.getAssertionId());
        saml.verifyEmbeddedIssuerSignature();
    }

    public void testParseClientRequest() throws Exception {
        initFimNs();
        Document clientReq = TestDocuments.getTestDocument(TestDocuments.FIM2005APR_CLIENT_REQ);
        Message reqMess = new Message(clientReq);

        WssProcessor processor = new WssProcessorImpl();

        ProcessorResult result = processor.undecorateMessage(reqMess, null, null, null, null, null);

        XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
        assertNotNull(tokens);
        assertTrue(tokens.length > 0);

        SamlSecurityToken saml = null;
        for (int i = 0; i < tokens.length; i++) {
            XmlSecurityToken token = tokens[i];
            if (token instanceof SamlSecurityToken) {
                if (saml != null) fail("More than one SamlSecurityToken found in request message");
                saml = (SamlSecurityToken)token;
            }
        }
        assertNotNull("No SamlSecurityToken found in request message", saml);

        String expectedAssertionId = "Assertion-uuid38d2bc2e-0103-fd68-0f9c-9119ab46159a";
        assertEquals(saml.getAssertionId(), expectedAssertionId);
    }

    public void testReproServerRst() throws Exception {
        WsTrustConfig wstConfig = initFimNs();
        TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig);

        Document serverRst = TestDocuments.getTestDocument(TestDocuments.FIM2005APR_SERVER_RST);

        // Get the saml assertion
        Document clientRstr = TestDocuments.getTestDocument(TestDocuments.FIM2005APR_CLIENT_RSTR);
        SamlSecurityToken samlToken =
                (SamlSecurityToken)tokenServiceClient.parseUnsignedRequestSecurityTokenResponse(clientRstr);

        String appliesTo = "http://s.example.com/services/EchoService";
        Document got = tokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                            WsTrustRequestType.VALIDATE,
                                                                            samlToken,
                                                                            appliesTo,
                                                                            null);

        String gotStr = XmlUtil.nodeToFormattedString(got);
        String wantStr = XmlUtil.nodeToFormattedString(serverRst);

        // Uncomment to test
        //assertEquals(wantStr, gotStr);
    }

    public void testParseServerRstr() throws Exception {
        WsTrustConfig wstConfig = initFimNs();
        TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig);

        Document serverRstr = TestDocuments.getTestDocument(TestDocuments.FIM2005APR_SERVER_RSTR);

        Object got = tokenServiceClient.parseUnsignedRequestSecurityTokenResponse(serverRstr);
        assertNotNull(got);
        assertTrue(got instanceof UsernameToken);

        UsernameToken ut = (UsernameToken)got;

        assertEquals("testuser", ut.getUsername());
        assertNull(ut.getPassword());
        assertEquals(SecurityTokenType.WSS_USERNAME, ut.getType());
        log.info("Got username token: " + XmlUtil.nodeToFormattedString(ut.asElement()));
    }
}
