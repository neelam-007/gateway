package com.l7tech.external.assertions.websocket.server;

import com.l7tech.message.Header;
import com.sun.ws.management.enumeration.Enumeration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

/*
 * This class was created to support policy processing for messages passed via TCP (through websockets).
 * It supports HttpServletRequests that are retrieved by calling servletUpgradeRequest.getHttpServletRequest().
 */
@RunWith(MockitoJUnitRunner.class)
public class MockHttpServletRequestTest {

    @Mock
    private HttpServletRequest servletRequest;

    MockHttpServletRequest mockHttpServletRequest;

    @Test
    public void populateHeadersWithNoErrorsWhenConnectionPolicyHeadersAreNull() {
        Collection connectionPolicyHeaders = null;
        Mockito.when(servletRequest.getHeaderNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getAttributeNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getParameterNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getLocales()).thenReturn(new Vector().elements());

        mockHttpServletRequest = new MockHttpServletRequest(servletRequest, connectionPolicyHeaders);
        Assert.assertNotNull(mockHttpServletRequest.getHeaderNames());
    }

    @Test
    public void populateHeadersVerifyNewHeadersAreAdded() {
        String headerKey = "myHeader";
        String headerValue = "myHeaderValue";

        Collection connectionPolicyHeaders = new ArrayList();
        connectionPolicyHeaders.add(getHeader(headerKey, headerValue));
        Mockito.when(servletRequest.getHeaderNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getAttributeNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getParameterNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getLocales()).thenReturn(new Vector().elements());

        mockHttpServletRequest = new MockHttpServletRequest(servletRequest, connectionPolicyHeaders);
        Assert.assertEquals(headerValue, mockHttpServletRequest.getHeaders(headerKey).nextElement());
    }

    @Test
    public void populateHeadersVerifyWhenMultipleHeadersAreAdded() {
        String headerKey1 = "myHeader1";
        String headerValue1 = "myHeaderValue1";
        String headerKey2 = "myHeader2";
        String headerValue2 = "myHeaderValue2";

        Collection connectionPolicyHeaders = new ArrayList();
        connectionPolicyHeaders.add(getHeader(headerKey1, headerValue1));
        connectionPolicyHeaders.add(getHeader(headerKey2, headerValue2));

        Mockito.when(servletRequest.getHeaderNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getAttributeNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getParameterNames()).thenReturn(new Vector().elements());
        Mockito.when(servletRequest.getLocales()).thenReturn(new Vector().elements());

        mockHttpServletRequest = new MockHttpServletRequest(servletRequest, connectionPolicyHeaders);

        Assert.assertEquals(headerValue1, mockHttpServletRequest.getHeaders(headerKey1).nextElement());
        Assert.assertEquals(headerValue2, mockHttpServletRequest.getHeaders(headerKey2).nextElement());
    }

    private Header getHeader(String key, String value) {
        return new Header(key, value, "String");
    }
}
