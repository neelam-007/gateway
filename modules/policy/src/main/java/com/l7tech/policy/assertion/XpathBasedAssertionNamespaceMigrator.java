package com.l7tech.policy.assertion;

import com.l7tech.policy.PolicyUtil;
import com.l7tech.util.Functions;
import com.l7tech.xml.xpath.XpathExpression;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for changing namespaces in one or more assertions that may be XpathBasedAssertions.
 */
public class XpathBasedAssertionNamespaceMigrator implements Functions.UnaryVoid<Assertion> {
    private Map<String, String> nsUriSourceToDest;

    public XpathBasedAssertionNamespaceMigrator(Map<String, String> nsUriSourceToDest) {
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
     *               this is an XpathBasedAssertion.
     */
    public void migrate(Assertion target) {
        if (target instanceof XpathBasedAssertion) {
            migrateXpathBasedAssertion((XpathBasedAssertion) target);
        }
    }

    @Override
    public void call(Assertion assertion) {
        migrate(assertion);
    }

    private void migrateXpathBasedAssertion(XpathBasedAssertion xba) {
        XpathExpression xpath = xba.getXpathExpression();

        Map<String, String> origNsMap = xpath.getNamespaces();
        Map<String, String> newNsMap = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : origNsMap.entrySet()) {
            String origUri = entry.getValue();
            String newUri = nsUriSourceToDest.get(origUri);
            newNsMap.put(entry.getKey(), newUri != null ? newUri : origUri);
        }

        xba.setXpathExpression(new XpathExpression(xpath.getExpression(), newNsMap));
    }
}

