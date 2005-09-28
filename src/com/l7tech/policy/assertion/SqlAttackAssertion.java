/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import java.util.*;

/**
 * Assertion that triggers the canned SQL attack threat protection behavior.
 */
public class SqlAttackAssertion extends Assertion {
    private static final String PROTECTIONS[] = {
            "SqlMetaText",
            "SqlMeta",
            "MsSql",
            "MySql",
            "OraSql",
    };

    public static final String PROTECTION_LABELS[] = {
            "Protect against SQL injection (normal)",
            "Protect against SQL injection (agressive)",
            "Protect against common MS SQL Server exploits",
            "Protect against common MySQL exploits",
            "Protect against common Oracle exploits",
    };

    public static final String PROTECTION_DESCRIPTIONS[] = {
            "Blocks messages with SQL escape characters in any XML TEXT or CDATA section.  Protects against most SQL injection attacks, with fewer false positives.",
            "Blocks messages with SQL escape characters anywhere in the XML.  Protects against almost all SQL injection attacks, but with many false positives.\n\nIn particular, any message containing a shorthand XPointer reference will be rejected, as will most messages containing signed XML.",
            "Blocks messages which appear to contain common MS SQL Server exploits.",
            "Blocks messages which appear to contain common MySQL exploits.",
            "Blocks messages which appear to contain common Oracle exploits.",
    };

    Set protections = new HashSet();

    public SqlAttackAssertion() {
    }

    /**
     * Get the list of all possible protection names that could be enforced by this assertion.
     *
     * @return a list of protections that COULD be enabled.  Never null or empty.
     */
    public List getAllProtections() {
        return Arrays.asList(PROTECTIONS);
    }

    /**
     * Get the set of protection names that are currently being enforced by this assertion.
     *
     * @return the set of enabled protection names.  May be empty, but never null.
     */
    public Set getProtections() {
        return Collections.unmodifiableSet(protections);
    }

    /**
     * Enable the specified protection.
     *
     * @param protection  the protection name to enable.  May not be null or empty.
     */
    public void setProtection(String protection) {
        if (protection == null || protection.length() > 0)
            throw new IllegalArgumentException("Protection name must not be null or empty");
        this.protections.add(protection);
    }

    /**
     * Disable the specified protection.
     *
     * @param protection  the protection name to disable.
     */
    public void removeProtection(String protection) {
        this.protections.remove(protection);
    }

    /**
     * Get the human-readable label, as for a checkbox, for the specified protection name.
     *
     * @param protection  the protection name whose label to look up.
     * @return a human-readable label.  Never null.
     * @throws PolicyAssertionException if no protection with this name is known to this assertion.
     */
    public String getProtectionLabel(String protection) throws PolicyAssertionException {
        for (int i = 0; i < PROTECTIONS.length; i++) {
            String s = PROTECTIONS[i];
            if (s.equalsIgnoreCase(protection)) {
                return PROTECTION_LABELS[i];
            }
        }
        throw new PolicyAssertionException("No label for protection \"" + protection + "\"");
    }


    public String getProtectionDescription(String protection) throws PolicyAssertionException {
        for (int i = 0; i < PROTECTIONS.length; i++) {
            String s = PROTECTIONS[i];
            if (s.equalsIgnoreCase(protection)) {
                return PROTECTION_DESCRIPTIONS[i];
            }
        }
        throw new PolicyAssertionException("No label for protection \"" + protection + "\"");
    }

}
