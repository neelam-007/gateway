package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;


/**
 * Assertion that indicates that WSS-1.1 should be used.
 */
@RequiresSOAP(wss=true)
public class WssVersionAssertion extends Assertion {
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(SHORT_NAME, "Use WS-Security 1.1");
        meta.put(DESCRIPTION, "Indicates that this service should use WSS 1.1 features like SignatureConfirmation when responding to requests.");
        meta.putNull(PROPERTIES_EDITOR_FACTORY);
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssVersionAssertionValidator");
        return meta;
    }
}
