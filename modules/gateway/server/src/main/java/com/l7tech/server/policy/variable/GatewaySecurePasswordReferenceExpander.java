package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.security.password.SecurePasswordReferenceExpander;
import com.l7tech.objectmodel.FindException;

/**
 * Simple Gateway implementation of SecurePasswordReferenceExpander.
 * TODO convert into Spring bean, inject securePasswordManager, move guts of expandSinglePasswordOnlyVariable() from ServerVariables to here
 */
public class GatewaySecurePasswordReferenceExpander implements SecurePasswordReferenceExpander {
    private final Audit audit;
    private final boolean requireUseFromContextVariable;

    /**
     * Create an expander that will recognize a password that is actually a single ${secpass.FOO.plaintext} reference,
     * and that will allow use of a secpass that is not enabled for use via context variable.
     *
     * @param audit auditor.  required.
     */
    public GatewaySecurePasswordReferenceExpander(Audit audit) {
        this.audit = audit;
        this.requireUseFromContextVariable = false;
    }

    /**
     * Create an expander that will expand multiple secpass references in a template, but will require each referenced secpass
     * to be enabled for use via context variables.
     *
     * @param audit auditor.  required.
     * @param requireUseFromContextVariable  true if being used by a server assertion -- will allow multiple references, but require each secpass enable use via context variable.
     *                                       false if being referened from some entity password field -- will allow only a single secpass reference, but will work even with a secpass
     *                                                 that does nto enable use via context variable.
     */
    public GatewaySecurePasswordReferenceExpander(Audit audit, boolean requireUseFromContextVariable) {
        this.audit = audit;
        this.requireUseFromContextVariable = requireUseFromContextVariable;
    }

    @Override
    public char[] expandPasswordReference(String passwordOrSecpassRef) throws FindException {
        if (requireUseFromContextVariable) {
            return ServerVariables.expandPasswordOnlyVariable(audit, passwordOrSecpassRef).toCharArray();
        } else {
            return ServerVariables.expandSinglePasswordOnlyVariable(audit, passwordOrSecpassRef).toCharArray();
        }
    }
}
