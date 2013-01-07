/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.util.EnumTranslator;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Enum for types of code injection protection (i.e., types of attack to protect against).
 * <p/>
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionType implements Serializable, Cloneable {
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
     * Whether this type of protection is applicable to request messages.
     */
    private final boolean _applicableToRequest;

    /**
     * Whether this type of protection is applicable to response messages.
     */
    private final boolean _applicableToResponse;

    /**
     * Whether the code protects against characters which cannot always be displayed. If true, any such chars will
     * be converted to their unicode value.*
     */
    private final boolean _containsNonIdentifiableCharacters;

    /**
     * Map for looking up instance by wspName.
     * The keys are of type String. The values are of type {@link CodeInjectionProtectionType}.
     */
    private static final Map<String, CodeInjectionProtectionType> _byWspName = new HashMap<String, CodeInjectionProtectionType>();

    /**
     * Map for looking up instance by displayName.
     * The keys are of type String. The values are of type {@link CodeInjectionProtectionType}.
     */
    private static final Map<String, CodeInjectionProtectionType> _byDisplayName = new HashMap<String, CodeInjectionProtectionType>();

    // Warning: Never change wspName; in order to maintain backward compatibility.
    public static final CodeInjectionProtectionType HTML_JAVASCRIPT = new CodeInjectionProtectionType(
            "htmlJavaScriptInjection",
            "HTML/JavaScript Injection (Cross Site Scripting)",
            "Block messages which contain HTML tags that can be used to inject code (including <applet>, <body>, <embed>, <frame>, <frameset>, <html>, <iframe>, <ilayer>, <img>, <layer>, <link>, <meta>, <object>, <script>, <style>).",
            Pattern.compile("<\\s*(?:applet|body|embed|frame|frameset|html|iframe|ilayer|img|layer|link|meta|object|script|style)\\b", Pattern.CASE_INSENSITIVE),
            true,
            true);
    public static final CodeInjectionProtectionType PHP_EVAL_INJECTION = new CodeInjectionProtectionType(
            "phpEvalInjection",
            "PHP eval Injection",
            "Block messages which contain metacharacters that can be used to inject PHP code into a PHP eval statement. These metacharacters are ';\"\\",
            Pattern.compile("[';\"\\\\]"),
            true,
            false);
    public static final CodeInjectionProtectionType SHELL_INJECTION = new CodeInjectionProtectionType(
            "shellInjection",
            "Shell Injection",
            "Block messages which contain metacharacters that can be used to inject shell script into a system call statement.  These metacharacters are `;|&>\\",
            Pattern.compile("[`;|&>\\\\]"),
            true,
            false);
    public static final CodeInjectionProtectionType LDAP_DN_INJECTION = new CodeInjectionProtectionType(
            "ldapDnInjection",
            "LDAP DN Injection",
            "Block messages which contain metacharacters that can be used to inject code into LDAP DN values. These metacharacters are \\,+\"<>;",
            Pattern.compile("[\\\\,+\"<>;]"),
            true,
            true);
    public static final CodeInjectionProtectionType LDAP_SEARCH_INJECTION = new CodeInjectionProtectionType(
            "ldapSearchInjection",
            "LDAP Search Injection",
            "Block messages which contain metacharacters that can be used to inject code into LDAP search values. The metacharacters are \\*()\\u0000",
            Pattern.compile("[\\\\*()\u0000]"),
            true,
            true,
            true);
    public static final CodeInjectionProtectionType XPATH_INJECTION = new CodeInjectionProtectionType(
            "xpathInjection",
            "XPath Injection",
            "Block messages which contain metacharacters that can be used to inject code into XPath queries. The metacharacters are \"*^';&<>()",
            Pattern.compile("[\"*^';&<>()]"),
            true,
            true);

    private static final CodeInjectionProtectionType[] _values = new CodeInjectionProtectionType[]{
            HTML_JAVASCRIPT,
            PHP_EVAL_INJECTION,
            SHELL_INJECTION,
            LDAP_DN_INJECTION,
            LDAP_SEARCH_INJECTION,
            XPATH_INJECTION};

    public static CodeInjectionProtectionType[] values() {
        return _values.clone();
    }

    /**
     * Returns the enum constant of this type with the specified wspName.
     *
     * @param wspName wspName of enum constant
     * @return the enum constant with the specified wspName; <code>null</code> if
     *         this enum type has no constant with the specified wspName
     */
    public static CodeInjectionProtectionType fromWspName(final String wspName) {
        return _byWspName.get(wspName);
    }

    public static CodeInjectionProtectionType fromDisplayName(final String displayName) {
        return _byDisplayName.get(displayName);
    }

    private CodeInjectionProtectionType(final String wspName,
                                        final String displayName,
                                        final String description,
                                        final Pattern pattern,
                                        final boolean applicableToRequest,
                                        final boolean applicableToResponse,
                                        final boolean containsNonIdentifiableCharacters) {
        _wspName = wspName;
        _displayName = displayName;
        _description = description;
        _pattern = pattern;
        _applicableToRequest = applicableToRequest;
        _applicableToResponse = applicableToResponse;
        _byWspName.put(wspName, this);
        _byDisplayName.put(displayName, this);
        _containsNonIdentifiableCharacters = containsNonIdentifiableCharacters;
    }

    private CodeInjectionProtectionType(final String wspName,
                                        final String displayName,
                                        final String description,
                                        final Pattern pattern,
                                        final boolean applicableToRequest,
                                        final boolean applicableToResponse) {
        this(wspName, displayName, description, pattern, applicableToRequest, applicableToResponse, false);
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
     * @return whether this type of protection is applicable to request messages
     */
    public boolean isApplicableToRequest() {
        return _applicableToRequest;
    }

    /**
     * @return whether this type of protection is applicable to response messages
     */
    public boolean isApplicableToResponse() {
        return _applicableToResponse;
    }

    public boolean containsNonIdentifiableCharacters() {
        return _containsNonIdentifiableCharacters;
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
                CodeInjectionProtectionType type = (CodeInjectionProtectionType) target;
                return type.getWspName();
            }

            @Override
            public Object stringToObject(String wspName) throws IllegalArgumentException {
                CodeInjectionProtectionType type = CodeInjectionProtectionType.fromWspName(wspName);
                if (type == null)
                    throw new IllegalArgumentException("Unknown CodeInjectionProtectionType: '" + wspName + "'");
                return type;
            }
        };
    }
}
