/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy;

import com.l7tech.common.message.Message;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientRequestXpathAssertion;
import com.l7tech.proxy.policy.assertion.ClientResponseXpathAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class ClientXpathAssertionsTest extends TestCase {
    private static Logger log = Logger.getLogger(ClientXpathAssertionsTest.class.getName());
    private static final String PREFIX = "tadwj";

    public ClientXpathAssertionsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ClientXpathAssertionsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testClientResponseXpathAssertion() throws Exception {
        Ssg ssg = new Ssg(1);

        // build simulated placeOrder response (we'll fake it with the request XML though)
        Document placeorderDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        PolicyApplicationContext placeorderRes = makereq(ssg, placeorderDoc);

        // build simulated getQuote response (we'll faek it with the request XML though)
        Document getquoteDoc = TestDocuments.getTestDocument(TestDocuments.ETTK_SIGNED_REQUEST);
        PolicyApplicationContext getquoteRes = makereq(ssg, getquoteDoc);

        // build xpath assertion that matches placeorder, with a namespace prefix
        Map placeorderNsmap = XpathBasedAssertion.createDefaultNamespaceMap();
        placeorderNsmap.put(PREFIX, "http://warehouse.acme.com/ws");
        XpathExpression placeorderXpath = new XpathExpression("//" + PREFIX + ":placeOrder", placeorderNsmap);
        ResponseXpathAssertion placeorderRxa = new ResponseXpathAssertion(placeorderXpath);
        ClientResponseXpathAssertion placeorderCrxa = new ClientResponseXpathAssertion(placeorderRxa);

        // build xpath assertion that matches getQuote, without any namespace prefix
        XpathExpression getquoteXpath = new XpathExpression("//symbol", XpathBasedAssertion.createDefaultNamespaceMap());
        ResponseXpathAssertion getquoteRxa = new ResponseXpathAssertion(getquoteXpath);
        ClientResponseXpathAssertion getquoteCrxa = new ClientResponseXpathAssertion(getquoteRxa);

        // test positive match with namespace
        AssertionStatus result = placeorderCrxa.unDecorateReply(placeorderRes);
        assertEquals(result, AssertionStatus.NONE);

        // test positive match without namespace
        result = getquoteCrxa.unDecorateReply(getquoteRes);
        assertEquals(result, AssertionStatus.NONE);

        // test negative match1
        result = placeorderCrxa.unDecorateReply(getquoteRes);
        assertEquals(result, AssertionStatus.FALSIFIED);

        // test negative match2
        result = getquoteCrxa.unDecorateReply(placeorderRes);
        assertEquals(result, AssertionStatus.FALSIFIED);

        // test invalid xpath expression
        XpathExpression badXpath = new XpathExpression("!", XpathBasedAssertion.createDefaultNamespaceMap());
        ResponseXpathAssertion badRxa = new ResponseXpathAssertion(badXpath);
        ClientResponseXpathAssertion badCrxa = new ClientResponseXpathAssertion(badRxa);
        try {
            result = badCrxa.unDecorateReply(placeorderRes);
            fail("Failed to throw expected exception due to bad xpath");
        } catch (PolicyAssertionException e) {
            log.info("The correct exception was thrown.");
        }
    }

    private PolicyApplicationContext makereq(Ssg ssg, Document d) {
        try {
            return new PolicyApplicationContext(ssg, new Message(d), new Message(d), null, null, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void testClientRequestXpathAssertion() throws Exception {
        Ssg ssg = new Ssg(1);

        // build simulated placeOrder request
        Document placeorderDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        PolicyApplicationContext placeorderReq = makereq(ssg, placeorderDoc);

        // build simulated getQuote request
        Document getquoteDoc = TestDocuments.getTestDocument(TestDocuments.ETTK_SIGNED_REQUEST);
        PolicyApplicationContext getquoteReq = makereq(ssg, getquoteDoc);

        // build xpath assertion that matches placeorder, with a namespace prefix
        Map placeorderNsmap = XpathBasedAssertion.createDefaultNamespaceMap();
        placeorderNsmap.put(PREFIX, "http://warehouse.acme.com/ws");
        XpathExpression placeorderXpath = new XpathExpression("//" + PREFIX + ":placeOrder", placeorderNsmap);
        RequestXpathAssertion placeorderRxa = new RequestXpathAssertion(placeorderXpath);
        ClientRequestXpathAssertion placeorderCrxa = new ClientRequestXpathAssertion(placeorderRxa);

        // build xpath assertion that matches getQuote, without any namespace prefix
        XpathExpression getquoteXpath = new XpathExpression("//symbol", XpathBasedAssertion.createDefaultNamespaceMap());
        RequestXpathAssertion getquoteRxa = new RequestXpathAssertion(getquoteXpath);
        ClientRequestXpathAssertion getquoteCrxa = new ClientRequestXpathAssertion(getquoteRxa);

        // test positive match with namespace
        AssertionStatus result = placeorderCrxa.decorateRequest(placeorderReq);
        assertEquals(result, AssertionStatus.NONE);

        // test positive match without namespace
        result = getquoteCrxa.decorateRequest(getquoteReq);
        assertEquals(result, AssertionStatus.NONE);

        // test negative match1
        result = placeorderCrxa.decorateRequest(getquoteReq);
        assertEquals(result, AssertionStatus.FALSIFIED);

        // test negative match2
        result = getquoteCrxa.decorateRequest(placeorderReq);
        assertEquals(result, AssertionStatus.FALSIFIED);

        // test invalid xpath expression
        XpathExpression badXpath = new XpathExpression("!", XpathBasedAssertion.createDefaultNamespaceMap());
        RequestXpathAssertion badRxa = new RequestXpathAssertion(badXpath);
        ClientRequestXpathAssertion badCrxa = new ClientRequestXpathAssertion(badRxa);
        try {
            result = badCrxa.decorateRequest(placeorderReq);
            fail("Failed to throw expected exception due to bad xpath");
        } catch (PolicyAssertionException e) {
            log.info("The correct exception was thrown.");
        }
    }
}
