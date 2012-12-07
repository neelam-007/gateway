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
            new ArrayList<String>(Arrays.asList(PROT_MSSQL, PROT_ORASQL, PROT_METATEXT, PROT_META));
    private static final Map<String,SqlAttackProtectionType> ATTACK_PROTECTION_TYPES =
            Collections.unmodifiableMap(createProtectionMap());

    /** Whether to apply protections to request URL. */
    private boolean includeRequestUrl;

    /** Whether to apply protections to request body. */
    private boolean includeRequestBody;

    Set<String> protections = new HashSet<String>();

    public SqlAttackAssertion() {
        super(false);
    }

    private final static String baseName = "Protect Against SQL Attacks";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SqlAttackAssertion>(){
        @Override
        public String getAssertionName( final SqlAttackAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
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
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
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
    public void setProtections(Set<String> protections) {
        this.protections = protections == null ? new HashSet<String>() : new HashSet<String>(protections);
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

    public boolean isIncludeRequestUrl() {
        return includeRequestUrl;
    }

    public void setIncludeRequestUrl(boolean includeRequestUrl) {
        this.includeRequestUrl = includeRequestUrl;
    }

    public boolean isIncludeRequestBody() {
        return includeRequestBody;
    }

    public void setIncludeRequestBody(boolean includeRequestBody) {
        this.includeRequestBody = includeRequestBody;
    }

    @Override
    public void setTarget( final TargetMessageType target ) {
        super.setTarget(target);
        if ( target == TargetMessageType.REQUEST &&
                !( includeRequestUrl || includeRequestBody) ) {
            includeRequestUrl = true;
            includeRequestBody = true;
        }
    }

    private static Map<String, SqlAttackProtectionType> createProtectionMap() {
        HashMap<String, SqlAttackProtectionType> patternMap = new HashMap<String, SqlAttackProtectionType>(4);

        patternMap.put(PROT_MSSQL, SqlAttackProtectionType.MS_SQL);
        patternMap.put(PROT_ORASQL, SqlAttackProtectionType.ORACLE);
        patternMap.put(PROT_METATEXT, SqlAttackProtectionType.META_TEXT);
        patternMap.put(PROT_META, SqlAttackProtectionType.META);

        return patternMap;
    }
}
