/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.skunkworks.message.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author mike
 */
public class NuevoSoapServlet extends HttpServlet {
    public void doPost(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        try {
            // Initialize request
            String rawct = hrequest.getContentType();
            ContentTypeHeader ctype;
            if (rawct != null && rawct.length() > 0)
                ctype = ContentTypeHeader.parseValue(rawct);
            else
                ctype = ContentTypeHeader.XML_DEFAULT;

            HttpRequestKnob reqKnob = new HttpServletRequestKnob(hrequest);
            HttpResponseKnob respKnob = new HttpServletResponseKnob(hresponse);
            Message reqMsg = new Message(new ByteArrayStashManager(), ctype, hrequest.getInputStream());
            reqMsg.attachHttpRequestKnob(reqKnob);
            Message respMsg = new Message();
            respMsg.attachHttpResponseKnob(respKnob);

            AssertionStatus status = AssertionStatus.UNDEFINED;
            MessageContext context = new MessageContext(reqMsg, respMsg);
            RequestAdapter reqAdapter = new RequestAdapter(context);
            ResponseAdapter respAdapter = new ResponseAdapter(context);
            status = MessageProcessor.getInstance().processMessage(reqAdapter, respAdapter);
        } catch (PolicyAssertionException e) {
            throw new RuntimeException(e); // can't happen
        } catch (PolicyVersionException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

}
