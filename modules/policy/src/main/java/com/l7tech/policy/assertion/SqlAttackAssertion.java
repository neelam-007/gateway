/*
 * Copyright (C) 2005-2012 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.util.Functions;

import java.util.*;
import java.util.regex.Pattern;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Assertion that triggers the canned SQL attack threat protection behavior.
 */
public class SqlAttackAssertion extends MessageTargetableAssertion {
    public static final String PROT_METATEXT = "SqlMetaText";
    public static final String PROT_META = "SqlMeta";
    public static final String PROT_MSSQL = "MsSql";
    public static final String PROT_ORASQL = "OraSql";

    private static final List<String> ATTACK_PROTECTION_NAMES =
            new ArrayList<>(Arrays.asList(PROT_MSSQL, PROT_ORASQL, PROT_METATEXT, PROT_META));
    private static final Map<String,SqlAttackProtectionType> ATTACK_PROTECTION_TYPES =
            Collections.unmodifiableMap(createProtectionMap());

    /** Whether to apply protections to request URL path. */
    private boolean includeUrlPath;

    /** Whether to apply protections to request URL query string. */
    private boolean includeUrlQueryString;

    /** Whether to apply protections to request body. */
    private boolean includeBody;

    Set<String> protections = new HashSet<>();

    public SqlAttackAssertion() {
        super(false);

        includeBody = true;
    }

    private final static String baseName = "Protect Against SQL Attacks";

    final static AssertionNodeNameFactory<SqlAttackAssertion> policyNameFactory =
            new AssertionNodeNameFactory<SqlAttackAssertion>() {
        @Override
        public String getAssertionName(final SqlAttackAssertion assertion, final boolean decorate) {
            if (!decorate) return baseName;

            StringBuilder sb = new StringBuilder(baseName);

            sb.append(" [");

            if (assertion.includeUrlPath) {
                sb.append("URL Path");

                if (assertion.includeUrlQueryString) {
                    sb.append(" + URL Query String");
                }

                if (assertion.includeBody) {
                    sb.append(" + Body");
                }
            } else if (assertion.includeUrlQueryString) {
                sb.append("URL Query String");

                if (assertion.includeBody) {
                    sb.append(" + Body");
                }
            } else if (assertion.includeBody) {
                sb.append("Body");
            }

            sb.append("]");

            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "<html>Helps prevent <b>malicious code injection</b> and <b>common SQL injection</b> attacks by blocking common SQL exploits from reaching protected web services. </html>");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/SQLProtection16x16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.SqlAttackDialog");
        meta.put(PROPERTIES_ACTION_NAME, "SQL Attack Protection Properties");        
        meta.put(POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.SqlAttackAssertionAdvice");
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.SqlAttackAssertionValidator");
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, SqlAttackAssertion>(){
            @Override
            public Set<ValidatorFlag> call(SqlAttackAssertion assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
            }
        });
        return meta;
    }

    /**
     * Get the list of all possible protection names that could be enforced by this assertion.
     *
     * @return a list of protections that COULD be enabled.  Never null or empty.
     */
    public static List<String> getAllProtections() {
        return ATTACK_PROTECTION_NAMES;
    }

    /**
     * Get the set of protection names that are currently being enforced by this assertion.
     *
     * @return the set of enabled protection names.  May be empty, but never null.
     */
    public Set<String> getProtections() {
        return Collections.unmodifiableSet(protections);
    }

    /**
     * @deprecated this method is only here for deserialization purposes and should not be called directly.
     */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setProtections(Set<String> protections) {
        this.protections = protections == null ? new HashSet<String>() : new HashSet<>(protections);
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
     * Check if the option "Invasive SQL Injection Attack Protection" is enabled in this assertion.
     * @return true if the option is enabled.
     */
    public boolean isSqlMetaEnabled() {
        return (protections != null) && (! protections.isEmpty()) && protections.contains(PROT_META);
    }

    /**
     * Get the human-readable label, as for a checkbox, for the specified protection name.
     *
     * @param protection  the protection name whose label to look up.
     * @return a human-readable label. May be null.
     */
    public static String getProtectionLabel(String protection) {
        SqlAttackProtectionType protectionType = ATTACK_PROTECTION_TYPES.get(protection);

        if (protectionType == null) {
            return null;
        }

        return protectionType.getDisplayName();
    }

    /**
     * Get the human-readable long description for the specified protection name.
     *
     * @param protection the protection name whose description to look up.
     * @return a human-readable description.  May be null.
     */
    public static String getProtectionDescription(String protection) throws PolicyAssertionException {
        SqlAttackProtectionType protectionType = ATTACK_PROTECTION_TYPES.get(protection);

        if (protectionType == null) {
            return null;
        }

        return protectionType.getDescription();
    }

    /**
     * Get the Pattern which detects the conditions blocked by the specified protection.
     *
     * @param protection the protection name whose Pattern to look up.
     * @return a Pattern, or null if no such protection was found.
     */
    public static Pattern getProtectionPattern(String protection) {
        SqlAttackProtectionType protectionType = ATTACK_PROTECTION_TYPES.get(protection);

        return protectionType == null
                ? null
                : protectionType.getPattern();
    }

    /**
     * @deprecated this method is only here for deserialization purposes and should not be called directly.
     */
    @Deprecated
    public void setIncludeUrl(boolean includeUrl) {
        this.includeUrlQueryString = includeUrl;
    }

    public boolean isIncludeUrlPath() {
        return includeUrlPath;
    }

    public void setIncludeUrlPath(boolean includeUrlPath) {
        this.includeUrlPath = includeUrlPath;
    }

    public boolean isIncludeUrlQueryString() {
        return includeUrlQueryString;
    }

    public void setIncludeUrlQueryString(boolean includeUrlQueryString) {
        this.includeUrlQueryString = includeUrlQueryString;
    }

    public boolean isIncludeBody() {
        return includeBody;
    }

    public void setIncludeBody(boolean includeBody) {
        this.includeBody = includeBody;
    }

    @Override
    public void setTarget( final TargetMessageType target ) {
        super.setTarget(target);
    }

    private static Map<String, SqlAttackProtectionType> createProtectionMap() {
        HashMap<String, SqlAttackProtectionType> patternMap = new HashMap<>(4);

        patternMap.put(PROT_MSSQL, SqlAttackProtectionType.MS_SQL);
        patternMap.put(PROT_ORASQL, SqlAttackProtectionType.ORACLE);
        patternMap.put(PROT_METATEXT, SqlAttackProtectionType.META_TEXT);
        patternMap.put(PROT_META, SqlAttackProtectionType.META);

        return patternMap;
    }
}
