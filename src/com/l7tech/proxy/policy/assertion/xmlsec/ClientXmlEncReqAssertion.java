package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlEncReqAssertion;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 11:20:43 AM
 * $Id$
 *
 * Encrypts the body of the request
 */
public class ClientXmlEncReqAssertion implements ClientAssertion {

    public ClientXmlEncReqAssertion(XmlEncReqAssertion data) {
        // no data
    }
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        // todo
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // nothing to do
        return AssertionStatus.NONE;
    }
}
