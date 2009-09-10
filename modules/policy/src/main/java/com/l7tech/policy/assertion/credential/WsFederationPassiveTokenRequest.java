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
public class WsFederationPassiveTokenRequest extends WsFederationPassiveTokenAssertion {

    //- PUBLIC
    public WsFederationPassiveTokenRequest() {
    }

    public WsFederationPassiveTokenRequest(String url, String realm, boolean authenticate, String replyUrl, String contextUrl, boolean timestamp) {
        super(url, contextUrl, replyUrl, authenticate);
        this.realm = realm;
        this.timestamp = timestamp;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isTimestamp() {
        return timestamp;
    }

    public void setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void copyFrom(WsFederationPassiveTokenAssertion source) {
        super.copyFrom(source);
        if(source instanceof WsFederationPassiveTokenRequest){
            WsFederationPassiveTokenRequest request = (WsFederationPassiveTokenRequest)source;
            this.setRealm(request.getRealm());
            this.setTimestamp(request.isTimestamp());
        }
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) super.meta();

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Binary<String, Assertion, Boolean>(){
            public String call(Assertion assertion, Boolean decorate) {
                //todo [Donal] delete once fully tested
                if (!(assertion instanceof WsFederationPassiveTokenRequest)) throw new IllegalStateException("Assertion incorrectly configured");

                final String assertionName = "Obtain Credentials using WS-Federation Request";
                if(!decorate) return assertionName;

                return assertionName + " to " + ((WsFederationPassiveTokenRequest)assertion).getIpStsUrl();
            }
        });

        return meta;
    }

    //- PRIVATE

    private String realm;
    private boolean timestamp;
}
