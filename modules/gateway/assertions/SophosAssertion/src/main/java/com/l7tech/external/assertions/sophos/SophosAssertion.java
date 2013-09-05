package com.l7tech.external.assertions.sophos;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.IP_ADDRESS_ARRAY;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.OPTIONAL;

/**
 *
 */
public class SophosAssertion extends MessageTargetableAssertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(SophosAssertion.class.getName());

    private String failoverStrategyName;
    private String[] addresses = new String[0];

    private boolean failOnError = true;
    private String prefixVar = "sophos.scan.virus";

    public static final String CPROP_SOPHOS_FAILOVER_RETRIES = "sophos.failover.retries";
    public static final String PARAM_SOPHOS_FAILOVER_RETRIES = ClusterProperty.asServerConfigPropertyName(CPROP_SOPHOS_FAILOVER_RETRIES);
    public static final String CPROP_SOPHOS_VIRUS_FOUND_LOG_LEVEL = "sophos.virus.found.log.level";
    public static final String CPROP_SOPHOS_SOCKET_CONNECT_TIMEOUT = "sophos.socket.connect.timeout";
    public static final String CPROP_SOPHOS_SOCKET_READ_TIMEOUT = "sophos.socket.read.timeout";
    public static final String DEFAULT_LOG_LEVEL = "WARNING";
    public static final String DEFAULT_PORT = "0";
    public static final String DEFAULT_TIMEOUT = "5000";

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SophosAssertion.class.getName() + ".metadataInitialized";
    
    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(getPrefixVariable()+".name", false, true, null, false, DataType.STRING),
                new VariableMetadata(getPrefixVariable()+".location", false, true, null, false, DataType.STRING),
                new VariableMetadata(getPrefixVariable()+".type", false, true, null, false, DataType.STRING),
                new VariableMetadata(getPrefixVariable()+".disinfectable", false, true, null, false, DataType.STRING),
                new VariableMetadata(getPrefixVariable()+".count", false, true, null, false, DataType.STRING)
        );
    }
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(CPROP_SOPHOS_FAILOVER_RETRIES, new String[] {
                "Maximum number of retries for Sophos failover",
                "5"
        });
        props.put(CPROP_SOPHOS_VIRUS_FOUND_LOG_LEVEL, new String[] {
                "Logging level to use when a virus is detected. Default is WARNING",
                "WARNING"
        });
        props.put(CPROP_SOPHOS_SOCKET_CONNECT_TIMEOUT, new String[] {
                "Sophos socket connection timeout. Default is 5 seconds",
                "5000"
        });

        props.put(CPROP_SOPHOS_SOCKET_READ_TIMEOUT, new String[] {
                "Sophos socket read timeout(post connection). Default is 5 seconds",
                "5000"
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Scan using Sophos Antivirus");
        meta.put(AssertionMetadata.LONG_NAME, "This assertion scans all message attachments for viruses using Sophos Antivirus running on another machine.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.sophos.console.SophosAssertionPropertiesPanel");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Sophos Antivirus Properties");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Sophos" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public String getPropertiesDialogTitle() {
        return String.valueOf(meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME));
    }

    public String getFailoverStrategyName() {
        return failoverStrategyName;
    }

    public void setFailoverStrategyName(String failoverStrategyName) {
        this.failoverStrategyName = failoverStrategyName;
    }

    /** @return the custom IP addresses to use as an array of String, or null if no custom IP address list is configured. */
    @Migration(mapName = NONE, mapValue = OPTIONAL, valueType = IP_ADDRESS_ARRAY, resolver = PropertyResolver.Type.VALUE_REFERENCE)
    public String[] getAddresses() {
        return addresses;
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

     public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }
    public String getPrefixVariable() {
        return prefixVar;
    }

    public void setPrefixVariable(String prefixVar) {
        this.prefixVar = prefixVar;
    }


}
