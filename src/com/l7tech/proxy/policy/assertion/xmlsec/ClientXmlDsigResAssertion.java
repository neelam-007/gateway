package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy)
 *
 * On the server side, this decorates a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope.
 *
 * @author flascell
 */
public class ClientXmlDsigResAssertion implements ClientAssertion {

    /**
     * i dont want to decorate a request but rather validate something in the response
     *
     * @param request left untouched
     * @return AssertionStatus.NONE (always)
     * @throws PolicyAssertionException no
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        return AssertionStatus.NONE;
    }
}
