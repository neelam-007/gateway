package com.l7tech.proxy.util;

import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.protocol.DomainIdStatusCode;
import com.l7tech.common.protocol.DomainIdStatusHeader;
import static com.l7tech.common.protocol.SecureSpanConstants.HttpHeaders.HEADER_DOMAINIDSTATUS;
import static com.l7tech.security.socket.LocalTcpPeerIdentifier.*;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;
import static com.l7tech.proxy.util.DomainIdInjector.*;
import com.l7tech.message.Message;
import com.l7tech.util.Pair;
import static org.junit.Assert.*;
import org.junit.Test;

import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for DomainIdInjector class.
 */
public class DomainIdInjectorTest {
    public static final String SAMPLE_USERNAME = "joeblow";
    public static final String SAMPLE_DOMAIN = "salesdomain";
    public static final String SAMPLE_PROGRAM = "Ferocious Hard Sell 8.exe";

    public static final String UNICODE_USERNAME = "jos\u00e9hern\u00e1ndez";
    public static final String UNICODE_DOMAIN = "ingenier\u00eda";
    public static final String UNICODE_PROGRAM = "International \u0436\u2665\u0152.exe";

    private static final String WHITESPACE_AND_UNICODE2 = "test value with whitespace\n\n\t  blah  \t\n\n  and some unicode: \u0436\u2665\u0152";

    Map<String, String> socketCredentials = new LinkedHashMap<String, String>();

    void setStatics(boolean enabled, boolean always) {
        DomainIdInjector.INJECTION_ENABLED = enabled;
        DomainIdInjector.INJECT_ALWAYS = always;
    }

    GenericHttpRequestParams makeParams() {
        return new GenericHttpRequestParams();
    }

    PolicyApplicationContext makeContext() {
        Ssg ssg = new Ssg(1);
        Message request = new Message();
        Message response = new Message();

        socketCredentials.clear();
        socketCredentials.put(IDENTIFIER_USERNAME, SAMPLE_USERNAME);
        socketCredentials.put(IDENTIFIER_NAMESPACE, SAMPLE_DOMAIN);
        socketCredentials.put(IDENTIFIER_PROGRAM, SAMPLE_PROGRAM);

        return new PolicyApplicationContext(ssg, request, response, null, null, null) {
            public Map<String, String> getClientSocketCredentials() {
                return socketCredentials;
            }
        };
    }

    // Find the status header and parse it
    private DomainIdStatusHeader getStatus(GenericHttpRequestParams params) throws IOException {
        for (HttpHeader header : params.getExtraHeaders()) {
            if (HEADER_DOMAINIDSTATUS.equalsIgnoreCase(header.getName())) {
                return DomainIdStatusHeader.parseValue(header.getFullValue());
            }
        }
        return null;
    }

    private void assertIncluded(DomainIdStatusHeader stat) {
        assertNotNull(stat);
        assertEquals(DomainIdStatusCode.INCLUDED, stat.getStatus());
        final Set<Map.Entry<String,String>> paramEntries = stat.getParams().entrySet();
        assertTrue(paramEntries.contains(new Pair<String, String>(IDENTIFIER_USERNAME, HEADER_USERNAME)));
        assertTrue(paramEntries.contains(new Pair<String, String>(IDENTIFIER_NAMESPACE, HEADER_DOMAIN)));
        assertTrue(paramEntries.contains(new Pair<String, String>(IDENTIFIER_PROGRAM, HEADER_PROGRAM)));
    }

    private GenericHttpHeaders asHeaders(List<HttpHeader> headerList) {
        return new GenericHttpHeaders(headerList.toArray(new HttpHeader[headerList.size()]));
    }

    @Test
    public void testDeclined() throws IOException {
        setStatics(false, false);
        PolicyApplicationContext context = makeContext();
        GenericHttpRequestParams params = makeParams();
        context.getDomainIdInjectionFlags().enable = true;

        DomainIdInjector.injectHeaders(context, params);

        DomainIdStatusHeader stat = getStatus(params);
        assertNotNull(stat);
        assertEquals(DomainIdStatusCode.DECLINED, stat.getStatus());
    }
    
    @Test
    public void testFailed() throws IOException {
        setStatics(true, false);
        PolicyApplicationContext context = makeContext();
        GenericHttpRequestParams params = makeParams();
        context.getDomainIdInjectionFlags().enable = true;
        socketCredentials = null;

        DomainIdInjector.injectHeaders(context, params);

        DomainIdStatusHeader stat = getStatus(params);
        assertNotNull(stat);
        assertEquals(DomainIdStatusCode.FAILED, stat.getStatus());
    }

