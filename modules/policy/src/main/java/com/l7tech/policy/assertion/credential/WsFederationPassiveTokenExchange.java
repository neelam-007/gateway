package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.util.Functions;

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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) super.meta();

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Binary<String, Assertion, Boolean>(){
            public String call(Assertion assertion, Boolean decorate) {
                //todo [Donal] delete once fully tested
                if (!(assertion instanceof WsFederationPassiveTokenExchange)) throw new IllegalStateException("Assertion incorrectly configured");

                WsFederationPassiveTokenExchange exchangeAssertion = (WsFederationPassiveTokenExchange) assertion;
                final String name;
                if((exchangeAssertion.getIpStsUrl()!=null && exchangeAssertion.getIpStsUrl().length()>0) || !exchangeAssertion.isAuthenticate()) {
                    name = "Exchange credentials using WS-Federation Request to " + exchangeAssertion.getIpStsUrl();
                } else {
                    name = "Authenticate with WS-Federation Protected Service at " + exchangeAssertion.getReplyUrl();
                }
                return name;
            }
        });
        
        return meta;
    }
}
