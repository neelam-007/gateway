package com.l7tech.server.policy.variable;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeHeader;
import com.l7tech.common.mime.MimeHeaders;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import com.l7tech.server.policy.assertion.ServerXpathAssertion;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IteratorEnumeration;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class ExpandVariablesTest.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class ExpandVariablesTest {
    private static final Logger logger = Logger.getLogger(ExpandVariablesTest.class.getName());
    private static final Audit audit = new LogOnlyAuditor(logger);
    private static final String TINY_BODY = "<blah/>";

    @Before
    public void setUp() throws Exception {
        // put set up code here
        MessagesUtil.getAuditDetailMessageById(0); // This really shouldn't be necessary, but somebody's gotta do it
    }

    @Test
    public void testSingleVariableExpand() throws Exception {
        Map<String, String> variables = new HashMap<String, String>();
        String value = "value_variable1";
        variables.put("var1", value);

        String inputMessage = "Blah message blah ${var1}";
        String expectedOutputMessage = "Blah message blah value_variable1";
        String processedMessage = ExpandVariables.process(inputMessage, variables, audit);
        Assert.assertTrue(processedMessage.indexOf(value) >= 0);
        Assert.assertEquals(processedMessage, expectedOutputMessage);
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
        Assert.assertTrue(processedMessage.indexOf(value1) >= 0);
        Assert.assertEquals(processedMessage, expectedOutputMessage);
    }

    @Test
    public void testSingleVariableNotFound() throws Exception {
        Map<String, Object> variables = new HashMap<String, Object>();
        String value = "value_variable1";
        variables.put("var1", value);

        final String prefix = "Blah message blah ";
        String inputMessage = prefix + "${var2}";
        String out = ExpandVariables.process(inputMessage, variables, audit);
        Assert.assertEquals(out, prefix);
    }

    @Test
    public void testUnterminatedRef() throws Exception {
        String[] vars = Syntax.getReferencedNames("${foo");
        Assert.assertEquals(vars.length, 0);
    }

    @Test
    public void testMultivalueDelimiter() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new String[] { "bar", "baz"});

        String out1 = ExpandVariables.process("${foo}", vars, audit);
        Assert.assertEquals(out1, "bar, baz"); // Default delimiter is ", "

        String out2 = ExpandVariables.process("<foo><val>" + "${foo|</val><val>}" + "</val></foo>", vars, audit);
        Assert.assertEquals(out2, "<foo><val>bar</val><val>baz</val></foo>");

        String out3 = ExpandVariables.process("${foo|}", vars, audit);
        Assert.assertEquals(out3, "barbaz");

        String out4 = ExpandVariables.process("${foo||}", vars, audit);
        Assert.assertEquals(out4, "bar|baz");
    }

    @Test
    public void testMultivalueSubscript() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("foo", new Object[] { "bar", "baz" });
        }};

        String out1 = ExpandVariables.process("${foo[0]}", vars, audit);
        Assert.assertEquals(out1, "bar");

        String out2 = ExpandVariables.process("${foo[1]}", vars, audit);
        Assert.assertEquals(out2, "baz");

        // Array index out of bounds -- log a warning, return empty string
        String out3 = ExpandVariables.process("${foo[2]}", vars, audit);
        Assert.assertEquals(out3, "");

        try {
            ExpandVariables.process("${foo[asdf]}", vars, audit);
            Assert.fail("Should have thrown--non-numeric subscript");
        } catch (IllegalArgumentException e) {
        }

        try {
            ExpandVariables.process("${foo[-1]}", vars, audit);
            Assert.fail("Should have thrown--negative subscript");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test @BugNumber(6455)
    public void testUnrecognizedSuffix() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("certificate.subject.cn", new Object[] { "blah" });
            put("certificate.subject.o", new Object[] { "blorf" });
            put("certificate.subject", new Object[] { "cn=blah, o=blorf" });
        }};

        Assert.assertEquals("cn=blah, o=blorf", ExpandVariables.process("${certificate.subject}", vars, audit).toLowerCase());
        Assert.assertEquals("blah", ExpandVariables.process("${certificate.subject.cn}", vars, audit));
        Assert.assertEquals("blorf", ExpandVariables.process("${certificate.subject.o}", vars, audit));
        Assert.assertEquals("", ExpandVariables.process("${certificate.subject.nonexistent}", vars, audit));
    }

    @Test @BugNumber(6813)
    public void testBackslashEscaping() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("dn", "cn=test\\+1");
            put("dns", new String[] { "cn=test\\+1", "cn=test2", "cn=test\\+3" });
        }};

        Assert.assertEquals("cn=test\\+1", ExpandVariables.process("${dn}", vars, audit));
        Assert.assertEquals("||cn=test\\+1||cn=test2||cn=test\\+3||", ExpandVariables.process("||${dns|||}||", vars, audit));
    }

    @Test
    public void testMessageVariableHeader() throws Exception {
        Message foo = makeTinyRequest();
        String ctype = ExpandVariables.process("${foo.http.header.content-type}", makeVars(foo), audit);
        Assert.assertEquals("Unexpected header value", "application/octet-stream", ctype);
    }

    @Test
    public void testMessageVariableMultivaluedHeaderTruncated() throws Exception {
        Message foo = makeTinyRequest();
        String magic = ExpandVariables.process("${foo.http.header.magic}", makeVars(foo), audit);
        Assert.assertEquals("Multivalued header truncated", "foo", magic);
    }

    @Test
    public void testMessageVariableMultiConcatHeaderValues() throws Exception {
        Message foo = makeTinyRequest();
        String magic = ExpandVariables.process("${foo.http.headerValues.magic||}", makeVars(foo), audit);
        Assert.assertEquals("Multivalued concatenated header values", "foo|bar", magic);
    }

    @Test
    public void testMessageVariableMultiLiteralHeaderValues() throws Exception {
        Message foo = makeTinyRequest();
        String[] magic = (String[]) ExpandVariables.processSingleVariableAsObject("${foo.http.headerValues.magic}", makeVars(foo), audit);
        Assert.assertTrue("Multivalued header values", Arrays.equals(new String[] {"foo", "bar"}, magic));
    }

    @Test
    public void testMessageVariableMultiLiteralHeaderValuesCaseInsensitive() throws Exception {
        Message foo = makeTinyRequest();
        String[] magic = (String[]) ExpandVariables.processSingleVariableAsObject("${foo.http.HeaderVaLUes.magic}", makeVars(foo), audit);
        Assert.assertTrue("Multivalued header values", Arrays.equals(new String[] {"foo", "bar"}, magic));
    }

    @Test
    public void testRequestHttpParam() throws Exception {
        PolicyEnforcementContext pec = new PolicyEnforcementContext(makeTinyRequest(), new Message());

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"request.http.parameter.foo"}, audit);
        String paramValue = ExpandVariables.process("${request.http.parameter.foo}", vars, audit, true);
        Assert.assertEquals(paramValue, "bar");
    }

    @Test
    public void testRequestHttpParamCaseInsensitivity() throws Exception {
        PolicyEnforcementContext pec = new PolicyEnforcementContext(makeTinyRequest(), new Message());

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"request.http.parameter.Foo"}, audit);
        String paramValue = ExpandVariables.process("${request.http.parameter.fOO}", vars, audit, true);
        Assert.assertEquals(paramValue, "bar");
    }

    @Test
    public void testRequestHttpParamCaseInsensitivity2() throws Exception {
        PolicyEnforcementContext pec = new PolicyEnforcementContext(makeTinyRequest(), new Message());

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"request.http.parameter.Foo"}, audit);
        String paramValue = ExpandVariables.process("${request.HttP.paRAMeter.fOO}", vars, audit, true);
        Assert.assertEquals(paramValue, "bar");

        paramValue = ExpandVariables.process("${REQuest.httP.paRAMeter.fOO}", vars, audit, true);
        Assert.assertEquals(paramValue, "bar");
    }

    @Test
    public void testRequestHttpParamCasePreservation() throws Exception {
        final Enumeration<String> en = makeTinyRequest().getHttpRequestKnob().getParameterNames();
        String badname = null;
        while (en.hasMoreElements()) {
            String val = en.nextElement();
            if ("BaZ".equals(val)) return;
            if ("baz".equalsIgnoreCase(val)) badname = val;
        }
        Assert.fail("Expected to find original case, found " + badname);
    }

    @Test
    public void testMessageVariable() throws Exception {
        final Message foo = makeTinyRequest();
        PolicyEnforcementContext pec = new PolicyEnforcementContext(new Message(), new Message());
        pec.setVariable("foo", foo);

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"foo.mainPart"}, audit);
        String bodytext = ExpandVariables.process("${foo.mainPart}", vars, audit, true);
        Assert.assertEquals(bodytext, TINY_BODY);
    }

    @Test
    public void testMessageVariableCaseInsensitive() throws Exception {
        final Message foo = makeTinyRequest();
        PolicyEnforcementContext pec = new PolicyEnforcementContext(new Message(), new Message());
        pec.setVariable("fOo", foo);

        final Map<String, Object> vars = pec.getVariableMap(new String[]{"fOO.mainPart"}, audit);
        String bodytext = ExpandVariables.process("${foO.mainPart}", vars, audit, true);
        Assert.assertEquals(bodytext, TINY_BODY);
    }

    @Test
    public void testStrictNonexistentHeader() throws Exception {
        Message foo = makeTinyRequest();

        try {
            ExpandVariables.process("${foo.http.header.nonexistent}", makeVars(foo), audit, true);
            Assert.fail("Expected IAE for nonexistent header in strict mode");
        } catch (Exception e) {
            // OK
        }
    }

    @Test
    public void testStrictSuspiciousToString() throws Exception {
        Message foo = makeTinyRequest();

        try {
            ExpandVariables.process("${foo}", makeVars(foo), audit, true);
            Assert.fail("Expected IAE for suspicious toString in strict mode");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testStrictStatusOnRequest() throws Exception {
        Message foo = makeTinyRequest();

        try {
            ExpandVariables.process("${foo.http.status}", makeVars(foo), audit, true);
            Assert.fail("Expected IAE for status on request");
        } catch (Exception e) {
            // OK
        }
    }

    @Test
    public void testRequestBody() throws Exception {
        Message foo = makeTinyRequest();
        String body = ExpandVariables.process("${foo.mainPart}", makeVars(foo), audit, true);
        Assert.assertEquals(body, TINY_BODY);
    }

    @Test
    public void testRequestBodyNotText() throws Exception {
        Message foo = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(TINY_BODY.getBytes("UTF-8")));
        try {
            ExpandVariables.process("${foo.mainPart}", makeVars(foo), audit, true);
            Assert.fail("Expected IAE for non-text mainPart");
        } catch (Exception e) {
            // OK
        }
    }

    public void testCertIssuerDnToString() throws Exception {
        LdapUser u = new LdapUser(1234, "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        Assert.assertEquals("CN=OASIS Interop Test CA, O=OASIS", ExpandVariables.process("${authenticatedUser.cert.issuer}", vars, audit, true));
    }

    public void testCertIssuerDnName() throws Exception {
        LdapUser u = new LdapUser(1234, "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        Assert.assertEquals("CN=OASIS Interop Test CA, O=OASIS", ExpandVariables.process("${authenticatedUser.cert.issuer}", vars, audit, true));
    }

    public void testCertIssuerAttribute() throws Exception {
        LdapUser u = new LdapUser(1234, "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        Assert.assertEquals("OASIS", ExpandVariables.process("${authenticatedUser.cert.issuer.dn.o}", vars, audit, true));
    }

    public void testCertSubjectAttribute() throws Exception {
        LdapUser u = new LdapUser(1234, "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        Assert.assertEquals("Alice", ExpandVariables.process("${authenticatedUser.cert.subject.dn.cn}", vars, audit, true));
    }

    public void testMixedCaseCertAttributeName() throws Exception {
        LdapUser u = new LdapUser(1234, "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(TestDocuments.getWssInteropAliceCert());
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        Assert.assertEquals("Alice", ExpandVariables.process("${auTheNticAtedUser.cert.suBjeCt.dN.CN}", vars, audit, true));
    }

    public void testMultivaluedDcAttributes() throws Exception {
        LdapUser u = new LdapUser(1234, "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(CertUtils.decodeFromPEM(A_CERT));
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        String got = ExpandVariables.process("${authenticatedUser.cert.subject.dn.dc|,}", vars, audit, true);
        Assert.assertEquals("com,l7tech,locutus", got);
    }

    public void testMultivaluedDcBttributes() throws Exception {
        LdapUser u = new LdapUser(1234, "cn=Alice,dc=l7tech,dc=com", "Alice");
        u.setCertificate(CertUtils.decodeFromPEM(B_CERT));
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        vars.put("authenticatedUser", u);
        String got = ExpandVariables.process("${authenticatedUser.cert.subject.dn.dc|,}", vars, audit, true);
        Assert.assertEquals("com,l7tech,hello\\,cutus", got);
    }

    @Test
    public void testResponseStatus() throws Exception {
        Message resp = makeResponse();
        String status = ExpandVariables.process("${foo.http.status}", makeVars(resp), audit, true);
        Assert.assertEquals("123", status);
    }

    /*
    <soapenv:Envelope
        xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <soapenv:Body>
            <ns1:placeOrder
                soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://warehouse.acme.com/ws">
                <productid xsi:type="xsd:long">-9206260647417300294</productid>
                <amount xsi:type="xsd:long">1</amount>
                <price xsi:type="xsd:float">5.0</price>
                <accountid xsi:type="xsd:long">228</accountid>
            </ns1:placeOrder>
        </soapenv:Body>
    </soapenv:Envelope>
    */

    @Test
    public void testProcessSingleVariableAsDisplayableObject() throws Exception {
        final ApplicationContext spring = ApplicationContexts.getTestApplicationContext();
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        final PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(doc), new Message());

        final RequestXpathAssertion ass = new RequestXpathAssertion(new XpathExpression("/s:Envelope/s:Body/ns1:placeOrder/*", nsmap()));
        final AllAssertion all = new AllAssertion(Arrays.asList(ass, new AuditDetailAssertion("${requestXpath.elements}")));
        final ServerXpathAssertion sxa = new ServerRequestXpathAssertion(ass, spring);
        final AssertionStatus xpathStatus = sxa.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, xpathStatus);

        final Element[] els = (Element[]) context.getVariable("requestXpath.elements");
        Assert.assertEquals(4, els.length);
        
        final Object what = ExpandVariables.processSingleVariableAsDisplayableObject("${requestXpath.elements[0]}", context.getVariableMap(new String[] { "requestXpath.elements" }, audit), audit, true);
        System.out.println(what);
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
        PolicyEnforcementContext pec = new PolicyEnforcementContext(makeTinyRequest(), new Message());
        final Map<String, Object> vars = pec.getVariableMap(new String[]{}, audit);
        ExpandVariables.process("${dontexist}", vars, audit, true);
    }

    /**
     * Comprehensive test coverage for ExpandVariables.processNoFormat()
     * Ensures that no empty strings are returned as index 0
     * Tests that any preceeding text, even if a single space, is preserved
     * Tests the correct number of elements are returned when:-
     * only text is used
     * when text and single valued vars are used
     * when variables of type Message used
     * when multi valued variables backed by an Object [] and List are used
     * when a mix of text, single valued variables, variables of type Message and mulit valued variables are used
     * @throws Exception
     */
    @BugNumber(7688)
    @Test
    public void testProcessNoFormat() throws Exception{
        PolicyEnforcementContext pec = new PolicyEnforcementContext(makeTinyRequest(), new Message());
        final Map<String, Object> vars = pec.getVariableMap(new String[]{}, audit);
        vars.put("varname", "value");
        List<Object> paramValue = ExpandVariables.processNoFormat("${varname}", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 1, paramValue.size());
        Assert.assertEquals("varname's value not found", "value", paramValue.get(0));

        paramValue = ExpandVariables.processNoFormat("preceeding text ${varname}", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 2, paramValue.size());
        Assert.assertEquals("Preceeding text should have been found", "preceeding text ", paramValue.get(0));
        Assert.assertEquals("varname's value not found", "value", paramValue.get(1));

        //preserve any empty formatting for what ever reason
        paramValue = ExpandVariables.processNoFormat(" ${varname}", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 2, paramValue.size());
        Assert.assertEquals("Preceeding empty string should have been preserved", " ", paramValue.get(0));
        Assert.assertEquals("varname's value not found", "value", paramValue.get(1));

        //preserve any post variable formatting
        paramValue = ExpandVariables.processNoFormat(" ${varname} ", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 3, paramValue.size());
        Assert.assertEquals("Preceeding empty string should have been preserved", " ", paramValue.get(0));
        Assert.assertEquals("Trailing empty string should have been preserved", " ", paramValue.get(2));
        Assert.assertEquals("varname's value not found", "value", paramValue.get(1));

        paramValue = ExpandVariables.processNoFormat(" ${varname} trailing text", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 3, paramValue.size());
        Assert.assertEquals("Preceeding empty string should have been preserved", " ", paramValue.get(0));
        Assert.assertEquals("Trailing empty string should have been preserved", " trailing text", paramValue.get(2));
        Assert.assertEquals("varname's value not found", "value", paramValue.get(1));

        //Test variable of type Message support
        String xml = "<donal>value</donal>";
        Document doc = XmlUtil.parse(xml);
        Message m = new Message(doc);
        vars.put("MESSAGE_VAR", m);

        paramValue = ExpandVariables.processNoFormat("${MESSAGE_VAR}", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 1, paramValue.size());
        Assert.assertTrue("varname's value not found", paramValue.get(0) instanceof Message);

        //test mix of single value variable, message variable and text
        paramValue = ExpandVariables.processNoFormat("The single valued var ${varname} and the Message var ${MESSAGE_VAR} test", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 5, paramValue.size());
        Assert.assertEquals("Incorrect text value extracted", "The single valued var ", paramValue.get(0));
        Assert.assertEquals("varname's value not found", "value", paramValue.get(1));
        Assert.assertEquals("Incorrect text value extracted", " and the Message var ", paramValue.get(2));
        Assert.assertTrue("MESSAGE_VAR's value not found", paramValue.get(3) instanceof Message);
        Assert.assertEquals("Incorrect text value extracted", " test", paramValue.get(4));

        //test coverage for multi valued variables
        vars.put("MULTI_VALUED_VAR", new Object[]{"one", m, "three"});
        paramValue = ExpandVariables.processNoFormat("${MULTI_VALUED_VAR}", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 3, paramValue.size());
        Assert.assertEquals("Incorret value found from multi valued variable", "one", paramValue.get(0));
        Assert.assertTrue("Message not found from multi valued variable", paramValue.get(1) instanceof Message);
        Assert.assertEquals("Incorret value found from multi valued variable", "three", paramValue.get(2));

        //test coverage for multi valued variables surrounded by other text and variables
        paramValue = ExpandVariables.processNoFormat("The single valued var ${varname} and multi valued ${MULTI_VALUED_VAR} the Message var ${MESSAGE_VAR} test", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 9, paramValue.size());
        Assert.assertEquals("Incorrect text value extracted", "The single valued var ", paramValue.get(0));
        Assert.assertEquals("varname's value not found", "value", paramValue.get(1));
        Assert.assertEquals("Incorrect text value extracted", " and multi valued ", paramValue.get(2));
        Assert.assertEquals("Incorret value found from multi valued variable", "one", paramValue.get(3));
        Assert.assertTrue("Message not found from multi valued variable", paramValue.get(4) instanceof Message);
        Assert.assertEquals("Incorret value found from multi valued variable", "three", paramValue.get(5));
        Assert.assertEquals("Incorrect text value extracted", " the Message var ", paramValue.get(6));
        Assert.assertTrue("MESSAGE_VAR's value not found", paramValue.get(7) instanceof Message);
        Assert.assertEquals("Incorrect text value extracted", " test", paramValue.get(8));

        //Test list backed multi valued variable
        List<Object> testList = new ArrayList<Object>();
        testList.add("one");
        testList.add(m);
        testList.add("three");
        
        vars.put("LIST_VAR", new Object[]{"one", m, "three"});
        paramValue = ExpandVariables.processNoFormat("${LIST_VAR}", vars, audit, true);
        Assert.assertEquals("Incorrect number of values found", 3, paramValue.size());
        Assert.assertEquals("Incorret value found from multi valued variable", "one", paramValue.get(0));
        Assert.assertTrue("Message not found from multi valued variable", paramValue.get(1) instanceof Message);
        Assert.assertEquals("Incorret value found from multi valued variable", "three", paramValue.get(2));

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
        return foo;
    }

    private Message makeResponse() throws Exception {
        Message foo = new Message();
        foo.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
            {
                setStatus(123);
            }
            public void addCookie(HttpCookie cookie) {
                throw new UnsupportedOperationException();
            }
        });
        return foo;
    }

    private static class HttpRequestKnobAdapter implements HttpRequestKnob {
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

        public HttpCookie[] getCookies() {
            return new HttpCookie[0];
        }

        public HttpMethod getMethod() {
            return HttpMethod.POST;
        }

        @Override
        public String getMethodAsString() {
            return getMethod().name();
        }

        public String getRequestUri() {
            return uri;
        }

        public String getRequestUrl() {
            return "http://ssg" + uri;
        }

        public URL getRequestURL() {
            try {
                return new URL(getRequestUrl());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }

        public long getDateHeader(String name) throws ParseException {
            return Long.valueOf(headers.get(name).getMainValue());
        }

        public int getIntHeader(String name) {
            return Integer.valueOf(headers.get(name).getMainValue());
        }

        public String getHeaderSingleValue(String name) throws IOException {
            return getHeaderFirstValue(name);
        }

        public String getHeaderFirstValue(String name) {
            final MimeHeader header = headers.get(name);
            if (header == null) return null;
            return header.getMainValue();
        }

        public String[] getHeaderNames() {
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < headers.size(); i++) {
                MimeHeader header = headers.get(i);
                names.add(header.getName());
            }
            return names.toArray(new String[names.size()]);
        }

        public String[] getHeaderValues(String name) {
            if ("magic".equalsIgnoreCase(name)) {
                return new String[] { "foo", "bar" };
            }
            return new String[] { headers.get(name).getMainValue() };
        }

        public X509Certificate[] getClientCertificate() throws IOException {
            throw new UnsupportedOperationException();
        }

        public boolean isSecure() {
            return false;
        }

        public String getParameter(String name) throws IOException {
            String[] ss = params.get(name);
            if (ss == null || ss.length == 0) return null;
            return ss[0];
        }

        public Map getParameterMap() throws IOException {
            return params;
        }

        public String[] getParameterValues(String s) throws IOException {
            return params.get(s);
        }

        public Enumeration<String> getParameterNames() throws IOException {
            return new IteratorEnumeration<String>(params.keySet().iterator());
        }

        public Object getConnectionIdentifier() {
            return null;
        }

        public String getQueryString() {
            return getRequestURL().getQuery();
        }

        public String getRemoteAddress() {
            return "127.0.0.1";
        }

        public String getRemoteHost() {
            return "127.0.0.1";
        }

        public int getLocalPort() {
            return 8080;
        }

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
