package com.l7tech.external.assertions.jsonwebtoken;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Decodes JSON Web Tokens - OpenID Connect addition to Mobile Access Gateway
 * User: rseminoff
 * Date: 27/11/12
 */
public class JwtDecodeAssertion extends Assertion implements SetsVariables, UsesVariables {

    private String incomingToken = null;       // This must be a variable.
    private String algorithmSecretValue = null;     // This contains the private key, password, or a ${variable}
    private int algorithmSecretLocation = JwtUtilities.SELECTED_SECRET_NONE;   // The default, No selection
    private String outputVariable = null;

    private final String baseName = "Decode JWT";

    // SUPPORTED SIGNATURE ALGORITHMS are not in this file.
    // They are in JsonWebSignature.java

    //
    // Metadata
    //
    private static final String META_INITIALIZED = JwtDecodeAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Decodes and validates a JSON Web Token.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jsonwebtoken.console.JwtDecodePanel");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, baseName + " Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.jsonwebtoken.console.JwtDecodeValidator");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<JwtDecodeAssertion>() {

            @Override
            public String getAssertionName(final JwtDecodeAssertion assertion, final boolean decorate) {
                return baseName;
            }
        });

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if ((outputVariable != null) && (!outputVariable.trim().isEmpty())) {
            return new VariableMetadata[]{
                    new VariableMetadata(outputVariable, false, false, null, true, DataType.STRING)
            };
        }
        return new VariableMetadata[0];
    }

    @Override
    public String[] getVariablesUsed() {

        StringBuilder sb = new StringBuilder();
        if ((incomingToken != null) && (!incomingToken.trim().isEmpty())) {
            parseFieldForVariables(incomingToken, sb);
        }

        if ((algorithmSecretValue != null) && (!algorithmSecretValue.trim().isEmpty()) &&
                ((algorithmSecretLocation == JwtUtilities.SELECTED_SECRET_VARIABLE) || (algorithmSecretLocation == JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64))) {
            parseFieldForVariables(algorithmSecretValue, sb);
        }

        return Syntax.getReferencedNames(sb.toString());
    }

    public String getAlgorithmSecretValue() {
        return algorithmSecretValue;
    }

    public String getIncomingToken() {
        return incomingToken;
    }

    public void setIncomingToken(String incomingToken) {
        this.incomingToken = incomingToken;
    }

    public String getOutputVariable() {
        return outputVariable;
    }

    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }

    public void setAlgorithmSecretValue(String algorithmSecretValue) {
        this.algorithmSecretValue = algorithmSecretValue;
    }

    public int getAlgorithmSecretLocation() {
        return algorithmSecretLocation;
    }

    public void setAlgorithmSecretLocation(int algorithmSecretLocation) {
        this.algorithmSecretLocation = algorithmSecretLocation;
    }

    /**
     * This will process a passed variableField for variables, and add any variables found to the passed StringBuilder.
     */
    private void parseFieldForVariables(String variableField, StringBuilder sb) {
        // Any text in the field is treated as a variable.
        // The user is allowed to use multiple ${...}s in the field, we need to expand them.
        String[] totalVariables = Syntax.getReferencedNames(variableField);

        if (totalVariables.length > 0) {
            // We have at least one variable declared here with ${...}
            for (String incomingVariable : totalVariables) {
                sb.append(Syntax.SYNTAX_PREFIX).append(incomingVariable).append(Syntax.SYNTAX_SUFFIX);
            }
        } else {
            // There are no variables declared with ${...}, so we must assume the text is a single variable without
            // the prefix or suffix.
            sb.append(Syntax.SYNTAX_PREFIX).append(variableField).append(Syntax.SYNTAX_SUFFIX);
        }
    }
}
