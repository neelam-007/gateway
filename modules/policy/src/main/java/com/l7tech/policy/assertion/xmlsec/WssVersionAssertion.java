package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * Assertion that indicates that WSS-1.1 should be used.
 */
@RequiresSOAP(wss=true)
public class WssVersionAssertion extends Assertion {
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Use WS-Security version 1.1");
        meta.put(AssertionMetadata.LONG_NAME, "Use WS-Security version 1.1");
        meta.put(AssertionMetadata.DESCRIPTION, "Indicates that this service should use WSS 1.1 features like SignatureConfirmation when responding to requests.");
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        return meta;
    }
}
