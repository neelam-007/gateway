/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

import org.apache.axis.message.SOAPEnvelope;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.policy.assertion.FalseAssertion;

/**
 *
 * User: mike
 * Date: Jul 24, 2003
 * Time: 3:50:01 PM
 */
public class MessageViewerTest extends TestCase {
    private static Logger log = Logger.getLogger(MessageViewerTest.class.getName());

    public MessageViewerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MessageViewerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testMessageViewer() throws Exception {
        MessageViewer mv = new MessageViewer("Message viewer test");
        mv.show();
        RequestInterceptor ri = mv.getMessageViewerModel();
        ri.onReceiveMessage(new SOAPEnvelope());
        ri.onReceiveReply("<reply>blah blah blah, if this were an actual response, this would be a real SOAPEnvelope document.</reply>");
        ri.onPolicyUpdated(new Ssg(22, "My SSG", "http://whatever"),
                           new PolicyAttachmentKey("http://example.com/schemas/wompfoo",
                                                   "http://example.com/schemas/wompfoo#WompSomeFoos"),
                           FalseAssertion.getInstance());
    }
}
