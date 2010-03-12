package com.l7tech.external.assertions.actional;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class ActionalAssertion extends Assertion implements SetsVariables {

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ActionalAssertion.class.getName() + ".metadataInitialized";

    public static final String INTERCEPTOR_ENABLE_CLUSTER_PROPERTY = "interceptor.enabled";
    public static final String INTERCEPTOR_TRANSMIT_PROVIDER_PAYLOAD_CLUSTER_PROPERTY = "interceptor.transmitProviderPayloads";
    public static final String INTERCEPTOR_TRANSMIT_CONSUMER_PAYLOAD_CLUSTER_PROPERTY = "interceptor.transmitConsumerPayloads";
    public static final String INTERCEPTOR_CONFIG_DIRECTORY = "interceptor.configDir";
    public static final String INTERCEPTOR_ENFORCE_INBOUND_TRUST_ZONE = "interceptor.enforceInboundTrustZone";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(INTERCEPTOR_ENABLE_CLUSTER_PROPERTY, false, false, null, false, DataType.BOOLEAN),
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, getClusterPropertiesMetadata());

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Actional Interceptor");
        meta.put(AssertionMetadata.LONG_NAME, "Actional Interceptor Assertion");

        // The Actional Agent assertion doesn't appear in any palette folders.
        meta.putNull(AssertionMetadata.PALETTE_FOLDERS);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Actional" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        // Subscribe our ActionalInterceptorListener to the module loading events so it can set up its application listener
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.actional.server.InterceptorEventListener");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private Map<String, String[]> getClusterPropertiesMetadata() {
        Map<String, String[]> clusterProperties = new HashMap<String, String[]>();
        clusterProperties.put(INTERCEPTOR_ENABLE_CLUSTER_PROPERTY, new String[] {
                "Enables and disables the Actional Interceptor feature. (true/false)\n" +
                "This value is checked every 2 minutes.",
                String.valueOf(Boolean.FALSE)
        });

        clusterProperties.put(INTERCEPTOR_TRANSMIT_PROVIDER_PAYLOAD_CLUSTER_PROPERTY, new String[] {
                "Determines whether XML payloads are captured and forwarded by the Interceptor along with statistical information when processing incoming request messages. (true/false)\n" +
                "Warning. Transmitting the payload can be resource intensive.",
                String.valueOf(Boolean.FALSE)
        });

        clusterProperties.put(INTERCEPTOR_TRANSMIT_CONSUMER_PAYLOAD_CLUSTER_PROPERTY, new String[] {
                "Determines whether XML payloads are captured and forwarded by the Interceptor along with statistical information when processing outgoing request messages. (true/false)\n" +
                "Warning. Transmitting the payload can be resource intensive.",
                String.valueOf(Boolean.FALSE)
        });

        clusterProperties.put(INTERCEPTOR_CONFIG_DIRECTORY, new String[] {
                "Common configuration directory used by both the Interceptor and the Actional Agent.  This setting must be configured to the same location as the Agent and should never be changed for an appliance install.\n" +
                        "Requires restart.",
                "/opt/SecureSpan/Actional/LG.Interceptor"
        });

        clusterProperties.put(INTERCEPTOR_ENFORCE_INBOUND_TRUST_ZONE, new String[]{
                "Determines whether or not the Gateway interceptor enforces Trust Zones on inbound messages.",
                String.valueOf(Boolean.FALSE)
        });

        return clusterProperties;
    }
}
