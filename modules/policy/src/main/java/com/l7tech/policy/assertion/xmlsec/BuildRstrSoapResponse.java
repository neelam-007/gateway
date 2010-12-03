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
public class BuildRstrSoapResponse extends MessageTargetableAssertion {
    public static final String DEFAULT_VARIABLE_PREFIX = "responseBuilder";
    public static final String VARIABLE_RSTR_RESPONSE = "rstrResponse";
    public static final String VARIABLE_WSA_NAMESPACE = "wsaNamespace";
    public static final String VARIABLE_RSTR_WSA_ACTION = "rstrWsaAction";

    public static final long DEFAULT_LIFETIME = 60 * 60 * 1000;  // Unit: milliseconds.  Set the default as 1 hours.
    public static final int AUTOMATIC_KEY_SIZE = 0; // 0 means automatically retrieving the key size from the inbound RST message.

    private static final String ASSERTION_BASIC_NAME = "Build RSTR SOAP Response";
    private static final String META_INITIALIZED = BuildRstrSoapResponse.class.getName() + ".metadataInitialized";

    private boolean responseForIssuance = true; // Default: true.  "false" means the response for Cancellation.
    private boolean includeAppliesTo;
    private boolean includeLifetime;
    private boolean includeAttachedRef;
    private boolean includeUnattachedRef;
    private boolean includeKeySize;

    private String tokenIssued; // It is a context variable.
    private String addressOfEPR; // The address attribute of the endpoint reference
    private long lifetime = DEFAULT_LIFETIME; // Unit: milliseconds.
    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private int keySize = AUTOMATIC_KEY_SIZE; // Unit: bits. Default set as automatic key size.
    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;

    public BuildRstrSoapResponse() {}

    public boolean isResponseForIssuance() {
        return responseForIssuance;
    }

    public void setResponseForIssuance(boolean responseForIssuance) {
        this.responseForIssuance = responseForIssuance;
    }

    public boolean isIncludeAppliesTo() {
        return includeAppliesTo;
    }

    public void setIncludeAppliesTo(boolean includeAppliesTo) {
        this.includeAppliesTo = includeAppliesTo;
    }

    public boolean isIncludeLifetime() {
        return includeLifetime;
    }

    public void setIncludeLifetime(boolean includeLifetime) {
        this.includeLifetime = includeLifetime;
    }

    public boolean isIncludeAttachedRef() {
        return includeAttachedRef;
    }

    public void setIncludeAttachedRef(boolean includeAttachedRef) {
        this.includeAttachedRef = includeAttachedRef;
    }

    public boolean isIncludeUnattachedRef() {
        return includeUnattachedRef;
    }

    public void setIncludeUnattachedRef(boolean includeUnattachedRef) {
        this.includeUnattachedRef = includeUnattachedRef;
    }

    public boolean isIncludeKeySize() {
        return includeKeySize;
    }

    public void setIncludeKeySize(boolean includeKeySize) {
        this.includeKeySize = includeKeySize;
    }

    public String getTokenIssued() {
        return tokenIssued;
    }

    public void setTokenIssued(String tokenIssued) {
        this.tokenIssued = tokenIssued;
    }

    public String getAddressOfEPR() {
        return addressOfEPR;
    }

    public void setAddressOfEPR(String addressOfEPR) {
        this.addressOfEPR = addressOfEPR;
    }

    // Unit: milliseconds.
    public long getLifetime() {
        return lifetime;
    }

    // Unit: milliseconds.
    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    // Unit: bits
    public int getKeySize() {
        return keySize;
    }

    // Unit: bits
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public String getVariablePrefix() {
        if (variablePrefix == null || variablePrefix.trim().isEmpty()) variablePrefix = DEFAULT_VARIABLE_PREFIX;

        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = (variablePrefix == null || variablePrefix.trim().isEmpty())? DEFAULT_VARIABLE_PREFIX : variablePrefix;
    }

    public String[] getVariableSuffixes() {
        return new String[] {
            VARIABLE_RSTR_RESPONSE,
            VARIABLE_WSA_NAMESPACE,
            VARIABLE_RSTR_WSA_ACTION,
        };
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
        meta.put(AssertionMetadata.DESCRIPTION, "Build a SOAP response containing a RequestSecurityTokenResponse element in SOAP body.");

        // Add to palette folder(s)
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        //TODO: name factory

        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.BuildRstrSoapResponsePropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "RSTR SOAP Response Builder Properties");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(variablePrefix + "." + VARIABLE_RSTR_RESPONSE, false, false, null, false, DataType.MESSAGE),
            new VariableMetadata(variablePrefix + "." + VARIABLE_WSA_NAMESPACE, false, false, null, false, DataType.STRING),
            new VariableMetadata(variablePrefix + "." + VARIABLE_RSTR_WSA_ACTION, false, false, null, false, DataType.STRING),
        };
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames((tokenIssued == null? "" : tokenIssued) + (addressOfEPR == null? "" : addressOfEPR));
    }
}