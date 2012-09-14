/**
 * Copyright (C) 2012 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.util.EnumTranslator;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Enum for types of SQL attack protection.
 *
 * @author jwilliams
 */
public class SqlAttackProtectionType implements Serializable {
    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal++;

    /**
     * String representation used in XML serialization. Must not change for backward compatibility.
     */
    private final String _wspName;

    /**
     * For UI display. Can be internationalized.
     */
    private final String _displayName;

    /**
     * For UI display. Can be internationalized.
     */
    private final String _description;

    /**
     * Regular expression pattern to detect code injection.
     */
    private final Pattern _pattern;

    /**
     * Map for looking up instance by wspName.
     * The keys are of type String. The values are of type {@link com.l7tech.policy.assertion.SqlAttackProtectionType}.
     */
    private static final Map<String, SqlAttackProtectionType> _byWspName = new HashMap<String, SqlAttackProtectionType>();

    /**
     * Map for looking up instance by displayName.
     * The keys are of type String. The values are of type {@link com.l7tech.policy.assertion.SqlAttackProtectionType}.
     */
    private static final Map<String, SqlAttackProtectionType> _byDisplayName = new HashMap<String, SqlAttackProtectionType>();

    // Regex that matches SQL metacharacters, either directly or in SQL escape character form.
    private static final String SQL_METACHARACTERS = "--|['#]";

    // Warning: Never change wspName; in order to maintain backward compatibility.

    public static final SqlAttackProtectionType MS_SQL = new SqlAttackProtectionType(
            "MsSql",
            "Known MS SQL Server Exploits Protection",
            "Blocks messages which appear to contain common MS SQL Server exploits.",
            Pattern.compile("(?i)exec[\\s\\+]+(sp|xp)\\w+", Pattern.DOTALL | Pattern.MULTILINE));

    public static final SqlAttackProtectionType ORACLE = new SqlAttackProtectionType(
            "OraSql",
            "Known Oracle Exploit Protection",
            "Blocks messages which appear to contain common Oracle exploits.",
            Pattern.compile("(?i)\\bto_timestamp_tz\\b|\\btz_offset\\b|\\bbfilename\\b",
                    Pattern.DOTALL | Pattern.MULTILINE));

    public static final SqlAttackProtectionType META_TEXT = new SqlAttackProtectionType(
            "SqlMetaText",
            "Standard SQL Injection Attack Protection",
            "Blocks messages with SQL metacharacters in any XML TEXT or CDATA section.  Protects against most SQL injection attacks, but with many false positives.\n\nIn particular, any text containing a single-quote (') character will be blocked.",
            Pattern.compile(">[^<]*(?:" + SQL_METACHARACTERS +
                    ")[^>]*<|<\\s*!\\s*\\[CDATA\\s*\\[(?:(?!\\]\\s*\\]\\s*>).)*?(?:" +
                    SQL_METACHARACTERS + ").*?\\]\\s*\\]\\s*>", Pattern.DOTALL | Pattern.MULTILINE));

    public static final SqlAttackProtectionType META = new SqlAttackProtectionType(
            "SqlMeta",
            "Invasive SQL Injection Attack Protection",
            "Blocks messages with SQL metacharacters anywhere in the XML.  Protects against more SQL injection attacks, but with many more false positives.\n\nIn particular, any message containing a shorthand XPointer reference will be rejected, as will most messages containing signed XML.",
            Pattern.compile(SQL_METACHARACTERS, Pattern.DOTALL | Pattern.MULTILINE));

    private static final SqlAttackProtectionType[] _values = new SqlAttackProtectionType[]{
            MS_SQL,
            ORACLE,
            META_TEXT,
            META};

    public static SqlAttackProtectionType[] values() {
        return _values.clone();
    }

    /**
     * Returns the enum constant of this type with the specified wspName.
     *
     * @param wspName wspName of enum constant
     * @return the enum constant with the specified wspName; <code>null</code> if
     *         this enum type has no constant with the specified wspName
     */
    public static SqlAttackProtectionType fromWspName(final String wspName) {
        return _byWspName.get(wspName);
    }

    public static SqlAttackProtectionType fromDisplayName(final String displayName) {
        return _byDisplayName.get(displayName);
    }

    private SqlAttackProtectionType(final String wspName,
                                    final String displayName,
                                    final String description,
                                    final Pattern pattern) {
        _wspName = wspName;
        _displayName = displayName;
        _description = description;
        _pattern = pattern;
        _byWspName.put(wspName, this);
        _byDisplayName.put(displayName, this);
    }

    /**
     * @return string representation used in XML serialization
     */
    public String getWspName() {
        return _wspName;
    }

    /**
     * @return name for UI display
     */
    public String getDisplayName() {
        return _displayName;
    }

    /**
     * @return long description for UI display (suitable for tooltip or help)
     */
    public String getDescription() {
        return _description;
    }

    /**
     * @return the regular expression pattern used to detect code injection
     */
    public Pattern getPattern() {
        return _pattern;
    }

    /**
     * @return a String representation suitable for UI display
     */
    public String toString() {
        return _displayName;
    }

    protected Object readResolve() throws ObjectStreamException {
        return _values[_ordinal];
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            @Override
            public String objectToString(Object target) {
                SqlAttackProtectionType type = (SqlAttackProtectionType) target;
                return type.getWspName();
            }

            @Override
            public Object stringToObject(String wspName) throws IllegalArgumentException {
                SqlAttackProtectionType type = SqlAttackProtectionType.fromWspName(wspName);
                if (type == null)
                    throw new IllegalArgumentException("Unknown SqlAttackProtectionType: '" + wspName + "'");
                return type;
            }
        };
    }
}
