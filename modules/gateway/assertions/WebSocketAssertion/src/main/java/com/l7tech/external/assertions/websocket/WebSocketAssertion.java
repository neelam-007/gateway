package com.l7tech.external.assertions.websocket;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: cirving
 * Date: 5/30/12
 * Time: 2:56 PM
 */
public class WebSocketAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(WebSocketAssertion.class.getName());

    @Override
    public String[] getVariablesUsed() {
        return new String[0];
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = WebSocketAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        props.put(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY, new String[] { "This property sets the maximum text message size in bytes. (Default: 1048576 bytes). Requires gateway restart.", "1048576" });
        props.put(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY, new String[] { "This property sets the maximum binary message size in bytes. (Default: 1048576 bytes). Requires gateway restart.", "1048576" });
        props.put(WebSocketConstants.BUFFER_SIZE_KEY, new String[] { "This property sets the outbound client buffer size in bytes. (Default: 4096 bytes). Requires gateway restart.", "4096" });
        props.put(WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS_KEY, new String[] { "This property sets the idle time for all inbound connections in milliseconds. (Default: 60000 milliseconds). Requires gateway restart.", "60000" });
        props.put(WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS_KEY, new String[] { "This property sets the idle time for all outbound client connections in milliseconds. (Default: 60000 milliseconds). Requires gateway restart.", "60000" });
        props.put(WebSocketConstants.MAX_INBOUND_CONNECTIONS_KEY, new String[] { "This property sets the maximum connections for a single connection definition. The connection definition " +
                "can set a lower value if desired. (Default: 4096 connections). Requires gateway restart.", "4096" });
        props.put(WebSocketConstants.CONNECT_TIMEOUT_KEY, new String[] { "This property sets the timeout in seconds an outbound connection will wait before erroring out. (Default: 20 seconds). Requires gateway restart.", "20" });
        props.put(WebSocketConstants.MIN_INBOUND_THREADS_KEY, new String[] { "This property sets the minimum threads available for inbound connections. (Default: 10 threads). Requires gateway restart.", "10" });
        props.put(WebSocketConstants.MAX_INBOUND_THREADS_KEY, new String[] { "This property sets the maximum threads available for inbound connections. (Default: 25 threads). Requires gateway restart.", "25" });
        props.put(WebSocketConstants.MIN_OUTBOUND_THREADS_KEY, new String[] { "This property sets the minimum threads available for outbound connections. (Default: 10 threads). Requires gateway restart.", "10" });
        props.put(WebSocketConstants.MAX_OUTBOUND_THREADS_KEY, new String[] { "This property sets the maximum threads available for outbound connections. (Default: 25 threads). Requires gateway restart.", "25" });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{""});
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.websocket.server.WebSocketLoadListener");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");


        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:WebSocket" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
