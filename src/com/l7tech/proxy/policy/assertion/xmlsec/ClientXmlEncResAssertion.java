package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlEncResAssertion;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 11:20:57 AM
 * $Id$
 *
 * Verifies that the response's body is encrypted and decypher it
 *
 * todo, remove because this functionality has moved to ClientXmlResponseSecurity
 */
public class ClientXmlEncResAssertion extends ClientAssertion {
    public ClientXmlEncResAssertion(XmlEncResAssertion data) {
        // no data
    }
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        // nothing to do on request
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // todo
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }
}