    @Test
    public void testAlways() throws IOException {
        setStatics(true, true);
        PolicyApplicationContext context = makeContext();
        GenericHttpRequestParams params = makeParams();
        context.getDomainIdInjectionFlags().enable = false;

        DomainIdInjector.injectHeaders(context, params);

        DomainIdStatusHeader stat = getStatus(params);
        assertIncluded(stat);
        HttpHeaders headers = asHeaders(params.getExtraHeaders());
        assertEquals(SAMPLE_USERNAME, headers.getOnlyOneValue(HEADER_USERNAME));
        assertEquals(SAMPLE_DOMAIN, headers.getOnlyOneValue(HEADER_DOMAIN));
        assertEquals(SAMPLE_PROGRAM, headers.getOnlyOneValue(HEADER_PROGRAM));
    }
    
    @Test
    public void testUnicodeValues() throws IOException {
        setStatics(true, true);
        PolicyApplicationContext context = makeContext();
        GenericHttpRequestParams params = makeParams();
        context.getDomainIdInjectionFlags().enable = false;
        socketCredentials.put(IDENTIFIER_USERNAME, UNICODE_USERNAME);
        socketCredentials.put(IDENTIFIER_NAMESPACE, UNICODE_DOMAIN);
        socketCredentials.put(IDENTIFIER_PROGRAM, UNICODE_PROGRAM);

        DomainIdInjector.injectHeaders(context, params);

        DomainIdStatusHeader stat = getStatus(params);
        assertIncluded(stat);
        HttpHeaders headers = asHeaders(params.getExtraHeaders());
        assertEquals(MimeUtility.encodeText(UNICODE_USERNAME, "utf-8", "q"), headers.getOnlyOneValue(HEADER_USERNAME));
        assertEquals(MimeUtility.encodeText(UNICODE_DOMAIN, "utf-8", "q"), headers.getOnlyOneValue(HEADER_DOMAIN));
        assertEquals(MimeUtility.encodeText(UNICODE_PROGRAM, "utf-8", "q"), headers.getOnlyOneValue(HEADER_PROGRAM));
    }
    
    @Test
    public void testUnicodeAndTabs() throws IOException {
        setStatics(true, true);
        PolicyApplicationContext context = makeContext();
        GenericHttpRequestParams params = makeParams();
        context.getDomainIdInjectionFlags().enable = false;
        socketCredentials.put(IDENTIFIER_USERNAME, UNICODE_USERNAME);
        socketCredentials.put(IDENTIFIER_NAMESPACE, UNICODE_DOMAIN);
        socketCredentials.put(IDENTIFIER_PROGRAM, WHITESPACE_AND_UNICODE2);

        DomainIdInjector.injectHeaders(context, params);

        DomainIdStatusHeader stat = getStatus(params);
        assertIncluded(stat);
        HttpHeaders headers = asHeaders(params.getExtraHeaders());
        assertEquals(MimeUtility.encodeText(UNICODE_USERNAME, "utf-8", "q"), headers.getOnlyOneValue(HEADER_USERNAME));
        assertEquals(MimeUtility.encodeText(UNICODE_DOMAIN, "utf-8", "q"), headers.getOnlyOneValue(HEADER_DOMAIN));
        assertEquals(MimeUtility.encodeText(WHITESPACE_AND_UNICODE2, "utf-8", "q"), headers.getOnlyOneValue(HEADER_PROGRAM));
    }

    public static GenericHttpRequestParams injectSampleHeaders() {
        return new DomainIdInjectorTest().doInjectSampleHeaders();
    }

    private GenericHttpRequestParams doInjectSampleHeaders() {
        setStatics(true, true);
        PolicyApplicationContext context = makeContext();
        GenericHttpRequestParams params = makeParams();
        context.getDomainIdInjectionFlags().enable = false;

        DomainIdInjector.injectHeaders(context, params);

        return params;
    }

    public static GenericHttpRequestParams injectUnicodeHeaders() {
        return new DomainIdInjectorTest().doInjectUnicodeHeaders();
    }

    private GenericHttpRequestParams doInjectUnicodeHeaders() {
        setStatics(true, true);
        PolicyApplicationContext context = makeContext();
        GenericHttpRequestParams params = makeParams();
        context.getDomainIdInjectionFlags().enable = false;
        socketCredentials.put(IDENTIFIER_USERNAME, UNICODE_USERNAME);
        socketCredentials.put(IDENTIFIER_NAMESPACE, UNICODE_DOMAIN);
        socketCredentials.put(IDENTIFIER_PROGRAM, UNICODE_PROGRAM);

        DomainIdInjector.injectHeaders(context, params);

        return params;
    }


}
