package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 */
public class ServerNonSoapSignElementAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapSignElementAssertionTest.class.getName());

    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        JceProvider.init();
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", NonSoapXmlSecurityTestUtils.makeSecurityTokenResolver());
            put("ssgKeyStoreManager", NonSoapXmlSecurityTestUtils.makeSsgKeyStoreManager());
        }});
    }

    @Test
    public void testSimpleSignElement() throws Exception {
        assertTrue(true);

        NonSoapSignElementAssertion ass = new NonSoapSignElementAssertion();
        ass.setKeyAlias("data");
        ass.setUsesDefaultKeyStore(false);
        ass.setNonDefaultKeystoreId(-1);
        ass.setXpathExpression(new XpathExpression("/foo/bar"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapSignElementAssertion sass = new ServerNonSoapSignElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument("<foo><bar/></foo>"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Signed XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(1, doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature").getLength());
    }

    @Test
    @BugNumber(7871)
    public void testSimpleSignElementEcdsa() throws Exception {
        assertTrue(true);

        NonSoapSignElementAssertion ass = new NonSoapSignElementAssertion();
        ass.setKeyAlias(NonSoapXmlSecurityTestUtils.ECDSA_KEY_ALIAS);
        ass.setUsesDefaultKeyStore(false);
        ass.setNonDefaultKeystoreId(-1);
        ass.setXpathExpression(new XpathExpression("/foo/bar"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapSignElementAssertion sass = new ServerNonSoapSignElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument("<foo><bar/></foo>"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Signed XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(1, doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature").getLength());
    }
}
