/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManagerStub;
import com.l7tech.proxy.message.PolicyApplicationContext;
import org.xml.sax.SAXException;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 *
 * User: mike
 * Date: Jul 24, 2003
 * Time: 3:50:01 PM
 */
public class MessageViewerTest {
    private static PolicyApplicationContext request(String what, RequestInterceptor ri) throws IOException, SAXException {
        return new PolicyApplicationContext(null, new Message(XmlUtil.stringToDocument(what)), null, ri, null, null);
    }

    private static PolicyApplicationContext reply(String what, RequestInterceptor ri) throws IOException, SAXException {
        return new PolicyApplicationContext(null, null, new Message(XmlUtil.stringToDocument(what)), ri, null, null);
    }

    public static void main(String[] args) throws Exception {
        Gui.setInstance(Gui.createGui(null, new SsgManagerStub()));
        Gui.getInstance().start();

        MessageViewer mv = new MessageViewer("Message viewer test");
        mv.show();
        RequestInterceptor ri = mv.getMessageViewerModel();
        ri.onReceiveMessage(request("<foo><bar/><baz/></foo>", ri));
        ri.onReplyError(new Exception("BlahException: Blah blah blah!"));
        ri.onReceiveMessage(request("<foo><bar/><baz/></foo>", ri));
        ri.onReceiveReply(reply("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>", null));
        ri.onMessageError(new Exception("DumbException: you r teh dumb"));
        ri.onReceiveMessage(request("<foo><bar/><baz/></foo>", ri));
        ri.onReceiveReply(reply("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>", null));
        ri.onReceiveMessage(request("<foo><bar/><baz/></foo>", ri));
        ri.onReceiveReply(reply("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>", null));
        ri.onPolicyUpdated(new Ssg(22, "whatever"),
                           new PolicyAttachmentKey("http://example.com/schemas/wompfoo",
                                                   "http://example.com/schemas/wompfoo#WompSomeFoos",
                                                   "/gateway1/wompfoo"),
                           new Policy(FalseAssertion.getInstance(), "policyVersion 2.0"));
        mv.addWindowListener(new WindowAdapter() {
            public void windowDeactivated(WindowEvent e) { killit(); }
            public void windowClosed(WindowEvent e) { killit(); }
        });
    }

    private static void killit() {
        Gui.getInstance().stop();
        System.exit(0);
    }
}
