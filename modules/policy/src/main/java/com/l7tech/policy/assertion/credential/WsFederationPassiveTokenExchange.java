package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 *  Assertion for WS-Federation using Passive Request Profile.
 *
 * @author $Author$
 * @version $Revision$
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class WsFederationPassiveTokenExchange extends WsFederationPassiveTokenAssertion {

    //- PUBLIC

    public WsFederationPassiveTokenExchange() {
    }

    public WsFederationPassiveTokenExchange(String url, String contextUrl, String replyUrl) {
        super(url,contextUrl, replyUrl);
    }

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WsFederationPassiveTokenExchange>(){
        @Override
        public String getAssertionName( final WsFederationPassiveTokenExchange assertion, final boolean decorate) {
            final String name;
            if((assertion.getIpStsUrl()!=null && assertion.getIpStsUrl().length()>0) || !assertion.isAuthenticate()) {
                name = "Exchange credentials using WS-Federation Request to " + assertion.getIpStsUrl();
            } else {
                name = "Authenticate with WS-Federation Protected Service at " + assertion.getReplyUrl();
            }
            return name;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) super.meta();
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        return meta;
    }
}
