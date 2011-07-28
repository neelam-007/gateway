package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TimeUnit;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_NAME;

/**
 * @author ghuang
 */
public class CreateSecurityContextToken extends MessageTargetableAssertion {
    public static final long DEFAULT_SESSION_DURATION = 2*60*60*1000;  // Unit: milliseconds.  Set the default as 2 hours.
    public static final long MIN_SESSION_DURATION = 60*1000; // 1 min
    public static final long MAX_SESSION_DURATION = 24*60*60*1000; // 24 hrs

    public static final String DEFAULT_VARIABLE_PREFIX = "sctBuilder";
    public static final String VARIABLE_ISSUED_SCT = "issuedSCT";

    private static final String ASSERTION_BASIC_NAME = "Create Security Context Token";
    private static final String META_INITIALIZED = CreateSecurityContextToken.class.getName() + ".metadataInitialized";

    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;
    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private long lifetime = DEFAULT_SESSION_DURATION; // Unit: milliseconds.
    private int keySize;
    private boolean useSystemDefaultSessionDuration = true;

    public CreateSecurityContextToken() {}
    
    public String getVariablePrefix() {
        if (variablePrefix == null || variablePrefix.trim().isEmpty()) variablePrefix = DEFAULT_VARIABLE_PREFIX;

        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = (variablePrefix == null || variablePrefix.trim().isEmpty())? DEFAULT_VARIABLE_PREFIX : variablePrefix;
    }

    public String[] getVariableSuffixes() {
        return new String[] {
            VARIABLE_ISSUED_SCT,
        };
    }

    // Unit: bits
    public int getKeySize() {
        return keySize;
    }

    // Unit: bits
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public long getLifetime() { // Unit: milliseconds.
        return lifetime;
    }

    public void setLifetime(long lifetime) { // Unit: milliseconds.
        this.lifetime = lifetime;
    }

    public boolean isUseSystemDefaultSessionDuration() {
        return useSystemDefaultSessionDuration;
    }

    public void setUseSystemDefaultSessionDuration(boolean useSystemDefaultSessionDuration) {
        this.useSystemDefaultSessionDuration = useSystemDefaultSessionDuration;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, ASSERTION_BASIC_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Create a security context token containing a secure conversation session identifier.");

        // Add to palette folder(s)
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/trust.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/trust.png");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.CreateSecurityContextTokenPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Security Context Token Creator Properties");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
            new VariableMetadata(variablePrefix + "." + VARIABLE_ISSUED_SCT, false, false, null, false, DataType.STRING)
        );
    }
}