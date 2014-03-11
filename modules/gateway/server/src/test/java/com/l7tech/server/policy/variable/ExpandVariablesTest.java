package com.l7tech.server.policy.variable;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.*;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import com.l7tech.server.policy.assertion.ServerXpathAssertion;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.StoredSecureConversationSessionManagerStub;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Class ExpandVariablesTest.
 *
 * @author Emil Marceta
 */
public class ExpandVariablesTest {
    private static final Logger logger = Logger.getLogger(ExpandVariablesTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);
    private static final String TINY_BODY = "<blah/>";
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private static final MockConfig mockConfig = new MockConfig(new Properties());
    private static final OutboundSecureConversationContextManager outboundContextManager = new OutboundSecureConversationContextManager(mockConfig ,new StoredSecureConversationSessionManagerStub());

    static {
        beanFactory.addBean( "outboundSecureConversationContextManager", outboundContextManager );
    }

    private TestAudit testAudit;
    private TimeZone utcTimeZone;
    private TimeZone localTimeZone;
    /**
     * Wed, 27 Jun 2012 01:17:59 GMT
     */
    private final Date date = new Date(1340759879000L);

    @Before
    public void setUp() throws Exception {
        // put set up code here
        MessagesUtil.getAuditDetailMessageById(0); // This really shouldn't be necessary, but somebody's gotta do it

        testAudit = new TestAudit();
        utcTimeZone = TimeZone.getTimeZone("UTC");
        localTimeZone = TimeZone.getDefault();
    }

    @Test
    public void testSingleVariableExpand() throws Exception {
        Map<String, String> variables = new HashMap<String, String>();
        String value = "value_variable1";
        variables.put("var1", value);

        String inputMessage = "Blah message blah ${var1}";
        String expectedOutputMessage = "Blah message blah value_variable1";
        String processedMessage = ExpandVariables.process(inputMessage, variables, audit);
        assertTrue( processedMessage.contains( value ) );
        assertEquals(processedMessage, expectedOutputMessage);
    }

    @Test
    public void testSingleVariableExpandExtraWhitespace() throws Exception {
        Map<String, String> variables = new HashMap<String, String>();
        String value = "value_variable1";
        variables.put("var1", value);

        String inputMessage = "Blah message blah ${ var1 }";
        String expectedOutputMessage = "Blah message blah value_variable1";
        String processedMessage = ExpandVariables.process(inputMessage, variables, audit);
        assertTrue( processedMessage.contains( value ) );
        assertEquals(processedMessage, expectedOutputMessage);
    }

    @Test
    public void testMultipleVariableExpand() throws Exception {
        Map<String, String> variables = new HashMap<String, String>();
        String value1 = "value_variable1";
        String value2 = "value_variable2";
        variables.put("var1", value1);
        variables.put("var2", value2);

        String inputMessage = "Blah message blah ${var1} and more blah ${var2}";
        String expectedOutputMessage = "Blah message blah value_variable1 and more blah value_variable2";
        String processedMessage = ExpandVariables.process(inputMessage, variables, audit);
        assertTrue( processedMessage.contains( value1 ) );
        assertEquals(processedMessage, expectedOutputMessage);
    }

    @Test
    public void testSingleVariableNotFound() throws Exception {
        Map<String, Object> variables = new HashMap<String, Object>();
        String value = "value_variable1";
        variables.put("var1", value);

        final String prefix = "Blah message blah ";
        String inputMessage = prefix + "${var2}";
        String out = ExpandVariables.process(inputMessage, variables, audit);
        assertEquals(out, prefix);
    }

    @Test
    public void testUnterminatedRef() throws Exception {
        String[] vars = Syntax.getReferencedNames("${foo");
        assertEquals( (long) vars.length, 0L );
    }

