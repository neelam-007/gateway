package com.l7tech.policy.assertion.transport;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * This assertion is meant to instruct XML VPN Clients to compress payloads prior to
 * forwarding the request to the SSG
 * <p/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 */
public class PreemptiveCompression extends Assertion {
    private boolean serverSideCheck = true;

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"xml"});
        meta.put(SHORT_NAME, "Compress Messages to/from SecureSpan XVC");
        meta.put(DESCRIPTION, "Messages to and from the SecureSpan XML VPN Client will be compressed using the gzip algorithm.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.PreemptiveCompressionAction");
        meta.put(PROPERTIES_ACTION_NAME, "Compression Properties");
        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");

        meta.put(CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/folder.gif");
        
        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.server.policy.assertion.transport.ServerPreemptiveCompression");
        meta.put(CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.transport.ClientPreemptiveCompression");
        meta.put(USED_BY_CLIENT, Boolean.TRUE);        
        return meta;
    }

    public boolean isServerSideCheck() {
        return serverSideCheck;
    }

    public void setServerSideCheck(boolean serverSideCheck) {
        this.serverSideCheck = serverSideCheck;
    }
}
