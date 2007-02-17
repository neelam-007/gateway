package com.l7tech.external.assertions.echorouting;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

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
        meta.put(PALETTE_NODE_ICON, "com/l7tech/external/assertions/echorouting/console/resources/echo16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "routing" });

        meta.put(POLICY_NODE_NAME, "Echo request to response");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        return meta;
    }
}
