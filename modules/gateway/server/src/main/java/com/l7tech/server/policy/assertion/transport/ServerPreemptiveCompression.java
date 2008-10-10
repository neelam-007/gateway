package com.l7tech.server.policy.assertion.transport;

import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * This assertion is meant to instruct XML VPN Clients to compress payloads prior to
 * forwarding the request to the SSG
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 3, 2008<br/>
 */
public class ServerPreemptiveCompression extends AbstractServerAssertion implements ServerAssertion {
    private Logger logger = Logger.getLogger(ServerPreemptiveCompression.class.getName());
    private PreemptiveCompression assertion;

    public ServerPreemptiveCompression(PreemptiveCompression assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (assertion.isServerSideCheck()) {
            if (context.isRequestWasCompressed()) {
                return AssertionStatus.NONE;
            } else {
                logger.info("Request was supposed to be compressed but was not");
                context.setRequestPolicyViolated();
                return AssertionStatus.FALSIFIED;
            }
        } else {
            return AssertionStatus.NONE;
        }
    }
}
