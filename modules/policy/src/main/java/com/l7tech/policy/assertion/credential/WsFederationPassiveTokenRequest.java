package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

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

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WsFederationPassiveTokenRequest>(){
        @Override
        public String getAssertionName( final WsFederationPassiveTokenRequest assertion, final boolean decorate) {
            final String assertionName = "Obtain Credentials using WS-Federation Request";
            if(!decorate) return assertionName;

            return assertionName + " to " + assertion.getIpStsUrl();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) super.meta();
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PALETTE_NODE_CLASSNAME, "com.l7tech.console.tree.WsFederationPassiveTokenRequestPaletteNode");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        return meta;
    }

    //- PRIVATE

    private String realm;
    private boolean timestamp;
}
