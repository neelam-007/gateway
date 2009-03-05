package com.l7tech.external.assertions.whichmodule;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.AssertionMapping;

import java.util.HashMap;

/**
 * This assertion demonstrates how to install a global compatibility mapping.
 * <p/>
 * When this assertion is registered, and WSP_COMPATIBILITY_MAPPINGS work as expected,
 * policy XML containing a TrueAssertion that has had its "TrueAssertion" element renamed
 * to "BogusTrueAssertion" will still work as expected.
 * <p/>
 * This assertion itself cannot be added to a policy.
 */
public class TrueAssertionCompatMappingAssertion extends Assertion {
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put("BogusTrueAssertion", new AssertionMapping(TrueAssertion.class, "BogusTrueAssertion"));
        }});
        return meta;
    }
}
