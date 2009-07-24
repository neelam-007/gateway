package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions;

import java.util.logging.Logger;

/**
 * Immediately verify one or more signed Elements in a non-SOAP XML message.
 */
public class NonSoapVerifyElementAssertion extends NonSoapSecurityAssertionBase {
    private static final Logger logger = Logger.getLogger(NonSoapVerifyElementAssertion.class.getName());
    private static final String META_INITIALIZED = NonSoapVerifyElementAssertion.class.getName() + ".metadataInitialized";

    public NonSoapVerifyElementAssertion() {
        super(TargetMessageType.REQUEST);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Immediate Verify (Non-SOAP) XML Element");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately verify one or more signatures of the message.  " +
                                                "This does not require a SOAP Envelope and does not examine or produce WS-Security processor results.  " +
                                                "Instead, this assertion examines the target message immediately.  The XPath should match the Signature elements " +
                                                "which are to be verified.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1110);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapVerifyElementAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, NonSoapVerifyElementAssertion>() {
            @Override
            public String call( final NonSoapVerifyElementAssertion ass ) {
                StringBuilder name = new StringBuilder("Immediately Verify (Non-SOAP) XML Element ");
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
