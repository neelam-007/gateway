package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.token.SecurityTokenType;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 *
 */
@RequiresSOAP
public class ProcessRstrSoapResponse extends MessageTargetableAssertion {

    //- PUBLIC
    public static final String DEFAULT_VARIABLE_PREFIX = "rstrResponseProcessor";
    public static final String VARIABLE_TOKEN = "token";
    public static final String VARIABLE_CREATE_TIME = "createTime";
    public static final String VARIABLE_EXPIRY_TIME = "expiryTime";
    public static final String VARIABLE_SERVER_ENTROPY = "serverEntropy";
    public static final String VARIABLE_KEY_SIZE = "keySize";
    public static final String VARIABLE_FULL_KEY = "fullKey";

    public ProcessRstrSoapResponse() {
    }

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType( final SecurityTokenType tokenType ) {
        this.tokenType = tokenType;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }

    public static String[] getVariableSuffixes() {
        return new String[] {
            VARIABLE_TOKEN,
            VARIABLE_CREATE_TIME,
            VARIABLE_EXPIRY_TIME,
            VARIABLE_SERVER_ENTROPY,
            VARIABLE_KEY_SIZE,
            VARIABLE_FULL_KEY,
        };
    }

    @Override
    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(SHORT_NAME, ASSERTION_BASIC_NAME);
        meta.put(DESCRIPTION, "Process the RSTR response message to get the security token such as Security Context Token or SAML token.");

        // Add to palette folder(s)
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ProcessRstrSoapResponsePropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "RSTR Response Processor Properties");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    //- PROTECTED

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(variablePrefix + "." + VARIABLE_TOKEN, false, false, null, false, DataType.ELEMENT ),
                new VariableMetadata(variablePrefix + "." + VARIABLE_CREATE_TIME, false, false, null, false),
                new VariableMetadata(variablePrefix + "." + VARIABLE_EXPIRY_TIME, false, false, null, false),
                new VariableMetadata(variablePrefix + "." + VARIABLE_SERVER_ENTROPY, false, false, null, false),
                new VariableMetadata(variablePrefix + "." + VARIABLE_KEY_SIZE, false, false, null, false, DataType.INTEGER),
                new VariableMetadata(variablePrefix + "." + VARIABLE_FULL_KEY, false, false, null, false)
        );
    }

    //- PRIVATE

    private static final String ASSERTION_BASIC_NAME = "Process RSTR Response";
    private static final String META_INITIALIZED = ProcessRstrSoapResponse.class.getName() + ".metadataInitialized";

    private SecurityTokenType tokenType = SecurityTokenType.UNKNOWN;
    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;
}
