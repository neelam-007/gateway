/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.proxy.message.PolicyApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientSslAssertion extends ClientAssertion {
    public ClientSslAssertion( SslAssertion data ) {
        this.data = data;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param context
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context)  {
        if (data.getOption() == SslAssertion.FORBIDDEN)
            context.setSslForbidden(true);
        if (data.getOption() == SslAssertion.REQUIRED)
            context.setSslRequired(true);
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)  {
        // no action on response
        return AssertionStatus.NONE;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String ret = "Require SSL Transport";
        if (SslAssertion.FORBIDDEN.equals(data.getOption()))
            ret = "Forbid SSL transport";
        else if (SslAssertion.OPTIONAL.equals(data.getOption()))
            ret = "Optional SSL transport";
        return ret;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/ssl.gif";
    }

    protected SslAssertion data;
}
