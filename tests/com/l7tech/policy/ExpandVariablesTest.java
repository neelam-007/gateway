package com.l7tech.policy;

import com.l7tech.common.audit.Audit;
import com.l7tech.common.audit.Messages;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.message.AbstractHttpResponseKnob;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeHeader;
import com.l7tech.common.mime.MimeHeaders;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.DefaultStashManagerFactory;
import com.l7tech.server.audit.LogOnlyAuditor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class ExpandVariablesTest.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class ExpandVariablesTest extends TestCase {
    private static final Logger logger = Logger.getLogger(ExpandVariablesTest.class.getName());
    private static final Audit audit = new LogOnlyAuditor(logger);
    private static final String TINY_BODY = "<blah/>";

    /**
     * test <code>ExpandVariablesTest</code> constructor
     */
    public ExpandVariablesTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ExpandVariablesTest <code>TestCase</code>
     */
    public static Test suite() {
        return new TestSuite(ExpandVariablesTest.class);
    }

    public void setUp() throws Exception {
        // put set up code here
        Messages.getAuditDetailMessageById(0); // This really shouldn't be necessary, but somebody's gotta do it
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testSingleVariableExpand() throws Exception {
        Map variables = new HashMap();
        String value = "value_variable1";
        variables.put("var1", value);

        String inputMessage = "Blah message blah ${var1}";
        String expectedOutputMessage = "Blah message blah value_variable1";
        String processedMessage = ExpandVariables.process(inputMessage, variables, audit);
        assertTrue(processedMessage.indexOf(value) >= 0);
        assertEquals(processedMessage, expectedOutputMessage);
    }

    public void testMultipleVariableExpand() throws Exception {
        Map variables = new HashMap();
        String value1 = "value_variable1";
        String value2 = "value_variable2";
        variables.put("var1", value1);
        variables.put("var2", value2);

        String inputMessage = "Blah message blah ${var1} and more blah ${var2}";
        String expectedOutputMessage = "Blah message blah value_variable1 and more blah value_variable2";
        String processedMessage = ExpandVariables.process(inputMessage, variables, audit);
        assertTrue(processedMessage.indexOf(value1) >= 0);
        assertEquals(processedMessage, expectedOutputMessage);
    }

    public void testSingleVariableNotFound() throws Exception {
        Map<String, Object> variables = new HashMap<String, Object>();
        String value = "value_variable1";
        variables.put("var1", value);

        final String prefix = "Blah message blah ";
        String inputMessage = prefix + "${var2}";
        String out = ExpandVariables.process(inputMessage, variables, audit);
        assertEquals(out, prefix);
    }

    public void testUnterminatedRef() throws Exception {
        String[] vars = Syntax.getReferencedNames("${foo");
        assertEquals(vars.length, 0);
    }

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

    public void testMessageVariableHeader() throws Exception {
        Message foo = makeTinyRequest();
        String ctype = ExpandVariables.process("${foo.http.header.content-type}", makeVars(foo), audit);
        assertEquals("Unexpected header value", ctype, "application/octet-stream");
    }

    public void testMessageVariableMultivaluedHeaderTruncated() throws Exception {
        Message foo = makeTinyRequest();
        String magic = ExpandVariables.process("${foo.http.header.magic}", makeVars(foo), audit);
        assertEquals("Multivalued header truncated", "foo", magic);
    }

    public void testMessageVariableMultiConcatHeaderValues() throws Exception {
        Message foo = makeTinyRequest();
        String magic = ExpandVariables.process("${foo.http.headerValues.magic||}", makeVars(foo), audit);
        assertEquals("Multivalued concatenated header values", "foo|bar", magic);
    }

    public void testMessageVariableMultiLiteralHeaderValues() throws Exception {
        Message foo = makeTinyRequest();
        String[] magic = (String[]) ExpandVariables.processSingleVariableAsObject("${foo.http.headerValues.magic}", makeVars(foo), audit);
        assertTrue("Multivalued header values", Arrays.equals(new String[] {"foo", "bar"}, magic));
    }

    public void testStrictNonexistentHeader() throws Exception {
        Message foo = makeTinyRequest();

        try {
            ExpandVariables.process("${foo.http.header.nonexistent}", makeVars(foo), audit, true);
            fail("Expected IAE for nonexistent header in strict mode");
        } catch (Exception e) {
            // OK
        }
    }

    public void testStrictSuspiciousToString() throws Exception {
        Message foo = makeTinyRequest();

        try {
            ExpandVariables.process("${foo}", makeVars(foo), audit, true);
            fail("Expected IAE for suspicious toString in strict mode");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    public void testStrictStatusOnRequest() throws Exception {
        Message foo = makeTinyRequest();

        try {
            ExpandVariables.process("${foo.http.status}", makeVars(foo), audit, true);
            fail("Expected IAE for status on request");
        } catch (Exception e) {
            // OK
        }
    }

    public void testRequestBody() throws Exception {
        Message foo = makeTinyRequest();
        String body = ExpandVariables.process("${foo.mainPart}", makeVars(foo), audit, true);
        assertEquals(body, TINY_BODY);
    }

    public void testRequestBodyNotText() throws Exception {
        Message foo = new Message(DefaultStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(TINY_BODY.getBytes("UTF-8")));
        try {
            ExpandVariables.process("${foo.mainPart}", makeVars(foo), audit, true);
            fail("Expected IAE for non-text mainPart");
        } catch (Exception e) {
            // OK
        }
    }

    public void testResponseStatus() throws Exception {
        Message resp = makeResponse();
        String status = ExpandVariables.process("${foo.http.status}", makeVars(resp), audit, true);
        assertEquals("123", status);
    }

    public void testStuff() throws Exception {
        Matcher mat = Syntax.regexPattern.matcher("${foo}blahblah${bar}");
        System.out.println(mat.replaceAll(""));
    }

    private Map<String, Object> makeVars(Message foo) {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", foo);
        return vars;
    }

    private Message makeTinyRequest() throws NoSuchPartException, IOException {
        Message foo = new Message(DefaultStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(TINY_BODY.getBytes("UTF-8")));
        Map<String, MimeHeader> headers = new HashMap<String, MimeHeader>();
        headers.put("Content-Type", ContentTypeHeader.OCTET_STREAM_DEFAULT);
        foo.attachHttpRequestKnob(new HttpRequestKnobAdapter(new MimeHeaders(headers)));
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

    /**
     * Test <code>ExpandVariablesTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }

    private static class HttpRequestKnobAdapter implements HttpRequestKnob {
        private final MimeHeaders headers;

        private HttpRequestKnobAdapter(MimeHeaders headers) {
            this.headers = headers;
        }

        public HttpCookie[] getCookies() {
            return new HttpCookie[0];
        }

        public String getMethod() {
            return "POST";
        }

        public String getRequestUri() {
            return "/";
        }

        public String getRequestUrl() {
            return "http://ssg/";
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
            return names.toArray(new String[0]);
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
            return null;
        }

        public Map getParameterMap() throws IOException {
            return Collections.emptyMap();
        }

        public String[] getParameterValues(String s) throws IOException {
            return new String[0];
        }

        public Enumeration getParameterNames() throws IOException {
            return null;
        }

        public Object getConnectionIdentifier() {
            return null;
        }

        public String getQueryString() {
            return null;
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
}
