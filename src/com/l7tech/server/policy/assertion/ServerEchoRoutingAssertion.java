package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EchoRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Assertion that echoes the request into the response
 * @author emil
 * @version 21-Mar-2005
 */
public class ServerEchoRoutingAssertion extends ServerRoutingAssertion {
    public ServerEchoRoutingAssertion(EchoRoutingAssertion ea, ApplicationContext applicationContext) {
        super(ea, applicationContext);
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();

        // See if we have a real response -- if not, we'll cheat and just copy request to response
        final HttpResponseKnob respHttp = (HttpResponseKnob)response.getKnob(HttpResponseKnob.class);
        MimeKnob respMime = (MimeKnob)response.getKnob(MimeKnob.class);

        if (respMime != null) {
            // We have a real response -- try to preserve it
            Message oldResponse = new Message();
            oldResponse.takeOwnershipOfKnobsFrom(response);

            // Copy request to response
            copyRequestToResponse(response, request, respHttp);

            // Copy old response to request
            final HttpRequestKnob reqHttp = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);

            request.takeOwnershipOfKnobsFrom(oldResponse);
            if (reqHttp != null && request.getKnob(HttpRequestKnob.class) == null) request.attachHttpRequestKnob(reqHttp);
        }

        // No real response.  Just point the response at the request knobs and have done.
        copyRequestToResponse(response, request, respHttp);
        return AssertionStatus.NONE;
    }

    private void copyRequestToResponse(Message response, Message request, HttpResponseKnob respHttp) {
        response.takeOwnershipOfKnobsFrom(request);
        if (respHttp != null && response.getKnob(HttpResponseKnob.class) == null) {
            response.attachHttpResponseKnob(respHttp);
            respHttp.setStatus(HttpServletResponse.SC_OK);
        }
    }
}
