package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ServerRequestSwAAssertion implements ServerAssertion {
    private final RequestSwAAssertion data;
    private final Logger logger = Logger.getLogger(getClass().getName());

     public ServerRequestSwAAssertion( RequestSwAAssertion data ) {
        if (data == null) throw new IllegalArgumentException("must provide assertion");
        this.data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if (request instanceof SoapRequest && ((SoapRequest)request).isSoap()) {
            SoapRequest req = (SoapRequest)request;
            try {
                Document doc = req.getDocument();
            } catch (SAXException e) {
                logger.log(Level.SEVERE, "Error getting xml document from request", e);
                return AssertionStatus.SERVER_ERROR;
            }

            //if (data.hasMimeParts() ) {
            // return AssertionStatus.NONE;
        } else {

        }

        return AssertionStatus.NONE;
    }
}
