package com.l7tech.policy.assertion;

import com.l7tech.policy.PolicyUtil;
import com.l7tech.util.Functions;
import com.l7tech.xml.NamespaceMigratable;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for changing namespaces in one or more assertions that may be NamespaceMigratable.
 */
public class NamespaceMigrator implements Functions.UnaryVoid<Assertion> {
    private Map<String, String> nsUriSourceToDest;

    public NamespaceMigrator(Map<String, String> nsUriSourceToDest) {
        this.nsUriSourceToDest = new HashMap<String, String>(nsUriSourceToDest);
    }

    /**
     * Perform migration for the specified assertion and all descendants.
     *
     * @param root   the root of the assertion subtree whose namespaces to migrate.  Required.
     * @param assertionTranslator an AssertionTranslator for expanding Include assertions, or null to use the current default.
     */
    public void migrateDescendantsAndSelf(Assertion root, AssertionTranslator assertionTranslator) {
        PolicyUtil.visitDescendantsAndSelf(root, this, assertionTranslator);
    }

    /**
     * Perform migration for the specified assertion, but none of its descendants (if any).
     *
     * @param target a single assertion to migrate, or null.  This method does nothing unless
     *               this assertion implements NamespaceMigratable.
     */
    public void migrate(Assertion target) {
        if (target instanceof NamespaceMigratable) {
            NamespaceMigratable migratable = (NamespaceMigratable) target;
            migratable.migrateNamespaces(nsUriSourceToDest);
        }
    }

    @Override
    public void call(Assertion assertion) {
        migrate(assertion);
    }
}

