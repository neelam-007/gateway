/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.TransportMetadata;
import com.l7tech.message.TransportProtocol;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.policy.assertion.ConfidentialityAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.util.Collections;
import java.util.Set;

/**
 * The policy assertion describes the XML encryption requirements.
 *
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class XmlEncAssertion extends ConfidentialityAssertion
  implements DirectionConstants {
    private int direction = IN;
    /**
     * default constructor, required by XML serializers
     */
    public XmlEncAssertion() {
    }

    public AssertionStatus checkRequest(Request request, Response response)
      throws PolicyAssertionException {
        return AssertionStatus.NOT_YET_IMPLEMENTED;

    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request)
      throws PolicyAssertionException {
        request.setSslRequired(true);
        return AssertionStatus.NONE;
    }

    /**
     * Return the direction to which the assertion applies to,
     * IN specifies the incoming messaage requires encryption, OUT
     * specifies that the outgoing message requires encryption and
     * INOUT specifies that the encryption is required both ways.
     *
     * @return the direction where the assertions applies
     * @see DirectionConstants
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Set the encryption direction value
     *
     * @param direction the new direction
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }
}
