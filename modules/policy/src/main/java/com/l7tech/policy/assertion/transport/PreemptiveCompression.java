package com.l7tech.policy.assertion.transport;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

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

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});
        meta.put(AssertionMetadata.SHORT_NAME, "Compress Messages to/from SecureSpan XVC");
        meta.put(AssertionMetadata.DESCRIPTION, "Messages to and from the SecureSpan XML VPN Client will be compressed using the gzip algorithm.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.PreemptiveCompressionAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Compression Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");

        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/folder.gif");
        
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
