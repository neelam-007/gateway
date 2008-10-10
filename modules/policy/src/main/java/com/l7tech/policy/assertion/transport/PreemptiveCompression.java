package com.l7tech.policy.assertion.transport;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

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
public class PreemptiveCompression extends Assertion {
    private boolean serverSideCheck = true;

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.server.policy.assertion.transport.ServerPreemptiveCompression");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.transport.ClientPreemptiveCompression");
        return meta;
    }

    public boolean isServerSideCheck() {
        return serverSideCheck;
    }

    public void setServerSideCheck(boolean serverSideCheck) {
        this.serverSideCheck = serverSideCheck;
    }
}
