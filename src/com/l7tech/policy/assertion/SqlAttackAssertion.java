/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

import java.util.*;

/**
 * Assertion that triggers the canned SQL attack threat protection behavior.
 */
@ProcessesRequest
public class SqlAttackAssertion extends Assertion {
    public static final String PROT_METATEXT = "SqlMetaText";
    public static final String PROT_META = "SqlMeta";
    public static final String PROT_MSSQL = "MsSql";
    public static final String PROT_ORASQL = "OraSql";

    private static final int NAME = 0;
    private static final int LABEL = 1;
    private static final int DESC = 2;
    private static final int REGEX = 3;

    // Regex that matches SQL metacharacters, either directly or in SQL escape character form.
    private static final String RE_SQLMETA = "--|['#]";

    private static final String PROTS[][] = {
            { PROT_MSSQL,       "Known MS SQL Server Exploits Protection",
                                "Blocks messages which appear to contain common MS SQL Server exploits.",
                                "(?i)exec[\\s\\+]+(sp|xp)\\w+",
            },
            { PROT_ORASQL,      "Known Oracle Exploit Protection",
                                "Blocks messages which appear to contain common Oracle exploits.",
                                "(?i)\\bto_timestamp_tz\\b|\\btz_offset\\b|\\bbfilename\\b",
            },
            { PROT_METATEXT,    "Standard SQL Injection Attack Protection",
                                "Blocks messages with SQL metacharacters in any XML TEXT or CDATA section.  Protects against most SQL injection attacks, but with many false positives.\n\nIn particular, any text containing a single-quote (') character will be blocked.",
                                ">[^<]*(?:" + RE_SQLMETA +
                                 ")[^>]*<|<\\s*!\\s*\\[CDATA\\s*\\[(?:(?!\\]\\s*\\]\\s*>).)*?(?:" +
                                        RE_SQLMETA + ").*?\\]\\s*\\]\\s*>",
            },
            { PROT_META,        "Invasive SQL Injection Attack Protection",
                                "Blocks messages with SQL metacharacters anywhere in the XML.  Protects against more SQL injection attacks, but with many more false positives.\n\nIn particular, any message containing a shorthand XPointer reference will be rejected, as will most messages containing signed XML.",
                                RE_SQLMETA,
            },
    };

    Set protections = new HashSet();

    public SqlAttackAssertion() {
    }

    /**
     * Get the list of all possible protection names that could be enforced by this assertion.
     *
     * @return a list of protections that COULD be enabled.  Never null or empty.
     */
    public static List getAllProtections() {
        List ret = new ArrayList();
        for (int i = 0; i < PROTS.length; i++) {
            String[] prot = PROTS[i];
            ret.add(prot[NAME]);
        }
        return ret;
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
     * @deprecated this method is only here for deserialization purposes and should not be called directly.
     */
    public void setProtections(Set protections) {
        this.protections = protections == null ? new HashSet() : new HashSet(protections);
    }

    /**
     * Enable the specified protection.
     *
     * @param protection  the protection name to enable.  May not be null or empty.
     */
    public void setProtection(String protection) {
        if (protection == null || protection.length() < 1)
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
     * @return a human-readable label.  Never null or empty.
     * @throws PolicyAssertionException if no protection with this name is known to this assertion.
     */
    public static String getProtectionLabel(String protection) throws PolicyAssertionException {
        String got = lookupName(protection, LABEL);
        if (got == null)
            throw new PolicyAssertionException(null, "No label for protection \"" + protection + "\"");
        return got;
    }

    /**
     * Get the human-readable long description for the specified protection name.
     *
     * @param protection the protection name whose description to look up.
     * @return a human-readable description.  Never null or empty.
     * @throws PolicyAssertionException if no protection with this name is known to this assertion.
     */
    public static String getProtectionDescription(String protection) throws PolicyAssertionException {
        String got = lookupName(protection, DESC);
        if (got == null)
            throw new PolicyAssertionException(null, "No label for protection \"" + protection + "\"");
        return got;
    }

    /**
     * Get the regular expression which detects the conditions blocked by this protection.
     *
     * @param protection the protection name whose regular expression to look up.
     * @return a String containing a regular expression, or null if no such regex was found.
     */
    public static String getProtectionRegex(String protection) {
        return lookupName(protection, REGEX);
    }

    private static String lookupName(String name, int column) {
        for (int i = 0; i < PROTS.length; i++) {
            String[] prot = PROTS[i];
            if (prot[NAME].equalsIgnoreCase(name)) {
                return prot[column];
            }
        }
        return null;
    }
}
