package com.l7tech.external.assertions.echorouting;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.HashMap;

/**
 * <code>EchoRoutingAssertion</code> is an assertion that copies the request content into the response.
 *
 * @author emil
 * @version 21-Mar-2005
 */
public class EchoRoutingAssertion extends RoutingAssertion {

    public EchoRoutingAssertion() {
        // This is because this was the effective default when the Server
        // assertion was not actually checking this property.
        setCurrentSecurityHeaderHandling(LEAVE_CURRENT_SECURITY_HEADER_AS_IS);
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "Echo Routing Assertion");
        meta.put(LONG_NAME, "Echo request to response");

        meta.put(PALETTE_NODE_NAME, "Echo Routing Assertion");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "routing" });

        meta.put(POLICY_NODE_NAME, "Echo request to response");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(WSP_EXTERNAL_NAME, "EchoRoutingAssertion"); // keep same WSP name as pre-3.7 (Bug #3605)
        final TypeMapping typeMapping = (TypeMapping)meta.get(WSP_TYPE_MAPPING_INSTANCE);
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put("EchoRouting", typeMapping);
        }});

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:EchoRouting" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }
}
