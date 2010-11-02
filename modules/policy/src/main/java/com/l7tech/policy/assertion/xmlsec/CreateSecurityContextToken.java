package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TimeUnit;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_NAME;

/**
 * @author ghuang
 */
public class CreateSecurityContextToken extends MessageTargetableAssertion {
    public static final String DEFAULT_VARIABLE_PREFIX = "sctBuilder";
    public static final String VARIABLE_ISSUED_SCT = "issuedSCT";
    public static final long DEFAULT_LIFETIME = 2 * 60 * 60 * 1000;  // Unit: milliseconds.  Set the default as 2 hours.

    private static final String ASSERTION_BASIC_NAME = "Create Security Context Token";
    private static final String META_INITIALIZED = CreateSecurityContextToken.class.getName() + ".metadataInitialized";

    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;
    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private long lifetime = DEFAULT_LIFETIME; // Unit: milliseconds.

    public CreateSecurityContextToken() {}
    
    public String getVariablePrefix() {
        if (variablePrefix == null) variablePrefix = DEFAULT_VARIABLE_PREFIX;

        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = (variablePrefix == null)? DEFAULT_VARIABLE_PREFIX : variablePrefix;
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
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(variablePrefix + "." + VARIABLE_ISSUED_SCT, false, false, null, false, DataType.STRING)
        };
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames((variablePrefix == null? "" : variablePrefix));
    }
}