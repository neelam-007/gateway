package com.l7tech.policy.assertion.transport;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * This assertion is meant to instruct the XML VPN to inject the requesting windows domain
 * identity in the outgoing message to the SecureSpan Gateway. On the gateway itself, nothing
 * is evaluated by this assertion. The injected identity, if present, can be retrieved through
 * message inspection.
 */
public class RemoteDomainIdentityInjection extends Assertion implements SetsVariables {
    private String variablePrefix = "injected";

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public VariableMetadata[] getVariablesSet() {
        String prefix = getVariablePrefix();
        return new VariableMetadata[] {
                new VariableMetadata(prefix + ".user"),
                new VariableMetadata(prefix + ".domain"),
                new VariableMetadata(prefix + ".program"),
        };
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Require Remote Domain Identity");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Remote Domain Identity Properties");
        meta.put(AssertionMetadata.DESCRIPTION, "Requires the request to have included Windows domain identity information.  The XML VPN Client will recognize this assertion and, if permitted to do so, will inject this information.");
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.server.policy.assertion.transport.ServerRemoteDomainIdentityInjection");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.transport.ClientRemoteDomainIdentityInjection");
        meta.put(AssertionMetadata.PALETTE_NODE_CLIENT_ICON, "com/l7tech/proxy/resources/tree/authentication.gif");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.RemoteDomainIdentityInjectionDialog");
        return meta;
    }
}
