package com.l7tech.external.assertions.jsonwebtoken;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Encodes JSON Web Tokens - OpenID Connect addition to Mobile Access Gateway
 * User: rseminoff
 * Date: 27/11/12
 */

public class JwtEncodeAssertion extends Assertion implements UsesVariables, SetsVariables {

    private String jwtHeaderVariable = ""; // Don't cause NPEs in existing policies.
    private int jwtHeaderType = JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS;

    private String jsonPayload = null;

    private String signatureValue = null;  // This will contain either the name of the algorithm or a ${variable}
    private int signatureSelected = JwtUtilities.SELECTED_SIGNATURE_NONE;   // The default.  No selection.

    private String algorithmSecretValue = null;     // This contains the private key, password, or a ${variable}
    private int algorithmSecretLocation = JwtUtilities.SELECTED_SECRET_NONE;   // The default, No selection

    private String outputVariable = null;

    private final String baseName = "Encode JWT";

    // SUPPORTED SIGNATURE ALGORITHMS are not in this file.
    // They are in JsonWebSignature.java

    //
    // Metadata
    //
    private static final String META_INITIALIZED = JwtEncodeAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Creates a JSON Web Token with an optional header or appended claims, optionally encoded with a digital signature.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jsonwebtoken.console.JwtEncodePanel");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, baseName + " Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.jsonwebtoken.console.JwtEncodeValidator");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set nice, informative policy node name for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<JwtEncodeAssertion>() {
            @Override
            public String getAssertionName(final JwtEncodeAssertion assertion, final boolean decorate) {
                return baseName;
            }
        });

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(outputVariable, false, false, null, true, DataType.STRING)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        StringBuilder sb = new StringBuilder();

        if ((jwtHeaderVariable != null) && (!jwtHeaderVariable.trim().isEmpty())) {
            // It's always a variable, so we always treat any content as such.
            sb.append(Syntax.SYNTAX_PREFIX).append(jwtHeaderVariable).append(Syntax.SYNTAX_SUFFIX);
        }

        if ((jsonPayload != null) && (!jsonPayload.trim().isEmpty())) {
            parseFieldForVariables(jsonPayload, sb);
        }

        if ((signatureValue != null) && (!signatureValue.trim().isEmpty()) && (signatureSelected == JwtUtilities.SELECTED_SIGNATURE_VARIABLE)) {
            parseFieldForVariables(signatureValue, sb);
        }

        if ((algorithmSecretValue != null) && (!algorithmSecretValue.trim().isEmpty()) &&
                ((algorithmSecretLocation == JwtUtilities.SELECTED_SECRET_VARIABLE) || (algorithmSecretLocation == JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64))) {
            parseFieldForVariables(algorithmSecretValue, sb);
        }
        return Syntax.getReferencedNames(sb.toString());
    }

    public String getJwtHeaderVariable() {
        return this.jwtHeaderVariable;
    }

    public void setJwtHeaderVariable(String value) {
        this.jwtHeaderVariable = value;
    }

    public int getJwtHeaderType() {
        return jwtHeaderType;
    }

    public void setJwtHeaderType(int jwtHeaderType) {
        this.jwtHeaderType = jwtHeaderType;
    }

    public String getJsonPayload() {
        return this.jsonPayload;
    }

    public void setJsonPayload(String value) {
        this.jsonPayload = value;
    }

    public String getSignatureValue() {
        return this.signatureValue;
    }

    public String getAlgorithmSecretValue() {
        return this.algorithmSecretValue;
    }

    public String getOutputVariable() {
        return this.outputVariable;
    }

    public void setOutputVariable(String value) {
        this.outputVariable = value;
    }

    public void setSignatureValue(String value) {
        this.signatureValue = value;
    }

    public void setAlgorithmSecretValue(String value) {
        this.algorithmSecretValue = value;
    }

    public int getSignatureSelected() {
        return signatureSelected;
    }

    public void setSignatureSelected(int signatureSelected) {
        this.signatureSelected = signatureSelected;
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
