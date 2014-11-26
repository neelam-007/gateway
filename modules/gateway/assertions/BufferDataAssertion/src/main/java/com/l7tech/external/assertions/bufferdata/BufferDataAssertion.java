package com.l7tech.external.assertions.bufferdata;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * 
 */
public class BufferDataAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(BufferDataAssertion.class.getName());

    public static final int MAX_BUFFER_NAME_LENGTH = 256;

    public static enum StorageUnit {
        BYTES( 1, "bytes" ),
        KILOBYTES( 1024, "kilobytes" ),
        MEGABYTES( 1024 * 1024, "megabytes" );

        private final int bytesPerUnit;
        private final String displayName;

        StorageUnit( int bytesPerUnit, String displayName ) {
            this.bytesPerUnit = bytesPerUnit;
            this.displayName = displayName;
        }

        public int getBytesPerUnit() {
            return bytesPerUnit;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    static final StorageUnit[] STORAGE_UNITS = new StorageUnit[] { StorageUnit.BYTES, StorageUnit.KILOBYTES, StorageUnit.MEGABYTES };
    static final TimeUnit[] TIME_UNITS = new TimeUnit[] { TimeUnit.MILLIS, TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS };

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

    /**
     * @return defensively-copied array of storage units used when building human readable policy node name.
     */
    public static StorageUnit[] getStorageUnits() {
        return Arrays.copyOf( STORAGE_UNITS, STORAGE_UNITS.length );
    }

    /**
     * @return defensively-copied array of time units used when building human readable policy node name.
     */
    public static TimeUnit[] getTimeUnits() {
        return Arrays.copyOf( TIME_UNITS, TIME_UNITS.length );
    }

    /**
     * Select the ideal time unit to use to represent the specified millisecond interval in a human-readable way.
     * <p/>
     * This method chooses the biggest time unit that can display the value without requiring a decimal point.
     *
     * @param timeInMillis the interval in milliseconds
     * @return largest supported time unit that can display this interval without a decimal point.  May be TimeUnit.MILLIS.  Never null.
     */
    @NotNull
    public static TimeUnit findBestTimeUnit( long timeInMillis ) {
        TimeUnit ret = TimeUnit.MILLIS;

        java.util.List<TimeUnit> units = new ArrayList<>( Arrays.asList( TIME_UNITS ) );
        Collections.reverse( units );
        for ( TimeUnit unit : units ) {
            if ( timeInMillis % unit.getMultiplier() == 0 ) {
                ret = unit;
                break;
            }
        }

        return ret;
    }

    /**
     * Select the ideal storage unit to use to represent the specified byte count in a human-readable way.
     * <p/>
     * This method chooses the biggest storage unit that can display the value without requiring a decimal point.
     *
     * @param storageInBytes number of bytes
     * @return largest supported storage unit that can display this number of bytes without a decimal point.  May be StorageUnit.BYTES.  Never null.
     */
    @NotNull
    public static StorageUnit findBestStorageUnit( long storageInBytes ) {
        StorageUnit ret = StorageUnit.BYTES;

        java.util.List<StorageUnit> units = new ArrayList<>( Arrays.asList( STORAGE_UNITS ) );
        Collections.reverse( units );
        for ( StorageUnit unit : units ) {
            if ( storageInBytes % unit.getBytesPerUnit() == 0 ) {
                ret = unit;
                break;
            }
        }

        return ret;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = BufferDataAssertion.class.getName() + ".metadataInitialized";

    private static final String BASE_NAME = "Accumulate Data in Memory";

    public static final String PARAM_MAX_BUFFER_SIZE = "accumdataMaxBufferSize";
    public static final String CLUSTER_PROP_MAX_BUFFER_SIZE = "accumdata.maxBufferSize";
    public static final Long DEFAULT_MAX_BUFFER_SIZE = 500000000L;

    public static final String PARAM_MAX_IDLE_BUFFER_AGE = "accumdataMaxIdleBufferAge";
    public static final String CLUSTER_PROP_MAX_IDLE_BUFFER_AGE = "accumdata.maxIdleBufferAge";
    public static final Long DEFAULT_MAX_IDLE_BUFFER_AGE = 86400L;

    private static final AssertionNodeNameFactory<BufferDataAssertion> NODE_NAME_FACTORY = new AssertionNodeNameFactory<BufferDataAssertion>() {
        @Override
        public String getAssertionName(final BufferDataAssertion assertion, final boolean decorate) {
            if ( !decorate ) {
                return BASE_NAME;
            }

            final StringBuilder sb = new StringBuilder( BASE_NAME );

            if ( assertion.getBufferName() != null && assertion.getBufferName().trim().length() > 0 ) {
                sb.append( " to buffer \"" ).append( assertion.getBufferName() ).append( '"' );
            }

            if ( assertion.getNewDataVarName() != null && assertion.getNewDataVarName().trim().length() > 0 ) {
                sb.append( " from variable ${" ).append( assertion.getNewDataVarName() ).append( '}' );
            }

            if ( assertion.getMaxSizeBytes() > 0 ) {
                StorageUnit unit = findBestStorageUnit( assertion.getMaxSizeBytes() );
                long num = assertion.getMaxSizeBytes() / unit.getBytesPerUnit();
                sb.append( " of up to " ).append( num ).append( ' ' ).append( unit );
            }

            if ( assertion.getMaxAgeMillis() > 0 ) {
                TimeUnit unit = findBestTimeUnit( assertion.getMaxAgeMillis() );
                long num = assertion.getMaxAgeMillis() / unit.getMultiplier();
                sb.append( " for up to " ).append( num ).append( ' ' ).append( unit );
            }

            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        props.put( CLUSTER_PROP_MAX_BUFFER_SIZE, new String[] {
                "The largest maximum buffer size that may be specified for the Accumulate Data in Memory Assertion. (bytes, default=500000000)",
                Long.toString( DEFAULT_MAX_BUFFER_SIZE )
        });
        props.put( CLUSTER_PROP_MAX_IDLE_BUFFER_AGE, new String[] {
                "The maximum amount of time an idle buffer created by the Accumulate Data in Memory Assertion is kept in memory before " +
                        "being discarded for going unused. (seconds, default=86400)",
                Long.toString( DEFAULT_MAX_IDLE_BUFFER_AGE )
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put( AssertionMetadata.SHORT_NAME, BASE_NAME );
        meta.put( AssertionMetadata.LONG_NAME, BASE_NAME );
        meta.put( AssertionMetadata.DESCRIPTION, "Append data to a buffer in memory that accumulates across multiple requests. " +
                "If the buffer is full, or was last extracted too long ago, atomically extract the buffer contents to a message variable." );

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "audit" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/ServerRegistry.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put( AssertionMetadata.POLICY_NODE_NAME_FACTORY, NODE_NAME_FACTORY );

        meta.put( AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.bufferdata.console.BufferDataPropertiesDialog" );

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:BufferData" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
