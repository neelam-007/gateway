package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Test WebSocketInboundHandler.
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketInboundHandlerTest {

    private WebSocketInboundHandler fixture;

    @Mock
    private WebSocketMessage message;
    @Mock
    private HttpServletRequestKnob servletRequestKnob;
    @Mock
    private HttpServletResponseKnob servletResponseKnob;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private MessageProcessor messageProcessor;
    @Mock
    private Audit audit;
    @Mock
    private AuditFactory auditFactory;

    private String[] headerNames;
    private String[] headerValues;

    @Before
    public void init() throws Exception {

        final String template = "<servletRequest><websocket><id></id><clientId></clientId><type></type><origin></origin><protocol>" +
                "</protocol><offset></offset><length></length><data></data></websocket></servletRequest>";
        Document document = XmlUtil.parse(template);

        doReturn(document).when(message).getMessageAsDocument();
        doReturn("origin").when(message).getOrigin();
        doReturn("websocket").when(message).getProtocol();
        doReturn("clientId").when(message).getClientId();

        headerNames = new String[]{"Sec-WebSocket-Protocol"};
        headerValues = new String[]{"quake.idsoftware.com"};

        doReturn(headerNames).when(servletRequestKnob).getHeaderNames();
        doReturn(headerValues).when(servletRequestKnob).getHeaderValues(anyString());

        doReturn(audit).when(auditFactory).newInstance(Mockito.any(), Mockito.any());
        doReturn("GET").when(servletRequest).getMethod();
        doReturn("Upgrade").when(servletRequest).getHeader("connection");
        Vector<String> names = new Vector<>();
                names.add("Upgrade");
        names.add("websocket");
        doReturn(names.elements()).when(servletRequest).getHeaderNames();
        doReturn(names.elements()).when(servletRequest).getHeaders(anyString());
        doReturn(new StringBuffer().append("http://yahoo.com")).when(servletRequest).getRequestURL();
        doReturn("websocket").when(servletRequest).getHeader("Upgrade");
        doReturn("HTTP/1.1").when(servletRequest).getProtocol();
        Vector<String> subProtocol = new Vector<>(0,1);
        subProtocol.add(0,headerValues[0]);
        doReturn(subProtocol.elements()).when(servletRequest).getHeaders("Sec-WebSocket-Protocol");

        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS_KEY, 0);
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_CONNECTIONS_KEY, 0);

        doReturn(AssertionStatus.NONE).when(messageProcessor).processMessage(Mockito.any());

        fixture = new WebSocketInboundHandler.WebSocketInboundHandlerBuilder()
                .withHandlerId(Goid.DEFAULT_GOID.toString())
                .withInboundPolicyGoid(Goid.DEFAULT_GOID)
                .withConnectionPolicyGoid(Goid.DEFAULT_GOID)
                .withMaxIdleTime(0)
                .withMaxConnections(0)
                .withOutboundUrl("ws://localhost.com")
                .withMessageProcessor(messageProcessor)
                .withAuditFactory(auditFactory)
                .build();
    }

    /**
     * Http Request Message headers are copied into the
     * L7 servletRequest message so they can be manipulated in Policy
     */
    @Test
    public void testInboundCopyServletRequestHeadersToL7RequestMessageHeaders(){
        Message request = new Message();
        fixture.copyHttpServletRequestHeadersToRequestMessage(servletRequestKnob, request);
        assertEquals(headerNames[0], request.getHeadersKnob().getHeaderNames()[0]);
        assertEquals(headerNames.length, request.getHeadersKnob().getHeaders().size());
    }

    /**
     * Message headers are copied into the Http Servlet Response
     */
    @Test
    public void testCopyMessageHeadersToHttpServletResponseHeaders() {
        Message response = new Message();
        response.getHeadersKnob().addHeader(headerNames[0], headerValues[0], HeadersKnob.HEADER_TYPE_HTTP);
        WebSocketConstants.setClusterProperty(WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY,
                !WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER);
        fixture.copyMessageHeadersToHttpServletResponseKnob(response, servletResponseKnob);
        assertEquals(headerNames[0], servletRequestKnob.getHeaderNames()[0]);
        assertEquals(headerNames.length, servletRequestKnob.getHeaderNames().length);
    }

    /**
     * Http Servlet Request headers are not copied into the
     * Http Servlet Response headers (default behavior)
     */
    @Test
    public void testInboundServletRequestHeadersNotCopiedToServletResponseHeaders(){
        WebSocketConstants.setClusterProperty(WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY,
                !WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER);
        fixture.copyWebSocketProtocolsToHttpServletResponse(servletRequest, servletResponse);
        Mockito.verifyZeroInteractions(servletResponse);
    }

    /**
     * Http Servlet Request headers are copied into the
     * Http Servlet Response headers
     */
    @Test
    public void testInboundServletRequestHeadersCopiedToServletResponseHeaders(){
        WebSocketConstants.setClusterProperty(WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY,
                WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER);
        fixture.copyWebSocketProtocolsToHttpServletResponse(servletRequest, servletResponse);
        verify(servletResponse).addHeader(headerNames[0], headerValues[0]);
    }

}
