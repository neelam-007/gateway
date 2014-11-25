package com.l7tech.external.assertions.bufferdata;

import com.l7tech.external.assertions.bufferdata.console.BufferDataPropertiesDialog;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public class BufferDataAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(BufferDataAssertion.class.getName());

    public static final int MAX_BUFFER_NAME_LENGTH = 256;

    private String bufferName;
    private String newDataVarName;
    private long maxSizeBytes = 1024L * 1024;
    private long maxAgeMillis = 86400L * 1000L;
    private boolean extractIfFull = true;
    private String variablePrefix = "buffer";

    public String getBufferName() {
        return bufferName;
    }

    public void setBufferName( String bufferName ) {
        this.bufferName = bufferName;
    }

    public String getNewDataVarName() {
        return newDataVarName;
    }

    public void setNewDataVarName( String newDataVarName ) {
        this.newDataVarName = newDataVarName;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes( long maxSizeBytes ) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public long getMaxAgeMillis() {
        return maxAgeMillis;
    }

    public void setMaxAgeMillis( long maxAgeMillis ) {
        this.maxAgeMillis = maxAgeMillis;
    }

    public boolean isExtractIfFull() {
        return extractIfFull;
    }

    public void setExtractIfFull( boolean extractIfFull ) {
        this.extractIfFull = extractIfFull;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix( String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }

    public String[] getVariablesUsed() {
        VariableUseSupport.VariablesUsed v = VariableUseSupport.expressions( bufferName );
        v.addVariables( newDataVarName );
        return v.asArray();
    }

    public String prefix( String var ) {
        return variablePrefix == null ? var : variablePrefix + "." + var;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata( prefix( "wasFull" ), true, false, prefix( "wasFull" ), false, DataType.BOOLEAN ),
                new VariableMetadata( prefix( "wasExtracted" ), true, false, prefix( "wasExtracted" ), false, DataType.BOOLEAN ),
                new VariableMetadata( prefix( "newSize.bytes" ), true, false, prefix( "newSize.bytes" ), false, DataType.DECIMAL ),
                new VariableMetadata( prefix( "newAge.millis" ), true, false, prefix( "newAge.millis" ), false, DataType.DECIMAL ),
                new VariableMetadata( prefix( "extractedMessage" ), true, false, prefix( "extractedMessage" ), false, DataType.MESSAGE )
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = BufferDataAssertion.class.getName() + ".metadataInitialized";

    public static final String PARAM_MAX_BUFFER_SIZE = "bufferdataMaxBufferSize";
    public static final String CLUSTER_PROP_MAX_BUFFER_SIZE = "bufferdata.maxBufferSize";
    public static final Long DEFAULT_MAX_BUFFER_SIZE = 500000000L;

    public static final String PARAM_MAX_IDLE_BUFFER_AGE = "bufferdataMaxIdleBufferAge";
    public static final String CLUSTER_PROP_MAX_IDLE_BUFFER_AGE = "bufferdata.maxIdleBufferAge";
    public static final Long DEFAULT_MAX_IDLE_BUFFER_AGE = 86400L;

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        props.put( CLUSTER_PROP_MAX_BUFFER_SIZE, new String[] {
                "The largest maximum buffer size that may be specified for the Buffer Data Assertion. (bytes, default=500000000)",
                Long.toString( DEFAULT_MAX_BUFFER_SIZE )
        });
        props.put( CLUSTER_PROP_MAX_IDLE_BUFFER_AGE, new String[] {
                "The maximum amount of time an idle buffer created by the Buffer Data Assertion is kept in memory before " +
                        "being discarded for going unused. (seconds, default=86400)",
                Long.toString( DEFAULT_MAX_IDLE_BUFFER_AGE )
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Buffer Data in Memory");
        meta.put(AssertionMetadata.LONG_NAME, "Buffer Data in Memory");
        meta.put(AssertionMetadata.DESCRIPTION, "Append data to a buffer in memory. " +
                "If the buffer is full, or was last extracted too long ago, atomically extract the buffer contents to a message variable.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "audit" });
        //meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put( AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.bufferdata.console.BufferDataPropertiesDialog" );

        // Set up smart Getter for nice, informative policy node name, for GUI
        //meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:BufferData" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
