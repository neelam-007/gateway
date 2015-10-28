package com.l7tech.server.policy.bundle.ssgman.restman;


import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.Functions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test the Solution Kit Manager
 */
@RunWith(MockitoJUnitRunner.class)
public class RestmanInvokerTest {

    //TODO: fix up this Test class
    @Mock
    private PolicyEnforcementContext pec;

    @Mock
    private GatewayManagementInvoker gatewayManagementInvoker;

    private RestmanInvoker restmanInvoker;


    @Before
    public void before() {
    }

    private final static String REQUEST = "ignored for these tests";

    @Test
    public void restmanInvokerError() throws Exception {
        // simulate error from restmanInvoker

        //When RestmanMessage == null
        restmanInvoker = Mockito.spy(new RestmanInvoker(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                // nothing to do in cancelled callback.
                return true;
            }
        }, gatewayManagementInvoker));
        Mockito.doReturn(null).when(restmanInvoker).getRestmanMessage(pec);

        try {
            restmanInvoker.callManagementCheckInterrupted(pec, REQUEST);
            fail("Expected: server error response.");
        } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
            assertEquals("Unexpected exception: a call result was expected.", e.getMessage());
        }

        //When RestmanMessage.isErrorType() == true
        final String response =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "           THIS IS A TEST RESTMAN MESSAGE RESPONSE\n" +
                        "</l7:Item>\n";
        Document document = XmlUtil.stringToDocument(response);
        final RestmanMessage rmMessage= Mockito.spy(new RestmanMessage(document));
        restmanInvoker = Mockito.spy(new RestmanInvoker(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                // nothing to do in cancelled callback.
                return true;
            }
        }, gatewayManagementInvoker));
        Mockito.doReturn(rmMessage).when(restmanInvoker).getRestmanMessage(pec);
        Mockito.doReturn(true).when(rmMessage).isErrorResponse();

        try {
            restmanInvoker.callManagementCheckInterrupted(pec, REQUEST);
            fail("Expected: server error response");
        } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
            assertEquals(response, e.getMessage());
        }
    }

}