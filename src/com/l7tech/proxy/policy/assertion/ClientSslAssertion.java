/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientSslAssertion extends ClientAssertion {
    private static final Category log = Category.getInstance(ClientSslAssertion.class);

    public ClientSslAssertion( SslAssertion data ) {
        this.data = data;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PendingRequest request)  {
        request.setSslRequired( data.getOption() == SslAssertion.REQUIRED );
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)  {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected SslAssertion data;
}
