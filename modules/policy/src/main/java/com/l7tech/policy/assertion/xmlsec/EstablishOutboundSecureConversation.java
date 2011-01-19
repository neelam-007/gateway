package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TimeUnit;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * @author ghuang
 */
public class EstablishOutboundSecureConversation extends MessageTargetableAssertion {
    public static final long MIN_SESSION_DURATION = 1000*60; // 1 min
    public static final long MAX_SESSION_DURATION = 1000*60*60*24; // 24 hrs
    public static final String VARIABLE_SESSION = "outboundSC.session";

    private static final String ASSERTION_BASIC_NAME = "Establish Outbound Secure Conversation";
    private static final String META_INITIALIZED = EstablishOutboundSecureConversation.class.getName() + ".metadataInitialized";

    private String serviceUrl;
    private String securityContextTokenVarName = ProcessRstrSoapResponse.DEFAULT_VARIABLE_PREFIX + "." + ProcessRstrSoapResponse.VARIABLE_TOKEN;
    private String clientEntropy = "${" + BuildRstSoapRequest.DEFAULT_VARIABLE_PREFIX + "." + BuildRstSoapRequest.VARIABLE_CLIENT_ENTROPY + "}";
    private String serverEntropy = "${" + ProcessRstrSoapResponse.DEFAULT_VARIABLE_PREFIX + "." + ProcessRstrSoapResponse.VARIABLE_SERVER_ENTROPY + "}";
    private String keySize = "${" + ProcessRstrSoapResponse.DEFAULT_VARIABLE_PREFIX + "." + ProcessRstrSoapResponse.VARIABLE_KEY_SIZE + "}";
    private String fullKey = "${" + ProcessRstrSoapResponse.DEFAULT_VARIABLE_PREFIX + "." + ProcessRstrSoapResponse.VARIABLE_FULL_KEY + "}";
    private String creationTime = "${" + ProcessRstrSoapResponse.DEFAULT_VARIABLE_PREFIX + "." + ProcessRstrSoapResponse.VARIABLE_CREATE_TIME + "}";
    private String expirationTime = "${" + ProcessRstrSoapResponse.DEFAULT_VARIABLE_PREFIX + "." + ProcessRstrSoapResponse.VARIABLE_EXPIRY_TIME + "}";
    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private long maxLifetime; // Unit: milliseconds.
    private boolean useSystemDefaultSessionDuration = true;

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getSecurityContextTokenVarName() {
        return securityContextTokenVarName;
    }

    public void setSecurityContextTokenVarName(String securityContextTokenVarName) {
        this.securityContextTokenVarName = securityContextTokenVarName;
    }

    public String getClientEntropy() {
        return clientEntropy;
    }

    public void setClientEntropy(String clientEntropy) {
        this.clientEntropy = clientEntropy;
    }

    public String getServerEntropy() {
        return serverEntropy;
    }

    public void setServerEntropy(String serverEntropy) {
        this.serverEntropy = serverEntropy;
    }

    public String getKeySize() {
        return keySize;
    }

    public void setKeySize( final String keySize ) {
        this.keySize = keySize;
    }

    public String getFullKey() {
        return fullKey;
    }

    public void setFullKey(String fullKey) {
        this.fullKey = fullKey;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(String expirationTime) {
        this.expirationTime = expirationTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
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
        meta.put(CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(SHORT_NAME, ASSERTION_BASIC_NAME);
        meta.put(DESCRIPTION, "Establish Outbound WS-Secure Conversation");

        // Add to palette folder(s)
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/trust.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/trust.png");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.EstablishOutboundSecureConversationPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Outbound Secure Conversation Establishment Properties");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return mergeVariablesSet(new VariableMetadata[] {
            new VariableMetadata(VARIABLE_SESSION, false, false, null, false, DataType.UNKNOWN),
        });
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        final Set<String> allVars = new LinkedHashSet<String>();
        final String[] strings = Syntax.getReferencedNames(
                serviceUrl,
                clientEntropy,
                serverEntropy,
                keySize,
                fullKey,
                creationTime,
                expirationTime
        );

        allVars.add( securityContextTokenVarName );
        allVars.addAll(Arrays.asList(strings));
        allVars.addAll(Arrays.asList(super.getVariablesUsed()));

        return allVars.toArray(new String[allVars.size()]);
    }
}