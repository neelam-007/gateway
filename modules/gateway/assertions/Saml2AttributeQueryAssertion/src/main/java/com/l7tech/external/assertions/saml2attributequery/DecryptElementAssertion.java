package com.l7tech.external.assertions.saml2attributequery;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.util.Functions;
import com.l7tech.xml.xpath.XpathExpression;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28-Jan-2009
 * Time: 7:25:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class DecryptElementAssertion extends Assertion {
    public static final int REQUEST_MESSAGE = 1;
    public static final int RESPONSE_MESSAGE = 2;
    public static final int MESSAGE_VARIABLE = 3;

    //- PUBLIC

    /**
     * Bean constructor
     */
    public DecryptElementAssertion() {
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

    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
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
        meta.put(AssertionMetadata.SHORT_NAME, "Decrypt SAML Element");
        meta.put(AssertionMetadata.LONG_NAME, "Decrypt SAML Element");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, DecryptElementAssertion>() {
            public String call(DecryptElementAssertion assertion) {
                if(assertion.getXpathExpression() == null) {
                    return "Decrypt SAML Element";
                } else {
                    return "Decrypt SAML Element " + assertion.getXpathExpression().getExpression();
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

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.saml2attributequery.console.DecryptElementAssertionPropertiesEditor");

        // Disable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:CertificateAttributes" rather than "set:modularAssertions"

        //TODO this assertion needs to be part of the "set:US:Assertions" feature set and can then use "(fromClass)"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private int inputMessageSource = REQUEST_MESSAGE;
    private String inputMessageVariableName;
    private XpathExpression xpathExpression;

    private static final String META_INITIALIZED = DecryptElementAssertion.class.getName() + ".metadataInitialized";
}