/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.client.gui;

import com.l7tech.message.Message;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;
import org.xml.sax.SAXException;
import org.junit.Ignore;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 *
 * User: mike
 * Date: Jul 24, 2003
 * Time: 3:50:01 PM
 */
@Ignore
public class MessageViewerTest {
    private static PolicyApplicationContext request(String what, RequestInterceptor ri) throws IOException, SAXException {
        return new PolicyApplicationContext(null, new Message( XmlUtil.stringToDocument(what),0), null, ri, null, null);
    }

    private static PolicyApplicationContext reply(String what, RequestInterceptor ri) throws IOException, SAXException {
        return new PolicyApplicationContext(null, null, new Message(XmlUtil.stringToDocument(what),0), ri, null, null);
    }

    public static void main(String[] args) throws Exception {
        //Gui.setInstance(Gui.createGui(new Gui.GuiParams(new SsgManagerStub(), 0)));
        Gui.getInstance().start();

        MessageViewer mv = new MessageViewer("Message viewer test");
        mv.setVisible(true);
        RequestInterceptor ri = mv.getModel();
        ri.onFrontEndRequest(request("<foo><bar/><baz/></foo>", ri));
        ri.onReplyError(new Exception("BlahException: Blah blah blah!"));
        ri.onFrontEndRequest(request("<foo><bar/><baz/></foo>", ri));
        ri.onFrontEndReply(reply("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>", null));
        ri.onMessageError(new Exception("DumbException: you r teh dumb"));
        ri.onFrontEndRequest(request("<foo><bar/><baz/></foo>", ri));
        ri.onFrontEndReply(reply("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>", null));
        ri.onFrontEndRequest(request("<foo><bar/><baz/></foo>", ri));
        ri.onFrontEndReply(reply("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>", null));
        ri.onPolicyUpdated(new Ssg(22, "whatever"),
                           new PolicyAttachmentKey("http://example.com/schemas/wompfoo",
                                                   "http://example.com/schemas/wompfoo#WompSomeFoos",
                                                   "/gateway1/wompfoo"),
                           new Policy(FalseAssertion.getInstance(), "policyVersion 2.0"));
        mv.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) { killit(); }
            @Override
            public void windowClosed(WindowEvent e) { killit(); }
        });
    }

    private static void killit() {
        Gui.getInstance().stop();
        System.exit(0);
    }
}
