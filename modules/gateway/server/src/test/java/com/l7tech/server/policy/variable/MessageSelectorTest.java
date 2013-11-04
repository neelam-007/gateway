package com.l7tech.server.policy.variable;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.DefaultSyntaxErrorHandler;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.management.config.monitoring.Header;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class MessageSelectorTest {

    private static final Logger logger = Logger.getLogger(MessageSelectorTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);
    private MessageSelector selector;
    private Message message;
    private JmsKnobStub jmsKnob;
    private Map<String, String> headers;
    private DefaultSyntaxErrorHandler handler;

    private String messageWithBinaryMimePart =
                    "UFVUIC9wdXR0ZXIgSFRUUC8xLjENCkFjY2VwdC1FbmNvZGluZzogZ3ppcCxkZWZsYXRlDQpDb250\n" +
                    "ZW50LVR5cGU6IG11bHRpcGFydC9mb3JtLWRhdGE7IGJvdW5kYXJ5PSItLS0tPV9QYXJ0XzBfMjg3\n" +
                    "NzQ1NDU5LjEzNDU3NjYxNDMzMzEiDQpNSU1FLVZlcnNpb246IDEuMA0KVXNlci1BZ2VudDogSmFr\n" +
                    "YXJ0YSBDb21tb25zLUh0dHBDbGllbnQvMy4xDQpIb3N0OiBsb2NhbGhvc3Q6ODA4MA0KQ29udGVu\n" +
                    "dC1MZW5ndGg6IDExNDQNCg0KDQotLS0tLS09X1BhcnRfMF8yODc3NDU0NTkuMTM0NTc2NjE0MzMz\n" +
                    "MQ0KQ29udGVudC1UeXBlOiBtdWx0aXBhcnQvZm9ybS1kYXRhDQpDb250ZW50LVRyYW5zZmVyLUVu\n" +
                    "Y29kaW5nOiA4Yml0DQoNCjxhPmlucHV0PC9hPg0KLS0tLS0tPV9QYXJ0XzBfMjg3NzQ1NDU5LjEz\n" +
                    "NDU3NjYxNDMzMzENCkNvbnRlbnQtVHlwZTogYXBwbGljYXRpb24vb2N0ZXQtc3RyZWFtOyBuYW1l\n" +
                    "PXBhcnNlcg0KQ29udGVudC1UcmFuc2Zlci1FbmNvZGluZzogYmluYXJ5DQpDb250ZW50LURpc3Bv\n" +
                    "c2l0aW9uOiBmb3JtLWRhdGE7IG5hbWU9InBhcnNlciI7IGZpbGVuYW1lPSJwYXJzZXIiDQoNCjCC\n" +
                    "AwYwggHuoAMCAQICCEJdB6diKkd1MA0GCSqGSIb3DQEBDAUAMCExEDAOBgNVBBETB2FiY2RlZmcx\n" +
                    "DTALBgNVBAMTBGZyZWQwHhcNMTIwNDA1MjExNDMzWhcNMTcwNDA0MjExNDMzWjAhMRAwDgYDVQQR\n" +
                    "EwdhYmNkZWZnMQ0wCwYDVQQDEwRmcmVkMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
                    "t9cke58z/nUIQJqJDBd3J9wFQxAhSujDaUXtmBkJCNTHJjYQxEDaGDeP7M4wpfuBmLJ5Ugv8EvWU\n" +
                    "yaD208bFjP69e1ubnfIjnkuTF6uElkR5MGKTM+aXeLDQkfkJtod/7T7B5lktrsVU8fwgZgk/cBwl\n" +
                    "QhN0sBARSY5Vloph3yZoYk7MIj5jgv7BCPk/MdD6nPOFMt6LvTY1Ffrb8ObUN1OVCfhfkcXaS0MF\n" +
                    "jFttN4FfiHxFRbPlu7rTKNw4yIMsb2tSjVv4RGm+VqpabcIi5W8m3O7rtOsfgS/FU7wp/b94MOzW\n" +
                    "eImq/jXEzTHDzYXM/wyolweTsrmtTLo1WdBhXwIDAQABo0IwQDAdBgNVHQ4EFgQUerKZR0i8molf\n" +
                    "DUdWwbKjTXBvZ9owHwYDVR0jBBgwFoAUerKZR0i8molfDUdWwbKjTXBvZ9owDQYJKoZIhvcNAQEM\n" +
                    "BQADggEBAJLYZRs4JvkYFdU1NrlhioJFujDtettOcLnoypPAggzdXIz6DR36ewfp7dQHgmefWreh\n" +
                    "ZXMwQw3q9e0GuUnpQhnlldTi6O3rujgNwbRtgULYRnNmIQgWXysa24aB5YIPpnOQIT1iekyoHceW\n" +
                    "/miwCysbr8ve2pptn5uN1t/X8Kys0Z4fnckLXbOGhYpdaxgaapI4kVVkmhM1PtAtdHzOQJcaddwP\n" +
                    "Az5fJbxDh/fPmEyR0W8uu2QnSZ68bDrw8FoZGqjRvkKvrPy1bjVoxVV0oWUFsIB189N3UFUdNm2V\n" +
                    "CGfOoMRBxcJR3u7HewnBsfn+wt7yofnptzLY3WFS6TampAgNCi0tLS0tLT1fUGFydF8wXzI4Nzc0\n" +
                    "NTQ1OS4xMzQ1NzY2MTQzMzMxLS0NCg==";

    private static String base64EncodedMimePart =
                    "MIIDBjCCAe6gAwIBAgIIQl0Hp2IqR3UwDQYJKoZIhvcNAQEMBQAwITEQMA4GA1UE" +
                    "ERMHYWJjZGVmZzENMAsGA1UEAxMEZnJlZDAeFw0xMjA0MDUyMTE0MzNaFw0xNzA0" +
                    "MDQyMTE0MzNaMCExEDAOBgNVBBETB2FiY2RlZmcxDTALBgNVBAMTBGZyZWQwggEi" +
                    "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC31yR7nzP+dQhAmokMF3cn3AVD" +
                    "ECFK6MNpRe2YGQkI1McmNhDEQNoYN4/szjCl+4GYsnlSC/wS9ZTJoPbTxsWM/r17" +
                    "W5ud8iOeS5MXq4SWRHkwYpMz5pd4sNCR+Qm2h3/tPsHmWS2uxVTx/CBmCT9wHCVC" +
                    "E3SwEBFJjlWWimHfJmhiTswiPmOC/sEI+T8x0Pqc84Uy3ou9NjUV+tvw5tQ3U5UJ" +
                    "+F+RxdpLQwWMW203gV+IfEVFs+W7utMo3DjIgyxva1KNW/hEab5WqlptwiLlbybc" +
                    "7uu06x+BL8VTvCn9v3gw7NZ4iar+NcTNMcPNhcz/DKiXB5Oyua1MujVZ0GFfAgMB" +
                    "AAGjQjBAMB0GA1UdDgQWBBR6splHSLyaiV8NR1bBsqNNcG9n2jAfBgNVHSMEGDAW" +
                    "gBR6splHSLyaiV8NR1bBsqNNcG9n2jANBgkqhkiG9w0BAQwFAAOCAQEAkthlGzgm" +
                    "+RgV1TU2uWGKgkW6MO16205wuejKk8CCDN1cjPoNHfp7B+nt1AeCZ59at6FlczBD" +
                    "Der17Qa5SelCGeWV1OLo7eu6OA3BtG2BQthGc2YhCBZfKxrbhoHlgg+mc5AhPWJ6" +
                    "TKgdx5b+aLALKxuvy97amm2fm43W39fwrKzRnh+dyQtds4aFil1rGBpqkjiRVWSa" +
                    "EzU+0C10fM5Alxp13A8DPl8lvEOH98+YTJHRby67ZCdJnrxsOvDwWhkaqNG+Qq+s" +
                    "/LVuNWjFVXShZQWwgHXz03dQVR02bZUIZ86gxEHFwlHe7sd7CcGx+f7C3vKh+em3" +
                    "MtjdYVLpNqakCA==";

    @Before
    public void setup() {
        selector = new MessageSelector();
        headers = new HashMap<String, String>();
        headers.put("h1", "h1value");
        headers.put("h2", "h2value");
        headers.put("h3", "h3value");
        jmsKnob = new JmsKnobStub(new Goid(0,1234L), false, null);
        jmsKnob.setHeaders(headers);
        message = new Message();
        message.attachJmsKnob(jmsKnob);
        handler = new DefaultSyntaxErrorHandler(new TestAudit());
    }

    @Test
    public void selectArray() throws IOException, NoSuchPartException {

        ContentTypeHeader MULTIPART_RELATED = ContentTypeHeader.parseValue("multipart/related; charset=utf-8; boundary=\"----=_Part_0_287745459.1345766143331\"");
        final Message message = new Message();
        message.initialize(new ByteArrayStashManager(), MULTIPART_RELATED,
                new ByteArrayInputStream(HexUtils.decodeBase64(messageWithBinaryMimePart)));

        Audit audit = mock(Audit.class);

        Map<String,Object> vars = new HashMap<String,Object>();
        vars.put("request",message);

        PartInfo part = (PartInfo) ExpandVariables.processSingleVariableAsObject("${request.parts.2}",vars,audit);
        InputStream is = part.getInputStream(false);
        byte partBytes[] = part.getBytesIfAlreadyAvailable();
        assertTrue( HexUtils.encodeBase64(partBytes,true).equals(base64EncodedMimePart) );
    }

    @Test
    public void selectJmsHeaderNames() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.headernames", handler, false);

        final String[] selectedValue = (String[]) selection.getSelectedValue();
        final List<String> asList = Arrays.asList(selectedValue);
        assertEquals(3, asList.size());
        assertTrue(asList.contains("h1"));
        assertTrue(asList.contains("h2"));
        assertTrue(asList.contains("h3"));
    }

    @Test
    public void selectJmsHeaderNamesNone() {
        headers.clear();

        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.headernames", handler, false);

        final String[] selectedValue = (String[]) selection.getSelectedValue();
        assertEquals(0, selectedValue.length);
    }

    @Test
    public void selectJmsHeader() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.header.h2", handler, false);

        final String selectedValue = (String) selection.getSelectedValue();
        assertEquals("h2value", selectedValue);
    }

    @Test
    public void selectJmsHeaderNotFound() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.header.h4", handler, false);

        assertNull(selection);
    }

    @Test
    public void selectJmsHeaderValues() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.allheadervalues", handler, false);

        final Object[] selectedValue = (Object[]) selection.getSelectedValue();
        final List<Object> asList = Arrays.asList(selectedValue);
        assertEquals(3, asList.size());
        assertTrue(asList.contains("h1:h1value"));
        assertTrue(asList.contains("h2:h2value"));
        assertTrue(asList.contains("h3:h3value"));
    }

    @Test
    public void selectJmsHeaderValuesNone() {
        headers.clear();

        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.allheadervalues", handler, false);

        final Object[] selectedValue = (Object[]) selection.getSelectedValue();
        assertEquals(0, selectedValue.length);
    }

    @Test
    public void testMultiValue() {
        PolicyEnforcementContext c = PolicyEnforcementContextFactory.createPolicyEnforcementContext(message, message);
        AuthenticationContext ac = c.getAuthenticationContext(message);
        User user1 = new UserBean(new Goid(0,123), "Tester1");
        User user2 = new UserBean(new Goid(0,123), "Tester2");
        AuthenticationResult ar1 = new AuthenticationResult( user1, new OpaqueSecurityToken());
        AuthenticationResult ar2 = new AuthenticationResult( user2, new OpaqueSecurityToken());
        ac.addAuthenticationResult(ar1);
        ac.addAuthenticationResult(ar2);
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "authenticatedusers", handler, false);
        assertEquals(user2.getName(), ((String[])selection.getSelectedValue())[1]);
        assertEquals(ac.getAllAuthenticationResults().size(), ((String[])selection.getSelectedValue()).length);

        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("request", message);
        }};

        assertEquals("Tester1, Tester2", ExpandVariables.process("${request.authenticatedusers}", vars, audit));
        assertEquals("", ExpandVariables.process("${request.authenticatedusers.0}", vars, audit));
        assertEquals("", ExpandVariables.process("${request.authenticatedusers.0.login}", vars, audit));
        //Should return the last authentication result
        assertEquals(user2.getName(), ExpandVariables.process("${request.authenticateduser}", vars, audit));
        assertEquals(user1.getName(), ExpandVariables.process("${request.authenticateduser.0}", vars, audit));
        assertEquals("", ExpandVariables.process("${request.authenticateduser.0.login}", vars, audit));
        assertEquals(user2.getName(), ExpandVariables.process("${request.authenticateduser.1}", vars, audit));
        assertEquals(user2.getName(), ExpandVariables.process("${request.authenticateduser.login}", vars, audit));

        List<HttpHeader> headers = new ArrayList<HttpHeader>();
        HttpHeader h1 = new GenericHttpHeader(new Header("h1", "h1value"));
        HttpHeader h12 = new GenericHttpHeader(new Header("h1", "h12value"));
        HttpHeader h2 = new GenericHttpHeader(new Header("h2", "h2value"));
        headers.add(h1);
        headers.add(h12);
        headers.add(h2);

        HttpRequestKnobStub rk = new HttpRequestKnobStub(headers);

        message.attachHttpRequestKnob(rk);

        assertEquals("h1, h1, h2", ExpandVariables.process("${request.http.headernames}", vars, audit));
        assertEquals("h1:h1value, h12value, h1:h1value, h12value, h2:h2value", ExpandVariables.process("${request.http.allheaderValues}", vars, audit));
        assertEquals("h1value, h12value", ExpandVariables.process("${request.http.headerValues.h1}", vars, audit));

    }

    @BugNumber(13278)
    @Test
    public void testMultiValueLength() {
        PolicyEnforcementContext c = PolicyEnforcementContextFactory.createPolicyEnforcementContext(message, message);
        AuthenticationContext ac = c.getAuthenticationContext(message);
        User user1 = new UserBean(new Goid(0,123), "Tester1");
        User user2 = new UserBean(new Goid(0,123), "Tester2");
        AuthenticationResult ar1 = new AuthenticationResult( user1, new OpaqueSecurityToken());
        AuthenticationResult ar2 = new AuthenticationResult( user2, new OpaqueSecurityToken());
        ac.addAuthenticationResult(ar1);
        ac.addAuthenticationResult(ar2);
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "authenticatedusers", handler, false);
        assertEquals(user2.getName(), ((String[])selection.getSelectedValue())[1]);
        assertEquals(ac.getAllAuthenticationResults().size(), ((String[])selection.getSelectedValue()).length);

        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("request", message);
        }};

        assertEquals(Integer.toString(ac.getAllAuthenticationResults().size()), ExpandVariables.process("${request.authenticatedusers.length}", vars, audit));
        //Should return the last authentication result
        assertEquals("", ExpandVariables.process("${request.authenticateduser.length}", vars, audit));

        assertEquals(Integer.toString(ac.getAllAuthenticationResults().size()), ExpandVariables.process("${request.authenticateddns.length}", vars, audit));

        List<HttpHeader> headers = new ArrayList<HttpHeader>();
        HttpHeader h1 = new GenericHttpHeader(new Header("h1", "h1value"));
        HttpHeader h12 = new GenericHttpHeader(new Header("h1", "h12value"));
        HttpHeader h2 = new GenericHttpHeader(new Header("h2", "h2value"));
        headers.add(h1);
        headers.add(h12);
        headers.add(h2);

        HttpRequestKnobStub rk = new HttpRequestKnobStub(headers);

        message.attachHttpRequestKnob(rk);

        assertEquals(Integer.toString(headers.size()), ExpandVariables.process("${request.http.headernames.length}", vars, audit));
        assertEquals("2", ExpandVariables.process("${request.http.headerValues.h1.length}", vars, audit));
        assertEquals(Integer.toString(headers.size()), ExpandVariables.process("${request.http.allheaderValues.length}", vars, audit));

    }

    @BugNumber(13278)
    @Test
    public void testLengthAsVariableNameButNotSuffix() {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("request", message);
        }};

        List<HttpHeader> headers = new ArrayList<HttpHeader>();
        HttpHeader h1 = new GenericHttpHeader(new Header("h1.length", "h1value"));
        HttpHeader h12 = new GenericHttpHeader(new Header("h1.length", "h12value"));
        HttpHeader h2 = new GenericHttpHeader(new Header("h2", "h2value"));
        headers.add(h1);
        headers.add(h12);
        headers.add(h2);

        HttpRequestKnobStub rk = new HttpRequestKnobStub(headers);

        message.attachHttpRequestKnob(rk);

        assertEquals("h1value, h12value", ExpandVariables.process("${request.http.headerValues.h1.length}", vars, audit));
        assertEquals("2", ExpandVariables.process("${request.http.headerValues.h1.length.length}", vars, audit));

    }

    @Test
    public void testMessageBufferDisallowed() throws Exception {
        final Message request = new Message();
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("request", request);
        }};

        assertEquals("uninitialized", ExpandVariables.process("${request.buffer.status}", vars, audit));
        assertEquals("true", ExpandVariables.process("${request.buffer.allowed}", vars, audit));

        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream("foo".getBytes()));

        assertEquals("unread", ExpandVariables.process("${request.buffer.status}", vars, audit));
        assertEquals("true", ExpandVariables.process("${request.buffer.allowed}", vars, audit));

        request.getMimeKnob().setBufferingDisallowed(true);

        assertEquals("unread", ExpandVariables.process("${request.buffer.status}", vars, audit));
        assertEquals("false", ExpandVariables.process("${request.buffer.allowed}", vars, audit));

        request.getMimeKnob().getEntireMessageBodyAsInputStream().close();

        assertEquals("gone", ExpandVariables.process("${request.buffer.status}", vars, audit));
        assertEquals("false", ExpandVariables.process("${request.buffer.allowed}", vars, audit));
    }

    @Test
    public void testMessageBufferAllowed() throws Exception {
        final Message request = new Message();
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("request", request);
        }};

        assertEquals("uninitialized", ExpandVariables.process("${request.buffer.status}", vars, audit));
        assertEquals("true", ExpandVariables.process("${request.buffer.allowed}", vars, audit));

        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream("foo".getBytes()));

        assertEquals("unread", ExpandVariables.process("${request.buffer.status}", vars, audit));
        assertEquals("true", ExpandVariables.process("${request.buffer.allowed}", vars, audit));

        request.getMimeKnob().getEntireMessageBodyAsInputStream().close();

        assertEquals("buffered", ExpandVariables.process("${request.buffer.status}", vars, audit));
        assertEquals("true", ExpandVariables.process("${request.buffer.allowed}", vars, audit));
    }

    @Test
    public void selectHeaderFromHeadersKnob() {
        message.getHeadersKnob().addHeader("test", "value");
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "http.header.test", handler, false);
        final String selectedValue = (String) selection.getSelectedValue();
        assertEquals("value", selectedValue);
    }

    @Test
    public void selectHeaderFromRequestKnobBeforeHeadersKnob() {
        message.attachHttpRequestKnob(new HttpRequestKnobStub(Collections.<HttpHeader>singletonList(new GenericHttpHeader("test", "requestKnobValue"))));
        message.getHeadersKnob().addHeader("test", "headersKnobValue");
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "http.header.test", handler, false);
        final String selectedValue = (String) selection.getSelectedValue();
        assertEquals("requestKnobValue", selectedValue);
    }

    @Test
    public void selectHeaderFromHttpOutboundRequestKnobBeforeHeadersKnob() {
        HttpOutboundRequestFacet.getOrCreateHttpOutboundRequestKnob(message);
        message.getKnob(OutboundHeadersKnob.class).addHeader("test", "onOutboundKnob");
        message.getHeadersKnob().addHeader("test", "onHeadersKnob");
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "http.header.test", handler, false);
        final String selectedValue = (String) selection.getSelectedValue();
        assertEquals("onOutboundKnob", selectedValue);
    }

    @Test
    public void selectHeaderFromResponseKnobBeforeHeadersKnob() {
        final HttpServletResponseKnob httpResponseKnob = new HttpServletResponseKnob(new MockHttpServletResponse());
        httpResponseKnob.addHeader("test", "onResponseKnob");
        message.attachHttpResponseKnob(httpResponseKnob);
        message.getHeadersKnob().addHeader("test", "onHeadersKnob");
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "http.header.test", handler, false);
        final String selectedValue = (String) selection.getSelectedValue();
        assertEquals("onResponseKnob", selectedValue);
    }

    @Test
    public void selectHeaderFromInboundResponseKnobBeforeHeadersKnob() {
        final HttpInboundResponseFacet facet = new HttpInboundResponseFacet();
        facet.setHeaderSource(new HttpHeadersHaver() {
            @Override
            public HttpHeaders getHeaders() {
                return new GenericHttpHeaders(new HttpHeader[]{new GenericHttpHeader("test", "onInboundResponseKnob")});
            }
        });
        message.attachKnob(HttpInboundResponseKnob.class, facet);
        message.getHeadersKnob().addHeader("test", "onHeadersKnob");
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "http.header.test", handler, false);
        final String selectedValue = (String) selection.getSelectedValue();
        assertEquals("onInboundResponseKnob", selectedValue);
    }
}
