package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 *
 */
public class ServerXpathCredentialSourceTest {

    private static final Document DOC = XmlUtil.stringAsDocument("<foo><bar>blah</bar><blat>bloo</blat></foo>");
    private static final String JSON = "{ \"firstName\": \"John\" }";

    @Test
    public void testSimpleCredsFromXml() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("/foo/bar"));
        ass.setPasswordExpression(new XpathExpression("/foo/blat"));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(DOC), new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);
        assertFalse("No audits are expected.", testAudit.iterator().hasNext());

        LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
        assertNotNull("credentials shall be gathered", creds);

        assertEquals("login shall be from xml", "blah", creds.getLogin());
        final char[] pass = creds.getCredentials();
        assertNotNull("non-null password shall be gathered", pass);
        String passStr = new String(pass);
        assertEquals("password shall be from xml", "bloo", passStr);
    }

    @Test
    public void testSimpleCredsFromVariablesFromQueryString() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("$request.http.parameter.username"));
        ass.setPasswordExpression(new XpathExpression("$request.http.parameter.password"));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);

        final Message request = new Message(DOC);
        request.attachHttpRequestKnob(new HttpRequestKnobStub(Collections.<HttpHeader>emptyList(), "http://127.0.0.1/test?username=blah&password=bloo"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);
        assertFalse("No audits are expected.", testAudit.iterator().hasNext());

        LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
        assertNotNull("credentials shall be gathered", creds);

        assertEquals("login shall be from xml", "blah", creds.getLogin());
        final char[] pass = creds.getCredentials();
        assertNotNull("non-null password shall be gathered", pass);
        String passStr = new String(pass);
        assertEquals("password shall be from xml", "bloo", passStr);
    }

    @Test
    @BugNumber(7224)
    public void testEmptyXmlRequestFailure() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("\"blah\""));
        ass.setPasswordExpression(new XpathExpression("/foo/blat"));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);

        Message req = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue("assertion shall have failed", !AssertionStatus.NONE.equals(result));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.XPATHCREDENTIAL_REQUEST_NOT_XML));
    }

    @Test
    @BugNumber(7224)
    public void testEmptyXmlRequestWithExpressionLiterals() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("\"blah\""));
        ass.setPasswordExpression(new XpathExpression("\"bloo\""));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);

        Message req = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);
        assertFalse("No audits are expected.", testAudit.iterator().hasNext());

        LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
        assertNotNull("credentials shall be gathered", creds);

        assertEquals("login shall be from expression literal", "blah", creds.getLogin());
        final char[] pass = creds.getCredentials();
        assertNotNull("non-null password shall be gathered", pass);
        String passStr = new String(pass);
        assertEquals("password shall be from expression literal", "bloo", passStr);
    }

    @Test
    @BugNumber(9883)
    public void testNonXmlRequestWithExpressionLiterals() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("\"blah\""));
        ass.setPasswordExpression(new XpathExpression("\"bloo\""));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);

        Message req = new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSON.getBytes(Charsets.UTF8)));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);
        assertFalse("No audits are expected.", testAudit.iterator().hasNext());

        LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
        assertNotNull("credentials shall be gathered", creds);

        assertEquals("login shall be from expression literal", "blah", creds.getLogin());
        final char[] pass = creds.getCredentials();
        assertNotNull("non-null password shall be gathered", pass);
        String passStr = new String(pass);
        assertEquals("password shall be from expression literal", "bloo", passStr);
    }


    @Test
    @BugId("FR-297")
    public void testSimpleCredsFromXmlTargetMessage() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("/foo/bar"));
        ass.setPasswordExpression(new XpathExpression("/foo/blat"));
        ass.setOtherTargetMessageVariable("in");
        ass.setTarget(TargetMessageType.OTHER);
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        Message doc = new Message(DOC);
        context.setVariable("in",doc);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);
        assertFalse("No audits are expected.", testAudit.iterator().hasNext());

        LoginCredentials creds = context.getAuthenticationContext(doc).getLastCredentials();
        assertNotNull("credentials shall be gathered", creds);

        assertEquals("login shall be from xml", "blah", creds.getLogin());
        final char[] pass = creds.getCredentials();
        assertNotNull("non-null password shall be gathered", pass);
        String passStr = new String(pass);
        assertEquals("password shall be from xml", "bloo", passStr);
    }

    @Test
    @BugId("SSG-7208")
    public void testSimpleCredsFromAttribute() throws Exception {
        final Document DOC_ATTR = XmlUtil.stringAsDocument("<foo><bar user=\"me\">blah</bar><blat password=\"boo\">bloo</blat></foo>");

        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("/foo/bar/@user"));
        ass.setPasswordExpression(new XpathExpression("/foo/blat/@password"));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(DOC_ATTR), new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);
        assertFalse("No audits are expected.", testAudit.iterator().hasNext());

        LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
        assertNotNull("credentials shall be gathered", creds);

        assertEquals("login shall be from xml", "me", creds.getLogin());
        final char[] pass = creds.getCredentials();
        assertNotNull("non-null password shall be gathered", pass);
        String passStr = new String(pass);
        assertEquals("password shall be from xml", "boo", passStr);
    }

    /**
     * If a variable does not exist then the assertion will fail.
     *
     * @throws Exception
     */
    @Test
    public void testAuditGeneratedWhenVariableDoesNotExist() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("$request.http.parameter.username"));
        ass.setPasswordExpression(new XpathExpression("$request.http.parameter.password"));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);

        Message req = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals("Assertion should fail as the referenced variables do not exist", AssertionStatus.FAILED, result);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.XPATHCREDENTIAL_LOGIN_XPATH_FAILED));
    }

    /**
     * If no results are found then the assertion should fail.
     *
     * @throws Exception
     */
    @Test
    public void testAuditGeneratedWhenNoResultsFound() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("/foo/noexist"));
        ass.setPasswordExpression(new XpathExpression("/foo/blat"));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);
        final TestAudit testAudit = configureInjects(sass);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(DOC), new Message());

        AssertionStatus result = sass.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals("Assertion should fail as the referenced variables do not exist", AssertionStatus.AUTH_REQUIRED, result);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.XPATHCREDENTIAL_LOGIN_XPATH_NOT_FOUND));
    }

    // - PRIVATE

    private TestAudit configureInjects(ServerXpathCredentialSource serverAssertion){
        TestAudit testAudit = new TestAudit();

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return testAudit;
    }
}
