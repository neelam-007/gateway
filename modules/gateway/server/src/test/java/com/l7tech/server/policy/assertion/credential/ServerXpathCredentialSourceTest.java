package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
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
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(DOC), new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);

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
        final Message request = new Message(DOC);
        request.attachHttpRequestKnob(new HttpRequestKnobStub(Collections.<HttpHeader>emptyList(), "http://127.0.0.1/test?username=blah&password=bloo"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);

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

        Message req = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertTrue("assertion shall have failed", !AssertionStatus.NONE.equals(result));
    }

    @Test
    @BugNumber(7224)
    public void testEmptyXmlRequestWithExpressionLiterals() throws Exception {
        XpathCredentialSource ass = new XpathCredentialSource();
        ass.setXpathExpression(new XpathExpression("\"blah\""));
        ass.setPasswordExpression(new XpathExpression("\"bloo\""));
        ServerXpathCredentialSource sass = new ServerXpathCredentialSource(ass);

        Message req = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);

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

        Message req = new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSON.getBytes(Charsets.UTF8)));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals("server assertion shall succeed", AssertionStatus.NONE, result);

        LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
        assertNotNull("credentials shall be gathered", creds);

        assertEquals("login shall be from expression literal", "blah", creds.getLogin());
        final char[] pass = creds.getCredentials();
        assertNotNull("non-null password shall be gathered", pass);
        String passStr = new String(pass);
        assertEquals("password shall be from expression literal", "bloo", passStr);
    }
}
