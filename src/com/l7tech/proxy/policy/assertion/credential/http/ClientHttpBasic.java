/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.http;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.util.ClientLogger;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientHttpBasic extends ClientAssertion {
    private static final ClientLogger log = ClientLogger.getInstance(ClientHttpBasic.class);

    public ClientHttpBasic( HttpBasic data ) {
        _data = data;
    }

    /**
     * Set up HTTP Basic auth on the PendingRequest.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PendingRequest request)
            throws OperationCanceledException
    {
        request.getCredentials();
        request.setBasicAuthRequired(true);
        if (!request.getClientSidePolicy().isPlaintextAuthAllowed())
            request.setSslRequired(true); // force SSL when using HTTP Basic
        log.info("HttpBasic: will use HTTP basic on this request to " + request.getSsg());
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    /**
     * @return the human-readable node name that is displayed.
     */
    public String getName() {
        return "Require HTTP Basic Authentication";
    }

    /**
     * subclasses override this method specifying the resource name of the
     * icon to use when this assertion is displayed in the tree view.
     *
     * @param open for nodes that can be opened, can have children
     * @return a string such as "com/l7tech/proxy/resources/tree/assertion.png"
     */
    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/authentication.gif";
    }

    protected HttpBasic _data;
}
