/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 *
 * User: mike
 * Date: Jul 24, 2003
 * Time: 3:50:01 PM
 */
public class MessageViewerTest {
    private static Logger log = Logger.getLogger(MessageViewerTest.class.getName());

    public static void main(String[] args) throws Exception {
        Gui.setInstance(Gui.createGui(null, new SsgManagerStub()));
        Gui.getInstance().start();

        MessageViewer mv = new MessageViewer("Message viewer test");
        mv.show();
        RequestInterceptor ri = mv.getMessageViewerModel();
        ri.onReceiveMessage(new PendingRequest(XmlUtil.stringToDocument("<foo><bar/><baz/></foo>"), null, ri, null));
        ri.onReplyError(new Exception("BlahException: Blah blah blah!"));
        ri.onReceiveMessage(new PendingRequest(XmlUtil.stringToDocument("<foo><bar/><baz/></foo>"), null, ri, null));
        ri.onReceiveReply(new SsgResponse(XmlUtil.stringToDocument("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>"), null, 200, null, null));
        ri.onMessageError(new Exception("DumbException: you r teh dumb"));
        ri.onReceiveMessage(new PendingRequest(XmlUtil.stringToDocument("<foo><bar/><baz/></foo>"), null, ri, null));
        ri.onReceiveReply(new SsgResponse(XmlUtil.stringToDocument("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>"), null, 200, null, null));
        ri.onReceiveMessage(new PendingRequest(XmlUtil.stringToDocument("<foo><bar/><baz/></foo>"), null, ri, null));
        ri.onReceiveReply(new SsgResponse(XmlUtil.stringToDocument("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>"), null, 200, null, null));
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
