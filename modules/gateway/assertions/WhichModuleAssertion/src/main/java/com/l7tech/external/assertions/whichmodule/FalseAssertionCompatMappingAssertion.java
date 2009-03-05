package com.l7tech.external.assertions.whichmodule;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.wsp.AssertionMapping;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.HashMap;

/**
 * This assertion demonstrates how to install a global compatibility mapping.
 * <p/>
 * When this assertion is registered, and WSP_COMPATIBILITY_MAPPINGS work as expected,
 * policy XML containing a FalseAssertion that has had its "FalseAssertion" element renamed
 * to "BogusFalseAssertion" will still work as expected.
 * <p/>
 * This assertion itself cannot be added to a policy.
 */
public class FalseAssertionCompatMappingAssertion extends Assertion {
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put("BogusFalseAssertion", new AssertionMapping(FalseAssertion.class, "BogusFalseAssertion"));
        }});
        return meta;
    }
}