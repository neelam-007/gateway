package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlEncResAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 11:16:27 AM
 * $Id$
 *
 * Encrypts the response
 */
public class ServerXmlEncResAssertion implements ServerAssertion {
    public ServerXmlEncResAssertion(XmlEncResAssertion data) {
        // no data
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        // todo
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }
}