    @Test
    public void testMultivalueDelimiter() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new String[] { "bar", "baz"});

        String out1 = ExpandVariables.process("${foo}", vars, audit);
        assertEquals(out1, "bar, baz"); // Default delimiter is ", "

        String out2 = ExpandVariables.process("<foo><val>" + "${foo|</val><val>}" + "</val></foo>", vars, audit);
        assertEquals(out2, "<foo><val>bar</val><val>baz</val></foo>");

        String out3 = ExpandVariables.process("${foo|}", vars, audit);
        assertEquals(out3, "barbaz");

        String out4 = ExpandVariables.process("${foo||}", vars, audit);
        assertEquals(out4, "bar|baz");
    }

    @Test
    public void testMultivalueSubscript() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("foo", new Object[] { "bar", "baz" });
        }};

        String out1 = ExpandVariables.process("${foo[0]}", vars, audit);
        assertEquals(out1, "bar");

        String out2 = ExpandVariables.process("${foo[1]}", vars, audit);
        assertEquals(out2, "baz");

        // Array index out of bounds -- log a warning, return empty string
        String out3 = ExpandVariables.process("${foo[2]}", vars, audit);
        assertEquals(out3, "");

        try {
            ExpandVariables.process("${foo[asdf]}", vars, audit);
            fail("Should have thrown--non-numeric subscript");
        } catch (IllegalArgumentException e) {
        }

        try {
            ExpandVariables.process("${foo[-1]}", vars, audit);
            fail("Should have thrown--negative subscript");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test @BugNumber(9611)
    public void testArrayLength() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("foo", new Object[] { "bar", "baz" });
        }};

        assertEquals("2", ExpandVariables.process("${foo.length}", vars, audit));
        assertEquals("2", ExpandVariables.process("${foo.leNGth}", vars, audit));
        assertEquals("", ExpandVariables.process("${foo.length.blah}", vars, audit));
    }

    @Test @BugNumber(9611)
    public void testListLength() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("foo", Arrays.asList("bar", "baz" ));
        }};

        assertEquals("2", ExpandVariables.process("${foo.length}", vars, audit));
        assertEquals("2", ExpandVariables.process("${foo.leNGth}", vars, audit));
        assertEquals("", ExpandVariables.process("${foo.length.blah}", vars, audit));
    }

    @Test @BugNumber(6455)
    public void testUnrecognizedSuffix() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("certificate.subject.cn", new Object[] { "blah" });
            put("certificate.subject.o", new Object[] { "blorf" });
            put("certificate.subject", new Object[] { "cn=blah, o=blorf" });
        }};

        assertEquals("cn=blah, o=blorf", ExpandVariables.process("${certificate.subject}", vars, audit).toLowerCase());
        assertEquals("blah", ExpandVariables.process("${certificate.subject.cn}", vars, audit));
        assertEquals("blorf", ExpandVariables.process("${certificate.subject.o}", vars, audit));
        assertEquals("", ExpandVariables.process("${certificate.subject.nonexistent}", vars, audit));
    }

    @Test @BugNumber(6813)
    public void testBackslashEscaping() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("dn", "cn=test\\+1");
            put("dns", new String[] { "cn=test\\+1", "cn=test2", "cn=test\\+3" });
        }};

        assertEquals("cn=test\\+1", ExpandVariables.process("${dn}", vars, audit));
        assertEquals("||cn=test\\+1||cn=test2||cn=test\\+3||", ExpandVariables.process("||${dns|||}||", vars, audit));
    }

    @Test
    public void testMessageVariableHeader() throws Exception {
        Message foo = makeTinyRequest();
        String ctype = ExpandVariables.process("${foo.http.header.content-type}", makeVars(foo), audit);
        assertEquals("Unexpected header value", "application/octet-stream", ctype);
    }

    @Test
    public void testMessageVariableMultivaluedHeaderTruncated() throws Exception {
        Message foo = makeTinyRequest();
        String magic = ExpandVariables.process("${foo.http.header.magic}", makeVars(foo), audit);
        assertEquals("Multivalued header truncated", "foo", magic);
    }

    @Test
    public void testMessageVariableMultiConcatHeaderValues() throws Exception {
        Message foo = makeTinyRequest();
        String magic = ExpandVariables.process("${foo.http.headerValues.magic||}", makeVars(foo), audit);
        assertEquals("Multivalued concatenated header values", "foo|bar", magic);
    }

    @Test
    public void testMessageVariableMultiLiteralHeaderValues() throws Exception {
        Message foo = makeTinyRequest();
        String[] magic = (String[]) ExpandVariables.processSingleVariableAsObject("${foo.http.headerValues.magic}", makeVars(foo), audit);
        assertTrue("Multivalued header values", Arrays.equals(new String[] {"foo", "bar"}, magic));
    }

    @Test
    public void testMessageVariableMultiLiteralHeaderValuesCaseInsensitive() throws Exception {
        Message foo = makeTinyRequest();
        String[] magic = (String[]) ExpandVariables.processSingleVariableAsObject("${foo.http.HeaderVaLUes.magic}", makeVars(foo), audit);
        assertTrue("Multivalued header values", Arrays.equals(new String[] {"foo", "bar"}, magic));
    }

    @BugNumber(8056)
    @Test
    public void testMessageVariableTcpKnobVariables() throws Exception {
        Message foo = new Message();
        foo.attachKnob(TcpKnob.class, new TcpKnob() {
            @Override
            public String getRemoteAddress() {
                return "1.1.1.1";
            }

            @Override
            public String getRemoteHost() {
                return "remotehost13.example.com";
            }

            @Override
            public int getRemotePort() {
                return 31337;
            }

            @Override
            public String getLocalAddress() {
                return "1.1.1.2";
            }

            @Override
            public String getLocalHost() {
                return "serverhost4242.example.com";
            }

            @Override
            public int getLocalPort() {
                return 33138;
            }

            @Override
            public int getLocalListenerPort() {
                return 35791;
            }
        });

        assertEquals("1.1.1.1", ExpandVariables.process("${foo.tcp.remoteAddress}", makeVars(foo), audit));
        assertEquals("1.1.1.1", ExpandVariables.process("${foo.tcp.remoteIP}", makeVars(foo), audit));
        assertEquals("remotehost13.example.com", ExpandVariables.process("${foo.tcp.remoteHost}", makeVars(foo), audit));
        assertEquals("31337", ExpandVariables.process("${foo.tcp.remotePort}", makeVars(foo), audit));
        assertEquals("1.1.1.2", ExpandVariables.process("${foo.tcp.localAddress}", makeVars(foo), audit));
        assertEquals("1.1.1.2", ExpandVariables.process("${foo.tcp.localIP}", makeVars(foo), audit));
        assertEquals("serverhost4242.example.com", ExpandVariables.process("${foo.tcp.localHost}", makeVars(foo), audit));
        assertEquals("33138", ExpandVariables.process("${foo.tcp.localPort}", makeVars(foo), audit));
        assertEquals("35791", ExpandVariables.process("${foo.tcp.listenPort}", makeVars(foo), audit));
    }

    @Test
    public void testEvaluateMultivaluedExpressionLookup() throws Exception {
        final String expr="${elements[1]}";
        final Object element0 = new Object() { @Override public String toString() { return "element1"; } };
        final Object element1 = new Object() { @Override public String toString() { return "element2"; } };
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("elements", new Object[] { element0, element1 });
        Object got = ExpandVariables.processSingleVariableAsObject(expr, vars, audit, true);
        assertTrue(got == element1);
    }

    @Test
    public void testRequestHttpParam() throws Exception {
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(makeTinyRequest(), new Message());

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"request.http.parameter.foo"}, audit);
        String paramValue = ExpandVariables.process("${request.http.parameter.foo}", vars, audit, true);
        assertEquals(paramValue, "bar");
    }

    @Test
    public void testRequestHttpParamCaseInsensitivity() throws Exception {
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(makeTinyRequest(), new Message());

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"request.http.parameter.Foo"}, audit);
        String paramValue = ExpandVariables.process("${request.http.parameter.fOO}", vars, audit, true);
        assertEquals(paramValue, "bar");
    }

    @Test
    public void testRequestHttpParamCaseInsensitivity2() throws Exception {
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(makeTinyRequest(), new Message());

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"request.http.parameter.Foo"}, audit);
        String paramValue = ExpandVariables.process("${request.HttP.paRAMeter.fOO}", vars, audit, true);
        assertEquals(paramValue, "bar");

        paramValue = ExpandVariables.process("${REQuest.httP.paRAMeter.fOO}", vars, audit, true);
        assertEquals(paramValue, "bar");
    }

    @Test
    public void testRequestHttpParamCasePreservation() throws Exception {
        @SuppressWarnings({"unchecked"}) final Enumeration<String> en = makeTinyRequest().getHttpRequestKnob().getParameterNames();
        String badname = null;
        while (en.hasMoreElements()) {
            String val = en.nextElement();
            if ("BaZ".equals(val)) return;
            if ("baz".equalsIgnoreCase(val)) badname = val;
        }
        fail("Expected to find original case, found " + badname);
    }

    @Test
    public void testMessageVariable() throws Exception {
        final Message foo = makeTinyRequest();
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        pec.setVariable("foo", foo);

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"foo.mainPart"}, audit);
        String bodytext = ExpandVariables.process("${foo.mainPart}", vars, audit, true);
        assertEquals(bodytext, TINY_BODY);
    }

    @Test
    public void testMessageVariableCaseInsensitive() throws Exception {
        final Message foo = makeTinyRequest();
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        pec.setVariable("fOo", foo);

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"fOO.mainPart"}, audit);
        String bodytext = ExpandVariables.process("${foO.mainPart}", vars, audit, true);
        assertEquals(bodytext, TINY_BODY);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testStrictNonexistentHeader() throws Exception {
        Message foo = makeTinyRequest();
        ExpandVariables.process("${foo.http.header.nonexistent}", makeVars(foo), audit, true);
        fail("Expected IAE for nonexistent header in strict mode");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testStrictSuspiciousToString() throws Exception {
        Message foo = makeTinyRequest();
        ExpandVariables.process("${foo}", makeVars(foo), audit, true);
        fail("Expected IAE for suspicious toString in strict mode");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testStrictStatusOnRequest() throws Exception {
        Message foo = makeTinyRequest();
        ExpandVariables.process("${foo.http.status}", makeVars(foo), audit, true);
        fail("Expected IAE for status on request");
    }

    @Test
    public void testRequestBody() throws Exception {
        Message foo = makeTinyRequest();
        String body = ExpandVariables.process("${foo.mainPart}", makeVars(foo), audit, true);
        assertEquals(body, TINY_BODY);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRequestBodyNotText() throws Exception {
        Message foo = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(TINY_BODY.getBytes("UTF-8")));
        ExpandVariables.process("${foo.mainPart}", makeVars(foo), audit, true);
        fail("Expected IAE for non-text mainPart");
    }

    public void testCertIssuerDnToString() throws Exception {
        LdapUser u = new LdapUser( new Goid(0,1234), "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        assertEquals("CN=OASIS Interop Test CA, O=OASIS", ExpandVariables.process("${authenticatedUser.cert.issuer}", vars, audit, true));
    }

    public void testCertIssuerDnName() throws Exception {
        LdapUser u = new LdapUser( new Goid(0,1234), "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        assertEquals("CN=OASIS Interop Test CA, O=OASIS", ExpandVariables.process("${authenticatedUser.cert.issuer}", vars, audit, true));
    }

    public void testCertIssuerAttribute() throws Exception {
        LdapUser u = new LdapUser( new Goid(0,1234), "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        assertEquals("OASIS", ExpandVariables.process("${authenticatedUser.cert.issuer.dn.o}", vars, audit, true));
    }

    public void testCertSubjectAttribute() throws Exception {
        LdapUser u = new LdapUser( new Goid(0,1234), "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        assertEquals("Alice", ExpandVariables.process("${authenticatedUser.cert.subject.dn.cn}", vars, audit, true));
    }

    public void testMixedCaseCertAttributeName() throws Exception {
        LdapUser u = new LdapUser( new Goid(0,1234), "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        assertEquals("Alice", ExpandVariables.process("${auTheNticAtedUser.cert.suBjeCt.dN.CN}", vars, audit, true));
    }

    public void testMultivaluedDcAttributes() throws Exception {
        LdapUser u = new LdapUser( new Goid(0,1234), "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(CertUtils.decodeFromPEM(A_CERT));
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        String got = ExpandVariables.process("${authenticatedUser.cert.subject.dn.dc|,}", vars, audit, true);
        assertEquals("com,l7tech,locutus", got);
    }

    public void testMultivaluedDcBttributes() throws Exception {
        LdapUser u = new LdapUser( new Goid(0,1234), "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(CertUtils.decodeFromPEM(B_CERT));
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        String got = ExpandVariables.process("${authenticatedUser.cert.subject.dn.dc|,}", vars, audit, true);
        assertEquals("com,l7tech,hello\\,cutus", got);
    }

    @Test
    public void testResponseStatus() throws Exception {
        Message resp = makeResponse();
        String status = ExpandVariables.process("${foo.http.status}", makeVars(resp), audit, true);
        assertEquals("123", status);
    }

    @Test
    public void testProcessSingleVariableAsDisplayableObject() throws Exception {
        final ApplicationContext spring = ApplicationContexts.getTestApplicationContext();
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(doc), new Message());

        final RequestXpathAssertion ass = new RequestXpathAssertion(new XpathExpression("/s:Envelope/s:Body/ns1:placeOrder/*", nsmap()));
        new AllAssertion(Arrays.asList(ass, new AuditDetailAssertion("${requestXpath.elements}")));
        final ServerXpathAssertion sxa = new ServerRequestXpathAssertion(ass);
        final AssertionStatus xpathStatus = sxa.checkRequest(context);
        assertEquals(AssertionStatus.NONE, xpathStatus);

        final Element[] els = (Element[]) context.getVariable("requestXpath.elements");
        assertEquals( 4L, (long) els.length );
        
        final Object what = ExpandVariables.processSingleVariableAsDisplayableObject("${requestXpath.elements[0]}", context.getVariableMap(new String[] { "requestXpath.elements" }, audit), audit, true);
        assertEquals("Formatted element output","<productid xsi:type=\"xsd:long\">-9206260647417300294</productid>",what);
    }

    @Test
    public void testProcessMultivaluedVariableAsDisplayableObject() throws Exception {
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(doc), new Message());

        context.setVariable( "requestXpath.elements",  getBodyFirstChildChildElements( doc ));
        final Element[] els = (Element[]) context.getVariable("requestXpath.elements");
        assertEquals( 4L, (long) els.length );

        final Object what = ExpandVariables.processSingleVariableAsDisplayableObject("${requestXpath.elements}", context.getVariableMap(new String[] { "requestXpath.elements" }, audit), audit, true);
        assertTrue( "Array result", what instanceof Object[] ); // doesn't seem very displayable ...
        assertEquals("Formatted element output [0]", "<productid xsi:type=\"xsd:long\">-9206260647417300294</productid>", ((Object[])what)[0]);
        assertEquals("Formatted element output [1]", "<amount xsi:type=\"xsd:long\">1</amount>", ((Object[])what)[1]);
        assertEquals("Formatted element output [2]", "<price xsi:type=\"xsd:float\">5.0</price>", ((Object[])what)[2]);
        assertEquals("Formatted element output [3]", "<accountid xsi:type=\"xsd:long\">228</accountid>", ((Object[])what)[3]);
    }

    private Element[] getBodyFirstChildChildElements( final Document doc ) throws InvalidDocumentFormatException {
        final List<Element> children = new ArrayList<Element>();
        final Element body = SoapUtil.getBodyElement(doc);

        Node node = body.getFirstChild();
        while ( node != null ) {
            if ( (int) node.getNodeType() == (int) Node.ELEMENT_NODE ) {
                node = node.getFirstChild();
                break;
            }
            node = node.getNextSibling();
        }
        while ( node != null ) {
            if ( (int) node.getNodeType() == (int) Node.ELEMENT_NODE ) {
                children.add( (Element)node );
            }
            node = node.getNextSibling();
        }
        return children.toArray( new Element[children.size()] );
    }

    @Test
    public void testMultivaluedElementVariableConcatenation() throws Exception {
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(doc), new Message());

        context.setVariable( "elements",  getBodyFirstChildChildElements( doc ));
        final Element[] els = (Element[]) context.getVariable("elements");
        assertEquals( 4L, (long) els.length );

        final String what = ExpandVariables.process("${elements}", context.getVariableMap(new String[] { "elements" }, audit), audit, true);
        assertEquals("Variable output", "<productid xsi:type=\"xsd:long\">-9206260647417300294</productid><amount xsi:type=\"xsd:long\">1</amount><price xsi:type=\"xsd:float\">5.0</price><accountid xsi:type=\"xsd:long\">228</accountid>", what);
    }

    @Test
    public void testMultivaluedElementVariableConcatenationWithDelimiter() throws Exception {
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(doc), new Message());

        context.setVariable( "elements",  getBodyFirstChildChildElements( doc ));
        final Element[] els = (Element[]) context.getVariable("elements");
        assertEquals( 4L, (long) els.length );

        final String what = ExpandVariables.process("${elements|,}", context.getVariableMap(new String[] { "elements" }, audit), audit, true);
        assertEquals("Variable output", "<productid xsi:type=\"xsd:long\">-9206260647417300294</productid>,<amount xsi:type=\"xsd:long\">1</amount>,<price xsi:type=\"xsd:float\">5.0</price>,<accountid xsi:type=\"xsd:long\">228</accountid>", what);
    }

    @Test
    public void testArrayElementVariableFormatting() throws Exception {
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(doc), new Message());

        context.setVariable( "elements",  getBodyFirstChildChildElements( doc ));
        final Element[] els = (Element[]) context.getVariable("elements");
        assertEquals( 4L, (long) els.length );

        final String what = ExpandVariables.process("${elements[0]}", context.getVariableMap(new String[] { "elements" }, audit), audit, true);
        assertEquals("Variable output", "<productid xsi:type=\"xsd:long\">-9206260647417300294</productid>", what);
    }

    @Test
    public void testElementVariableFormatting() throws Exception {
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(doc), new Message());

        context.setVariable( "element", getBodyFirstChildChildElements( doc )[0]);

        final String what = ExpandVariables.process("${element}", context.getVariableMap(new String[] { "element" }, audit), audit, true);
        assertEquals("Variable output", "<productid xsi:type=\"xsd:long\">-9206260647417300294</productid>", what);
    }

    @BugNumber(7664)
    @Test(expected = VariableNameSyntaxException.class)
    public void testBadVariableThrows(){
        String varName = "doesnotexist";
        ExpandVariables.badVariable(varName, true, audit);
    }

    @BugNumber(7664)
    @Test
    public void testBadVariableNoThrows(){
        String varName = "doesnotexist";
        ExpandVariables.badVariable(varName, false, audit);
    }

    @BugNumber(7664)
    @Test(expected = VariableNameSyntaxException.class)
    public void testProcessNonExistentVariable() throws Exception {
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(makeTinyRequest(), new Message());
        final Map<String, Object> vars = pec.getVariableMap(new String[]{}, audit);
        ExpandVariables.process("${dontexist}", vars, audit, true);
    }

    /*
     * Comprehensive test coverage for ExpandVariables.processNoFormat()
     * Ensures that no empty strings are returned as index 0
     * Tests that any preceeding text, even if a single space, is preserved
     * Tests the correct number of elements are returned when:-
     * only text is used
     * when text and single valued vars are used
     * when variables of type Message used
     * when multi valued variables backed by an Object [] and List are used
     * when a mix of text, single valued variables, variables of type Message and mulit valued variables are used
     */
    @BugNumber(7688)
    @Test
    public void testProcessNoFormat() throws Exception{
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(makeTinyRequest(), new Message());
        final Map<String, Object> vars = pec.getVariableMap(new String[]{}, audit);
        vars.put("varname", "value");
        List<Object> paramValue = ExpandVariables.processNoFormat("${varname}", vars, audit, true);
        assertEquals("Incorrect number of values found", 1L, (long) paramValue.size() );
        assertEquals("varname's value not found", "value", paramValue.get(0));

        paramValue = ExpandVariables.processNoFormat("preceeding text ${varname}", vars, audit, true);
        assertEquals("Incorrect number of values found", 2L, (long) paramValue.size() );
        assertEquals("Preceeding text should have been found", "preceeding text ", paramValue.get(0));
        assertEquals("varname's value not found", "value", paramValue.get(1));

        //preserve any empty formatting for what ever reason
        paramValue = ExpandVariables.processNoFormat(" ${varname}", vars, audit, true);
        assertEquals("Incorrect number of values found", 2L, (long) paramValue.size() );
        assertEquals("Preceeding empty string should have been preserved", " ", paramValue.get(0));
        assertEquals("varname's value not found", "value", paramValue.get(1));

        //preserve any post variable formatting
        paramValue = ExpandVariables.processNoFormat(" ${varname} ", vars, audit, true);
        assertEquals("Incorrect number of values found", 3L, (long) paramValue.size() );
        assertEquals("Preceeding empty string should have been preserved", " ", paramValue.get(0));
        assertEquals("Trailing empty string should have been preserved", " ", paramValue.get(2));
        assertEquals("varname's value not found", "value", paramValue.get(1));

        paramValue = ExpandVariables.processNoFormat(" ${varname} trailing text", vars, audit, true);
        assertEquals("Incorrect number of values found", 3L, (long) paramValue.size() );
        assertEquals("Preceeding empty string should have been preserved", " ", paramValue.get(0));
        assertEquals("Trailing empty string should have been preserved", " trailing text", paramValue.get(2));
        assertEquals("varname's value not found", "value", paramValue.get(1));

        //Test variable of type Message support
        String xml = "<donal>value</donal>";
        Document doc = XmlUtil.parse(xml);
        Message m = new Message(doc);
        vars.put("MESSAGE_VAR", m);

        paramValue = ExpandVariables.processNoFormat("${MESSAGE_VAR}", vars, audit, true);
        assertEquals("Incorrect number of values found", 1L, (long) paramValue.size() );
        assertTrue("varname's value not found", paramValue.get(0) instanceof Message);

        //test mix of single value variable, message variable and text
        paramValue = ExpandVariables.processNoFormat("The single valued var ${varname} and the Message var ${MESSAGE_VAR} test", vars, audit, true);
        assertEquals("Incorrect number of values found", 5L, (long) paramValue.size() );
        assertEquals("Incorrect text value extracted", "The single valued var ", paramValue.get(0));
        assertEquals("varname's value not found", "value", paramValue.get(1));
        assertEquals("Incorrect text value extracted", " and the Message var ", paramValue.get(2));
        assertTrue("MESSAGE_VAR's value not found", paramValue.get(3) instanceof Message);
        assertEquals("Incorrect text value extracted", " test", paramValue.get(4));

        //test coverage for multi valued variables
        vars.put("MULTI_VALUED_VAR", new Object[]{"one", m, "three"});
        paramValue = ExpandVariables.processNoFormat("${MULTI_VALUED_VAR}", vars, audit, true);
        assertEquals("Incorrect number of values found", 3L, (long) paramValue.size() );
        assertEquals("Incorrect value found from multi valued variable", "one", paramValue.get(0));
        assertTrue("Message not found from multi valued variable", paramValue.get(1) instanceof Message);
        assertEquals("Incorrect value found from multi valued variable", "three", paramValue.get(2));

        //test coverage for multi valued variables surrounded by other text and variables
        paramValue = ExpandVariables.processNoFormat("The single valued var ${varname} and multi valued ${MULTI_VALUED_VAR} the Message var ${MESSAGE_VAR} test", vars, audit, true);
        assertEquals("Incorrect number of values found", 9L, (long) paramValue.size() );
        assertEquals("Incorrect text value extracted", "The single valued var ", paramValue.get(0));
        assertEquals("varname's value not found", "value", paramValue.get(1));
        assertEquals("Incorrect text value extracted", " and multi valued ", paramValue.get(2));
        assertEquals("Incorret value found from multi valued variable", "one", paramValue.get(3));
        assertTrue("Message not found from multi valued variable", paramValue.get(4) instanceof Message);
        assertEquals("Incorret value found from multi valued variable", "three", paramValue.get(5));
        assertEquals("Incorrect text value extracted", " the Message var ", paramValue.get(6));
        assertTrue("MESSAGE_VAR's value not found", paramValue.get(7) instanceof Message);
        assertEquals("Incorrect text value extracted", " test", paramValue.get(8));

        //Test list backed multi valued variable
        List<Object> testList = new ArrayList<Object>();
        testList.add("one");
        testList.add(m);
        testList.add("three");
        
        vars.put("LIST_VAR", testList);
        paramValue = ExpandVariables.processNoFormat("${LIST_VAR}", vars, audit, true);
        assertEquals("Incorrect number of values found", 3L, (long) paramValue.size() );
        assertEquals("Incorrect value found from multi valued variable", "one", paramValue.get(0));
        assertTrue("Message not found from multi valued variable", paramValue.get(1) instanceof Message);
        assertEquals("Incorrect value found from multi valued variable", "three", paramValue.get(2));

    }

    /**
     * Validate no null pointer when strict is false and variable does not exist.
     */
    @Test
    public void testProcessNoFormat_Lax() throws Exception{
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(makeTinyRequest(), new Message());
        final Map<String, Object> vars = pec.getVariableMap(new String[]{}, audit);
        List<Object> paramValue = ExpandVariables.processNoFormat("${does_not_exist}", vars, audit, false);
        Assert.assertTrue("List should be empty", paramValue.isEmpty());
    }

    @Test
    public void testProcessNoFormat_EmptyString() throws Exception {
        final List<Object> objects = ExpandVariables.processNoFormat("", new HashMap<String, Object>(), audit);
        Assert.assertNotNull(objects);
        Assert.assertTrue(objects.isEmpty());
    }

    @Test
    public void testPartInfoSelector() throws Exception {
        final byte[] partBody = "test part content".getBytes();
        final Map<String, Object> vars = new HashMap<String,Object>();
        vars.put("part", new PartInfoMock(partBody) );

        assertEquals("part.body", "test part content", ExpandVariables.process( "${part.body}", vars, audit, true ));
        assertEquals("part.contentType", "text/plain", ExpandVariables.process( "${part.contentType}", vars, audit, true ));
        assertEquals("part.header.test", "value", ExpandVariables.process( "${part.header.test}", vars, audit, true ));
        assertEquals("part.header.test2", "", ExpandVariables.process( "${part.header.test2}", vars, audit, false ));
        assertEquals("part.size", "17", ExpandVariables.process( "${part.size}", vars, audit, true ));
    }

    @Test
    public void testPartInfoArraySelector() throws Exception {
        final byte[] partBody = "test part content".getBytes();
        final Map<String, Object> vars = new HashMap<String,Object>();
        vars.put("parts", new PartInfo[]{ new PartInfoMock(partBody) } );

        assertEquals("parts.1.body", "test part content", ExpandVariables.process( "${parts.1.body}", vars, audit, true ));
        assertEquals("parts.1.contentType", "text/plain", ExpandVariables.process( "${parts.1.contentType}", vars, audit, true ));
        assertEquals("parts.1.header.test", "value", ExpandVariables.process( "${parts.1.header.test}", vars, audit, true ));
        assertEquals("parts.1.header.test2", "", ExpandVariables.process( "${parts.1.header.test2}", vars, audit, false ));
        assertEquals("parts.1.size", "17", ExpandVariables.process( "${parts.1.size}", vars, audit, true ));
    }

    @Test
    public void testMessagePartInfoArraySelector() throws Exception {
        final Map<String, Object> vars = new HashMap<String,Object>();
        vars.put("message", new Message(XmlUtil.parse( "<content/>" )) );

        assertEquals("message.parts.1.body", "<content/>", ExpandVariables.process( "${message.parts.1.body}", vars, audit, true ));
        assertEquals("message.parts.1.contentType", "text/xml; charset=utf-8", ExpandVariables.process( "${message.parts.1.contentType}", vars, audit, true ));
        assertEquals("message.parts.1.size", "10", ExpandVariables.process( "${message.parts.1.size}", vars, audit, true ));
    }

    @Test
    public void testSecureConversationSession() throws Exception {
        //  Create a user
        final UserBean user = new UserBean("Alice");
        user.setUniqueIdentifier("1");

        long creationTime = System.currentTimeMillis();

        // Create a secure conversation session
        SecureConversationSession session = outboundContextManager.createContextForUser(
            user,
            OutboundSecureConversationContextManager.newSessionKey( user, "fake_service_url" ),
            "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512",
            "fake_session_identifier",
            creationTime,
            creationTime + (long) (2 * 60 * 1000),
            generateNewSecret(64),
            null,
            null,
            0
        );

        // Set the variable, scLookup.session
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        pec.setVariable("scLookup.session", session);

        // Check the attribute, id
        String id = getSessionAttributeVariableValue(pec, "scLookup.session.id");
        assertEquals("scLookup.session.id", "fake_session_identifier", id);

        // Check the attribute, user
        pec.setVariable("scLookup.session.user", session.getUsedBy());
        String userId = getSessionAttributeVariableValue(pec, "scLookup.session.user.id");
        assertEquals("scLookup.session.user.id", "1", userId);

        // Check the attribute, creation
        long creationInMills = session.getCreation();
        String creation = getSessionAttributeVariableValue(pec, "scLookup.session.creation");
        assertEquals("scLookup.session.creation", ISO8601Date.format(new Date(creationInMills)), creation);

        // Check the attribute, expiration
        long expirationInMills = session.getExpiration();
        String expiration = getSessionAttributeVariableValue(pec, "scLookup.session.expiration");
        assertEquals("scLookup.session.expiration", ISO8601Date.format(new Date(expirationInMills)), expiration);

        // Check the attribute, scNamespace
        String scNamespace = getSessionAttributeVariableValue(pec, "scLookup.session.scNamespace");
        assertEquals("scLookup.session.scNamespace", "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512", scNamespace);
    }

    @Test
    public void testDefaultFormatting() throws Exception {
        testFormattedDate("mydate", "${mydate}", DateUtils.ISO8601_PATTERN, utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc() throws Exception {
        testFormattedDate("mydate", "${mydate.utc}", DateUtils.ISO8601_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.uTC}", DateUtils.ISO8601_PATTERN, utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Local() throws Exception {
        testFormattedDate("mydate", "${mydate.local}", DateUtils.ISO8601_PATTERN, localTimeZone);
        testFormattedDate("mydate", "${mydate.LocaL}", DateUtils.ISO8601_PATTERN, localTimeZone);
    }

    @Test
    public void testDefaultFormatting_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_CustomFormatSuffixWithPeriod() throws Exception {
        testFormattedDate("mydate", "${mydate.yyyyMMdd'T'HH:mm:ss.SSS}", "yyyyMMdd'T'HH:mm:ss.SSS", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.utc.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc_CustomFormatSuffixWithPeriod() throws Exception {
        testFormattedDate("mydate", "${mydate.utc.yyyyMMdd'T'HH:mm:ss.SSS}", "yyyyMMdd'T'HH:mm:ss.SSS", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Local_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.local.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", localTimeZone);
    }

    @Test
    public void testMillis() throws Exception {
        testTimestampDate("mydate", "${mydate.millis}", false);
    }

    @Test
    public void testSeconds() throws Exception {
        testTimestampDate("mydate", "${mydate.seconds}", true);
    }

    @Test
    public void testBuiltInSuffixes_ISO8601() throws Exception {
        testFormattedDate("mydate", "${mydate.iSo8601}", DateUtils.ISO8601_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.local.iso8601}", DateUtils.ISO8601_PATTERN, localTimeZone);
    }

    @Test
    public void testBuiltInSuffixes_RFC1123() throws Exception {
        testFormattedDate("mydate", "${mydate.RFc1123}", DateUtils.RFC1123_DEFAULT_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.utc.rfc1123}", DateUtils.RFC1123_DEFAULT_PATTERN, utcTimeZone);
    }

    @Test
    public void testBuiltInSuffixes_RFC850() throws Exception {
        testFormattedDate("mydate", "${mydate.RFc850}", DateUtils.RFC850_DEFAULT_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.lOCAL.rfc850}", DateUtils.RFC850_DEFAULT_PATTERN, localTimeZone);
    }

    @Test
    public void testBuiltInSuffixes_asctime() throws Exception {
        testFormattedDate("mydate", "${mydate.asctime}", DateUtils.ASCTIME_DEFAULT_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.lOCAL.asctime}", DateUtils.ASCTIME_DEFAULT_PATTERN, localTimeZone);
    }

    @Test
    public void testDateSelector_local1() throws Exception {
        //doesn't matter what the variable is called - added in .test to illustrate that all that matters is the
        // longest part of the variable reference which matches a defined variable.
        testDateSelector_local("mydate.test", new Date(),"${mydate.test.local}",null);
    }

    @Test
    @BugNumber(12158)
    public void testDateSelector_local2() throws Exception {
        //doesn't matter what the variable is called - added in .test to illustrate that all that matters is the
        // longest part of the variable reference which matches a defined variable even though there are spaces in between them.
        // TODO: uncomment the following if we change the behavior as part of Bug #12158 fix (DateTimeSelector is unable to parse vaiables with spaces)
        // testDateSelector_local("mydate.test", new Date(),"${ mydate.test . local }", null);
    }

    @Test
    @BugNumber(12158)
    public void testDateSelector_local3() throws Exception {
        //doesn't matter what the variable is called - added in . test , .local and .asctime to illustrate that all that matters is the
        // longest part of the variable reference which matches a defined variable even though there are spaces in between them.
        // TODO: uncomment the following if we change the behavior as part of Bug #12158 fix (DateTimeSelector is unable to parse vaiables with spaces)
        // testDateSelector_local("mydate.test", new Date(),"${ mydate.test . local. asctime  }",DateUtils.ASCTIME_DEFAULT_PATTERN);
    }
    private void testDateSelector_local(String value, Date date, String inputValue, String format) throws Exception {
        final HashMap<String, Object> vars = new HashMap<String, Object>();

        vars.put(value, date);
        final String actual = ExpandVariables.process(inputValue, vars, testAudit);

        //format according to local
        final String expected = DateUtils.getFormattedString(date, DateUtils.getTimeZone("local"), format);
        assertEquals(expected, actual);

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testNamedTimeZoneSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.Australia/Victoria}", DateUtils.ISO8601_PATTERN, TimeZone.getTimeZone("Australia/Victoria"));
        testFormattedDate("mydate", "${mydate.Australia/Victoria.rfc1123}", DateUtils.RFC1123_DEFAULT_PATTERN, TimeZone.getTimeZone("Australia/Victoria"));
    }

    @Test
    public void testOffsetTimeZoneNegative() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-07:00}", "2012-06-26T18:17:59.000-07:00");
    }

    @Test
    public void testOffsetTimeZoneNegative_Short() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-07}", "2012-06-26T18:17:59.000-07:00");
    }

    @Test
    public void testOffsetTimeZonePositive() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+07:00}", "2012-06-27T08:17:59.000+07:00");
    }

    @Test
    public void testOffsetTimeZonePositive_Short() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+07:00}", "2012-06-27T08:17:59.000+07:00");
    }

    @Test
    public void testOffsetTimeZoneNegative_NoColon() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-0700}", "2012-06-26T18:17:59.000-07:00");
    }

    @Test
    public void testOffsetTimeZonePositive_NoColon() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+0700}", "2012-06-27T08:17:59.000+07:00");
    }

    @Test
    public void testOffsetTimeZoneNegativeWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-04:30}", "2012-06-26T20:47:59.000-04:30");
    }

    @Test
    public void testOffsetTimeZonePositiveWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+05:30}", "2012-06-27T06:47:59.000+05:30");
    }

    @Test
    public void testOffsetTimeZoneNegative_NoColonWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-0230}", "2012-06-26T22:47:59.000-02:30");
    }

    @Test
    public void testOffsetTimeZonePositive_NoColonWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+0530}", "2012-06-27T06:47:59.000+05:30");
    }

    @BugNumber(13278)
    @Test
    public void testLengthOnArray() {
        final String[] s = new String[] {"a", "b", "c"};
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("test", s);
        }};
        assertEquals(Integer.toString(s.length), ExpandVariables.process("${test.length}", vars, audit));
        assertEquals(Integer.toString(s.length), ExpandVariables.process("${test.LengTH}", vars, audit));
        assertEquals(Integer.toString(s.length), ExpandVariables.process("${test.LENGTH}", vars, audit));
    }

    @BugNumber(13278)
    @Test
    public void testLengthOnCollection() {
        final List<String> s = new ArrayList<String>();
        s.add("a");
        s.add("b");
        s.add("c");
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("test", s);
        }};
        assertEquals(Integer.toString(s.size()), ExpandVariables.process("${test.length}", vars, audit));
        assertEquals(Integer.toString(s.size()), ExpandVariables.process("${test.LengTH}", vars, audit));
        assertEquals(Integer.toString(s.size()), ExpandVariables.process("${test.LENGTH}", vars, audit));
        assertEquals(Integer.toString(s.size()), ExpandVariables.process("${test.LENGTH}", vars, audit, true));
        assertEquals("", ExpandVariables.process("${test.length.length}", vars, audit));
    }

    @BugNumber(13278)
    @Test
    public void testLengthOnInvalidVariable() {
        final List<String> s = new ArrayList<String>();
        s.add("a");
        s.add("b");
        s.add("c");
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("test", s);
        }};
        assertEquals("", ExpandVariables.process("${test.a.length}", vars, audit ));
    }


    @BugNumber(13278)
    @Test
    public void testNoSelectorWithLength() {
        final String s = "a";
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("test", s);
        }};
        try {
            ExpandVariables.process("${test.a.lengTH}", vars, audit, true);
            fail();
        } catch (IllegalArgumentException e) {
            String expect = MessageFormat.format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), "a.lengTH");
            assertEquals(expect, e.getMessage());
        }
    }

    @BugNumber(13278)
    @Test
    public void testLengthOnSingleValueVariable() {
        final String s = "a";
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("test.length", s);
        }};
        assertEquals(s, ExpandVariables.process("${test.length}", vars, audit));
    }

    @BugNumber(13278)
    @Test
    public void testLengthOnNonCollectionObject() {
        final String s = "a";
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("test", s);
        }};
        try {
            ExpandVariables.process("${test.length}", vars, audit, true);
            fail();
        } catch (IllegalArgumentException e) {
            String expect = MessageFormat.format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), "length");
            assertEquals(expect, e.getMessage());
        }
    }



    private byte[] generateNewSecret(int length) {
        final byte[] output = new byte[length];
        Random random = new SecureRandom();
        random.nextBytes(output);
        return output;
    }

    private String getSessionAttributeVariableValue(PolicyEnforcementContext pec, String variableName) {
        final Map<String, Object> vars = pec.getVariableMap(new String[]{variableName}, audit);
        return ExpandVariables.process("${" + variableName + "}", vars, audit, true);
    }

    private Map<String, String> nsmap() {
        final Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("ns1", "http://warehouse.acme.com/ws");
        nsmap.put("s", "http://schemas.xmlsoap.org/soap/envelope/");
        return nsmap;
    }

    private Map<String, Object> makeVars(Message foo) {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", foo);
        return vars;
    }

    private Message makeTinyRequest() throws NoSuchPartException, IOException {
        Message foo = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(TINY_BODY.getBytes("UTF-8")));
        Map<String, MimeHeader> headers = new HashMap<String, MimeHeader>();
        headers.put("Content-Type", ContentTypeHeader.OCTET_STREAM_DEFAULT);

        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("foo", new String[] { "bar" });
        params.put("BaZ", new String[] { "quux", "xyzzy"});
        foo.attachHttpRequestKnob(new HttpRequestKnobAdapter("/foo", params, new MimeHeaders(headers)));
        foo.getHeadersKnob().addHeader("Content-Type", ContentTypeHeader.OCTET_STREAM_DEFAULT.getFullValue());
        foo.getHeadersKnob().addHeader("magic", "foo");
        foo.getHeadersKnob().addHeader("magic", "bar");
        return foo;
    }

    private Message makeResponse() throws Exception {
        Message foo = new Message();
        foo.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
            {
                setStatus(123);
            }
        });
        return foo;
    }

    private void testFormattedDate(final Date date, final String dateVarNameNoSuffixes, final String dateExpression, final String expectedOutput) {
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put(dateVarNameNoSuffixes, date);
        final String actual = ExpandVariables.process(dateExpression, vars, testAudit);
        System.out.println(actual);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(expectedOutput, actual);
        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    private void testFormattedDate(final String dateVarNameNoSuffixes, final String dateExpression, final String expectedFormat, final TimeZone expectedTimeZone) {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put(dateVarNameNoSuffixes, date);
        final String actual = ExpandVariables.process(dateExpression, vars, testAudit);
        System.out.println(actual);

        final SimpleDateFormat format = new SimpleDateFormat(expectedFormat);
        format.setTimeZone(expectedTimeZone);
        format.setLenient(false);
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        assertEquals(format.format(cal.getTime()), actual);
        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    private void testTimestampDate(final String dateVarNameNoSuffixes, final String dateExpression, final boolean seconds) {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put(dateVarNameNoSuffixes, date);
        final String process = ExpandVariables.process(dateExpression, vars, testAudit);
        if (seconds) {
            assertEquals(Long.valueOf(date.getTime() / 1000L), Long.valueOf(process));
        } else {
            assertEquals(Long.valueOf(date.getTime()), Long.valueOf(process));
        }

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    private static class PartInfoMock implements PartInfo {
        private final byte[] partBody;

        private PartInfoMock( final byte[] partBody ) {
            this.partBody = partBody;

        }

        @Override
        public MimeHeader getHeader( final String name ) {
            try {
                return "test".equals( name ) ? MimeHeader.parseValue("test","value") : null;
            } catch ( IOException e ) {
                throw ExceptionUtils.wrap(e);
            }
        }

        @Override
        public InputStream getInputStream( final boolean destroyAsRead ) throws IOException, NoSuchPartException {
            return new ByteArrayInputStream(partBody);
        }

        @Override
        public byte[] getBytesIfAlreadyAvailable() {
            return partBody;
        }

        @Override
        public byte[] getBytesIfAvailableOrSmallerThan( final int maxSize ) throws IOException, NoSuchPartException {
            return partBody;
        }

        @Override
        public long getContentLength() {
            return (long) partBody.length;
        }

        @Override
        public long getActualContentLength() throws IOException, NoSuchPartException {
            return (long) partBody.length;
        }

        @Override
        public ContentTypeHeader getContentType() {
            try {
                return ContentTypeHeader.parseValue( "text/plain" );
            } catch ( IOException e ) {
                throw ExceptionUtils.wrap(e);
            }
        }

        @Override public int getPosition() { return 0; }
        @Override public void setBodyBytes( final byte[] newBody ) throws IOException { throw new IOException(); }
        @Override public void setBodyBytes( final byte[] newBody, int offset, int length ) throws IOException { throw new IOException(); }
        @Override public void setContentType( final ContentTypeHeader newContentType ) {}
        @Override public MimeHeaders getHeaders() { return null; }
        @Override public String getContentId( final boolean stripAngleBrackets ) { return null; }
        @Override public boolean isValidated() { return false; }
        @Override public void setValidated( final boolean validated ) { }
        @Override public boolean isBodyAvailable() { return true; }
        @Override public boolean isBodyStashed() { return true; }
        @Override public boolean isBodyRead() { return true; }
    }

    private static class HttpRequestKnobAdapter extends TcpKnobAdapter implements HttpRequestKnob {
        private final String uri;
        private final MimeHeaders headers;
        private final Map<String, String[]> params;

        private HttpRequestKnobAdapter(String uri, Map<String, String[]> params, MimeHeaders headers) {
            this.uri = uri;
            this.headers = headers;
            final TreeMap<String, String[]> newmap = new TreeMap<String, String[]>(String.CASE_INSENSITIVE_ORDER);
            newmap.putAll(params);
            this.params = Collections.unmodifiableMap(newmap);
        }

        @Override
        public HttpCookie[] getCookies() {
            return new HttpCookie[0];
        }

        @Override
        public HttpMethod getMethod() {
            return HttpMethod.POST;
        }

        @Override
        public String getMethodAsString() {
            return getMethod().name();
        }

        @Override
        public String getRequestUri() {
            return uri;
        }

        @Override
        public String getRequestUrl() {
            return "http://ssg" + uri;
        }

        @Override
        public URL getRequestURL() {
            try {
                return new URL(getRequestUrl());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }

        @Override
        public long getDateHeader(String name) throws ParseException {
            return Long.valueOf(headers.get(name).getMainValue());
        }

        @Override
        public int getIntHeader(String name) {
            return Integer.valueOf(headers.get(name).getMainValue());
        }

        @Override
        public String getHeaderSingleValue(String name) throws IOException {
            return getHeaderFirstValue(name);
        }

        @Override
        public String getHeaderFirstValue(String name) {
            final MimeHeader header = headers.get(name);
            if (header == null) return null;
            return header.getMainValue();
        }

        @Override
        public String[] getHeaderNames() {
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < headers.size(); i++) {
                MimeHeader header = headers.get(i);
                names.add(header.getName());
            }
            return names.toArray(new String[names.size()]);
        }

        @Override
        public String[] getHeaderValues(String name) {
            if ("magic".equalsIgnoreCase(name)) {
                return new String[] { "foo", "bar" };
            }
            MimeHeader mimeHeaders = headers.get(name);
            if ( mimeHeaders==null ) {
                return new String[0];
            } else {
                return new String[] { mimeHeaders.getMainValue() };
            }
        }

        @Override
        public X509Certificate[] getClientCertificate() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getParameter(String name) throws IOException {
            String[] ss = params.get(name);
            if (ss == null || ss.length == 0) return null;
            return ss[0];
        }

        @Override
        public Map getParameterMap() throws IOException {
            return params;
        }

        @Override
        public String[] getParameterValues(String s) throws IOException {
            return params.get(s);
        }

        @Override
        public Enumeration<String> getParameterNames() throws IOException {
            return new IteratorEnumeration<String>(params.keySet().iterator());
        }

        @Override
        public Object getConnectionIdentifier() {
            return null;
        }

        @Override
        public String getQueryString() {
            return getRequestURL().getQuery();
        }

        @Override
        public String getRemoteAddress() {
            return "127.0.0.1";
        }

        @Override
        public String getRemoteHost() {
            return "127.0.0.1";
        }

        @Override
        public int getLocalPort() {
            return 8080;
        }

        @Override
        public String getSoapAction() throws IOException {
            return null;
        }
    }

    public static final String A_CERT = "-----BEGIN CERTIFICATE-----\n" +
        "MIICJTCCAc+gAwIBAgIJAKiXZCjIMvOjMA0GCSqGSIb3DQEBBQUAMFwxFDASBgNVBAMMC0FsZXgg\n" +
        "Q3J1aXNlMRcwFQYKCZImiZPyLGQBGRYHbG9jdXR1czEWMBQGCgmSJomT8ixkARkWBmw3dGVjaDET\n" +
        "MBEGCgmSJomT8ixkARkWA2NvbTAeFw0wODExMjQyMjMzMzdaFw0yODExMTkyMjMzMzdaMFwxFDAS\n" +
        "BgNVBAMMC0FsZXggQ3J1aXNlMRcwFQYKCZImiZPyLGQBGRYHbG9jdXR1czEWMBQGCgmSJomT8ixk\n" +
        "ARkWBmw3dGVjaDETMBEGCgmSJomT8ixkARkWA2NvbTBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQCJ\n" +
        "P11ezerCfEBJKxXbQO1ZWPTVghwMNCxwEAi067MstcfPnuI9XBwFSVR4ke2ytsEQ7vUSSXyPjqvc\n" +
        "/TNchtdnAgMBAAGjdDByMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgXgMBIGA1UdJQEB/wQI\n" +
        "MAYGBFUdJQAwHQYDVR0OBBYEFKPI4R7IsV3ieg87PQGkatHDVIlcMB8GA1UdIwQYMBaAFKPI4R7I\n" +
        "sV3ieg87PQGkatHDVIlcMA0GCSqGSIb3DQEBBQUAA0EABoI+uX4zLjz9Q0X9wPtat1ZeBgPGoWnn\n" +
        "P14O3BFXnwFZ3zsk6za9il9HEL8zqmtqNUkmkSTwx4N6f13m82/QGw==\n" +
        "-----END CERTIFICATE-----";

    public static final String B_CERT = "-----BEGIN CERTIFICATE-----\n" +
        "MIICLTCCAdegAwIBAgIJAJ2AExZqZJGFMA0GCSqGSIb3DQEBBQUAMGAxFDASBgNVBAMMC0FsZXgg\n" +
        "Q3J1aXNlMRswGQYKCZImiZPyLGQBGRYLaGVsbG8sY3V0dXMxFjAUBgoJkiaJk/IsZAEZFgZsN3Rl\n" +
        "Y2gxEzARBgoJkiaJk/IsZAEZFgNjb20wHhcNMDgxMTI0MjI1OTAyWhcNMjgxMTE5MjI1OTAyWjBg\n" +
        "MRQwEgYDVQQDDAtBbGV4IENydWlzZTEbMBkGCgmSJomT8ixkARkWC2hlbGxvLGN1dHVzMRYwFAYK\n" +
        "CZImiZPyLGQBGRYGbDd0ZWNoMRMwEQYKCZImiZPyLGQBGRYDY29tMFwwDQYJKoZIhvcNAQEBBQAD\n" +
        "SwAwSAJBALoIWqNqb3GOwb9nscnZoeqs932ffvYJV+PAL3WyvlYbwDqF/VA9/2LfbLCkXY7Jj5t6\n" +
        "NOYnOJawXsrbR69B5hsCAwEAAaN0MHIwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBeAwEgYD\n" +
        "VR0lAQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQUEBQl5ZvB9/hTuiv6IEw9KCKBQKwwHwYDVR0jBBgw\n" +
        "FoAUEBQl5ZvB9/hTuiv6IEw9KCKBQKwwDQYJKoZIhvcNAQEFBQADQQBi6aKbvzKdQ2/PNCch9cIP\n" +
        "x9zsgyMq0B1YxjRGE3VVdVsH1y2eotOaNTyDF+swZOhP7JjOcBCMFi0Besh+Fs2H\n" +
        "-----END CERTIFICATE-----";
}
