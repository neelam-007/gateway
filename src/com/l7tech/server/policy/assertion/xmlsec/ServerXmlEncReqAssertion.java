package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlEncReqAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 11:16:14 AM
 * $Id$
 *
 * Validates that the request's body is encrypted and decypher it
 *
 * todo, this should be removed because functionality has been moved to ServerXmlResponseSecurity
 */
public class ServerXmlEncReqAssertion implements ServerAssertion {
    public ServerXmlEncReqAssertion(XmlEncReqAssertion data) {
        // no data
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        // todo
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }
}
