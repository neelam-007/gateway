/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Map;
import java.io.IOException;

import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.policy.assertion.ClientRequestXpathAssertion;
import com.l7tech.proxy.policy.assertion.ClientResponseXpathAssertion;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.mime.MimeBody;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.*;
import org.w3c.dom.Document;

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
        RequestInterceptor ri = NullRequestInterceptor.INSTANCE;
        Document emptyDoc = XmlUtil.stringToDocument(
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"/>");
        PendingRequest emptyReq = new PendingRequest(ssg, null, null, emptyDoc, ri, null, null);

        // build simulated placeOrder response (we'll fake it with the request XML though)
        Document placeorderDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        SsgResponse placeorderRes = new SsgResponse(makemm(placeorderDoc), placeorderDoc, null, 200, null);

        // build simulated getQuote response (we'll faek it with the request XML though)
        Document getquoteDoc = TestDocuments.getTestDocument(TestDocuments.ETTK_SIGNED_REQUEST);
        SsgResponse getquoteRes = new SsgResponse(makemm(placeorderDoc), getquoteDoc, null, 200, null);

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
        AssertionStatus result = placeorderCrxa.unDecorateReply(emptyReq, placeorderRes);
        assertEquals(result, AssertionStatus.NONE);

        // test positive match without namespace
        result = getquoteCrxa.unDecorateReply(emptyReq, getquoteRes);
        assertEquals(result, AssertionStatus.NONE);

        // test negative match1
        result = placeorderCrxa.unDecorateReply(emptyReq, getquoteRes);
        assertEquals(result, AssertionStatus.FALSIFIED);

        // test negative match2
        result = getquoteCrxa.unDecorateReply(emptyReq, placeorderRes);
        assertEquals(result, AssertionStatus.FALSIFIED);

        // test invalid xpath expression
        XpathExpression badXpath = new XpathExpression("!", XpathBasedAssertion.createDefaultNamespaceMap());
        ResponseXpathAssertion badRxa = new ResponseXpathAssertion(badXpath);
        ClientResponseXpathAssertion badCrxa = new ClientResponseXpathAssertion(badRxa);
        try {
            result = badCrxa.unDecorateReply(emptyReq, placeorderRes);
            fail("Failed to throw expected exception due to bad xpath");
        } catch (PolicyAssertionException e) {
            log.info("The correct exception was thrown.");
        }
    }

    private MimeBody makemm(Document placeorderDoc) {
        try {
            return new MimeBody(XmlUtil.nodeToString(placeorderDoc).getBytes("UTF-8"),
                                        ContentTypeHeader.XML_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e);
        }
    }

    public void testClientRequestXpathAssertion() throws Exception {
        Ssg ssg = new Ssg(1);
        RequestInterceptor ri = NullRequestInterceptor.INSTANCE;

        // build simulated placeOrder request
        Document placeorderDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        PendingRequest placeorderReq = new PendingRequest(ssg, null, null, placeorderDoc, ri, null, null);

        // build simulated getQuote request
        Document getquoteDoc = TestDocuments.getTestDocument(TestDocuments.ETTK_SIGNED_REQUEST);
        PendingRequest getquoteReq = new PendingRequest(ssg, null, null, getquoteDoc, ri, null, null);

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
