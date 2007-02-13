/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.common.util.EnumTranslator;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Enum for types of code injection protection (i.e., types of attack to protect against).
 *
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionType implements Serializable {
    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal ++;

    private final String _name;

    private final String _description;

    /** Regular expression pattern to detect code injection. */
    private final Pattern _pattern;

    private final boolean _applicableToResponse;

    /** Map for looking up instance by name.
        The keys are of type String. The values are of type {@link HtmlFormDataLocation}. */
    private static final Map _byName = new HashMap();

    public static final CodeInjectionProtectionType HTML_JAVASCRIPT = new CodeInjectionProtectionType(
            "HTML/JavaScript injection (Cross Site Scripting)",
            "Blocks messages which appear to contain JavaScript by scanning for <script> tag.",
            Pattern.compile("<\\s*script", Pattern.CASE_INSENSITIVE),
            true);
    public static final CodeInjectionProtectionType PHP_EVAL_INJECTION = new CodeInjectionProtectionType(
            "PHP eval injection",
            "Blocks messages which contains metacharacters that can be used to inject PHP code into a PHP eval statement. These metacharacters are ';\"\\.",
            Pattern.compile("[';\"\\\\]"),
            false);
    public static final CodeInjectionProtectionType SHELL_INJECTION = new CodeInjectionProtectionType(
            "Shell injection",
            "Blocks messages which contains metacharacters that can be used to inject shell script into a system call statement.  These metacharacters are `;|&>\\.",
            Pattern.compile("[`;|&>\\\\]"),
            false);

    private static final CodeInjectionProtectionType[] _values = new CodeInjectionProtectionType[]{HTML_JAVASCRIPT, PHP_EVAL_INJECTION, SHELL_INJECTION};

    public static CodeInjectionProtectionType[] values() {
        return (CodeInjectionProtectionType[])_values.clone();
    }

    /**
     * Returns the enum constant of this type with the specified name.
     *
     * @param name  name of enum constant
     * @return the enum constant with the specified name; <code>null</code> if
     *         this enum type has no constant with the specified name
     */
    public static CodeInjectionProtectionType valueOf(final String name) {
        return (CodeInjectionProtectionType)_byName.get(name);
    }

    private CodeInjectionProtectionType(final String name,
                                        final String description,
                                        final Pattern pattern,
                                        final boolean applicableToResponse) {
        _name = name;
        _description = description;
        _pattern = pattern;
        _applicableToResponse = applicableToResponse;
        //noinspection unchecked
        _byName.put(name, this);
    }

    public String getName() {
        return _name;
    }

    public String getDescription() {
        return _description;
    }

    /** @return the regular expression pattern used to detect code injection */
    public Pattern getPattern() {
        return _pattern;
    }

    public boolean isApplicableToResponse() {
        return _applicableToResponse;
    }

    public String toString() {
        return _name;
    }

    protected Object readResolve() throws ObjectStreamException {
        return _values[_ordinal];
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public String objectToString(Object target) {
                CodeInjectionProtectionType type = (CodeInjectionProtectionType)target;
                return type.toString();
            }

            public Object stringToObject(String name) throws IllegalArgumentException {
                CodeInjectionProtectionType type = CodeInjectionProtectionType.valueOf(name);
                if (type == null) throw new IllegalArgumentException("Unknown CodeInjectionProtectionType name: '" + name + "'");
                return type;
            }
        };
    }
}
