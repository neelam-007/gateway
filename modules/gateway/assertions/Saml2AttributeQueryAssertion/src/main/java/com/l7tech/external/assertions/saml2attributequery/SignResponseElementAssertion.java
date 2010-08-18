package com.l7tech.external.assertions.saml2attributequery;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.util.Functions;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28-Jan-2009
 * Time: 7:25:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class SignResponseElementAssertion extends Assertion implements UsesVariables {
    public static final int REQUEST_MESSAGE = 1;
    public static final int RESPONSE_MESSAGE = 2;
    public static final int MESSAGE_VARIABLE = 3;

    //- PUBLIC

    /**
     * Bean constructor
     */
    public SignResponseElementAssertion() {
    }

    public String[] getVariablesUsed() {
        if(privateKeyDnVariable == null) {
            return new String[0];
        } else {
            return new String[] {privateKeyDnVariable};
        }
    }

    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
    }

    public String getPrivateKeyDnVariable() {
        return privateKeyDnVariable;
    }

    public void setPrivateKeyDnVariable(String privateKeyDnVariable) {
        this.privateKeyDnVariable = privateKeyDnVariable;
    }

    public String getKeyReference() {
        return keyReference;
    }

    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    public int getInputMessageSource() {
        return inputMessageSource;
    }

    public void setInputMessageSource(int inputMessageSource) {
        this.inputMessageSource = inputMessageSource;
    }

    public String getInputMessageVariableName() {
        return inputMessageVariableName;
    }

    public void setInputMessageVariableName(String inputMessageVariableName) {
        this.inputMessageVariableName = inputMessageVariableName;
    }

    /**
     * Get metadata for this assertion.
     *
     * @return The assertion metadata.
     */
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Enveloped Signature");
        meta.put(AssertionMetadata.LONG_NAME, "Sign Response Element (enveloped)");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, SignResponseElementAssertion>() {
            public String call(SignResponseElementAssertion assertion) {
                if(assertion.getXpathExpression() == null) {
                    return "Enveloped Signature";
                } else {
                    return "Enveloped Signature " + assertion.getXpathExpression().getExpression();
                }
            }
        });

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Disable automatic properties editor
        //meta.putNull(AssertionMetadata.PROPERTIES_ACTION_FACTORY);

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.saml2attributequery.console.SignResponseElementAssertionPropertiesEditor");

        // Disable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:CertificateAttributes" rather than "set:modularAssertions"

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private XpathExpression xpathExpression;
    private String privateKeyDnVariable;
    private String keyReference = KeyReference.SKI.getName();
    private int inputMessageSource = RESPONSE_MESSAGE;
    private String inputMessageVariableName;

    private static final String META_INITIALIZED = SignResponseElementAssertion.class.getName() + ".metadataInitialized";
}
