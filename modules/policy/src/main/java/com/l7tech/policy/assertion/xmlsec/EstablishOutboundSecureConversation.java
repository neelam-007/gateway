package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import com.l7tech.util.TimeUnit;

import java.util.*;

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
    private boolean allowInboundMsgUsingSession; // A flag indicates whether inbound request messages are allowed to use this outbound session.

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

    public boolean isAllowInboundMsgUsingSession() {
        return allowInboundMsgUsingSession;
    }

    public void setAllowInboundMsgUsingSession(boolean allowInboundMsgUsingSession) {
        this.allowInboundMsgUsingSession = allowInboundMsgUsingSession;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(SHORT_NAME, ASSERTION_BASIC_NAME);
        meta.put(DESCRIPTION, "Establish Outbound WS-Secure Conversation");

        // Add to palette folder(s)
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/trust.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<EstablishOutboundSecureConversation>() {
            @Override
            public String getAssertionName(EstablishOutboundSecureConversation assertion, boolean decorate) {
                if (! decorate) return ASSERTION_BASIC_NAME;

                final String serviceUrl = assertion.getServiceUrl();
                final String name = serviceUrl == null || serviceUrl.trim().isEmpty() ?
                        ASSERTION_BASIC_NAME :
                        ASSERTION_BASIC_NAME + " to " + TextUtils.truncateStringAtEnd(serviceUrl, 128);
                return AssertionUtils.decorateName(assertion, name);
            }
        });

        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/trust.png");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.EstablishOutboundSecureConversationPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Outbound Secure Conversation Establishment Properties");
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, EstablishOutboundSecureConversation>(){
            @Override
            public Set<ValidatorFlag> call(EstablishOutboundSecureConversation assertion) {
                return EnumSet.of(ValidatorFlag.MAY_TARGET_REQUEST_AFTER_RESPONSE);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(VARIABLE_SESSION, false, false, null, false, DataType.UNKNOWN)
        );
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(
                serviceUrl,
                clientEntropy,
                serverEntropy,
                keySize,
                fullKey,
                creationTime,
                expirationTime
        ).withVariables( securityContextTokenVarName );
    }
}