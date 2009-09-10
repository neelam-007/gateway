package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.util.Functions;

/**
 *
 */
public class NonSoapCheckVerifyResultsAssertion extends NonSoapSecurityAssertionBase {
    private static final String META_INITIALIZED = NonSoapCheckVerifyResultsAssertion.class.getName() + ".metadataInitialized";

    public NonSoapCheckVerifyResultsAssertion() {
        super(TargetMessageType.REQUEST);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Check Non-SOAP XML Verify Results");
        meta.put(META_PROP_VERB, "check");
        meta.put(AssertionMetadata.DESCRIPTION, "Check the results of verifying a non-SOAP XML signature to see if expected elements were signed.  " +
                                                "This does not require a SOAP Envelope. This requires context variables from a Non-SOAP XML Verify assertion " +
                                                "(but does not examine or produce WS-Security processor results). " +
                                                "The XPath should match the elements which are expected to have been signed.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1150);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapCheckVerifyResultsAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, NonSoapCheckVerifyResultsAssertion>() {
            @Override
            public String call( final NonSoapCheckVerifyResultsAssertion ass ) {
                StringBuilder name = new StringBuilder("Check that XML Elements were signed ");
                if (ass.getXpathExpression() == null) {
                    name.append("[XPath expression not set]");
                } else {
                    name.append(ass.getXpathExpression().getExpression());
                }
                return AssertionUtils.decorateName(ass, name);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
