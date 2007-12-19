/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.common.audit.Audit;
import com.l7tech.common.audit.CommonMessages;
import com.l7tech.server.ServerConfig;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alex
*/
public abstract class Syntax {
    private static final String DEFAULT_DELIMITER = ", ";

    public final String remainingName;
    public static final String SYNTAX_PREFIX = "${";
    public static final String SYNTAX_SUFFIX = "}";
    public static final String REGEX_PREFIX = "(?:\\$\\{)";
    public static final String REGEX_SUFFIX = "(?:\\})";
    public static final Pattern regexPattern = Pattern.compile(REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX);
    public static final Pattern oneVarPattern = Pattern.compile("^" + REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX + "$");

    private Syntax(String name) {
        this.remainingName = name;
    }

    public static void badVariable(String msg, boolean strict, Audit audit) {
        audit.logAndAudit(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, msg);
        if (strict) throw new IllegalArgumentException(msg);
    }

    /**
     * Finds the longest period-delimited subname of the provided name that matches at least one of the known names
     * in the provided Set, or null if no match can be found.
     * @param name the full name to search for
     * @param names the set of names to search within
     * @return the longest subset of the provided Name that matches one of the Set members, or null if no match is found 
     */
    public static String getMatchingName(String name, Set names) {
        final String lname = name.toLowerCase();
        if (names.contains(lname)) return lname;

        int pos = lname.length();
        do {
            String tryname = lname.substring(0, pos);
            if (names.contains(tryname)) return tryname;
            pos = lname.lastIndexOf(".", pos-1);
        } while (pos > 0);

        return null;
    }

    private static interface Formatter {
        String format(Syntax syntax, Object o, Audit audit, boolean strict);
    }

    private static final Set usefulToStringClasses = Collections.unmodifiableSet(new HashSet() {{
        add(String.class);
        add(Number.class);
        add(Long.TYPE);
        add(Integer.TYPE);
        add(Boolean.class);
        add(Boolean.TYPE);
    }});

    public static String[] getReferencedNames(String s) {
        if (s == null) throw new IllegalArgumentException();

        ArrayList vars = new ArrayList();
        Matcher matcher = regexPattern.matcher(s);
        while (matcher.find()) {
            int count = matcher.groupCount();
            if (count != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+count);
            }
            String var = matcher.group(1);
            vars.add(Syntax.parse(var).remainingName);
        }
        return (String[]) vars.toArray(new String[0]);
    }

    // TODO find out how to move this into Syntax subclasses
    public static Syntax parse(String rawName) {
        int ppos = rawName.indexOf("|");
        if (ppos == 0) throw new IllegalArgumentException("Variable names must not start with '|'");
        if (ppos > 0) {
            return new MultivalueDelimiterSyntax(rawName.substring(0,ppos), rawName.substring(ppos+1));
        } else {
            // Can't combine concatenation with subscript (yet -- 2D arrays?)
            int lbpos = rawName.indexOf("[");
            if (lbpos == 0) throw new IllegalArgumentException("Variable names must not start with '['");
            if (lbpos > 0) {
                int rbpos = rawName.indexOf("]", lbpos+1);
                if (rbpos == 0) throw new IllegalArgumentException("Array subscript must not be empty");
                if (rbpos > 0) {
                    String ssub = rawName.substring(lbpos+1, rbpos);
                    int subscript;
                    try {
                        subscript = Integer.parseInt(ssub);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Array subscript not an integer", e);
                    }
                    if (subscript < 0) throw new IllegalArgumentException("Array subscript must be positive");
                    return new MultivalueArraySubscriptSyntax(rawName.substring(0, lbpos), subscript);
                } else throw new IllegalArgumentException("']' expected but not found");
            } else {
                return new MultivalueDelimiterSyntax(rawName, defaultDelimiter());
            }
        }
    }

    private static String defaultDelimiter() {
        String delim = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_TEMPLATE_MULTIVALUE_DELIMITER);
        if (delim != null) return delim;
        return DEFAULT_DELIMITER;
    }

    public static final Formatter DEFAULT_FORMATTER = new Formatter() {
        public String format(Syntax syntax, Object o, Audit audit, boolean strict) {
            boolean ok = false;
            //noinspection ForLoopReplaceableByForEach
            for (Iterator i = usefulToStringClasses.iterator(); i.hasNext();) {
                Class clazz = (Class) i.next();
                if (clazz.isAssignableFrom(o.getClass())) {
                    ok = true;
                    break;
                }
            }

            if (!ok) {
                audit.logAndAudit(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING, syntax.remainingName, o.getClass().getName());
                if (strict) throw new IllegalArgumentException(MessageFormat.format(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING.getMessage(), syntax.remainingName, o.getClass().getName()));
            }
            return o.toString();
        }
    };

    public abstract Object[] filter(Object[] values, Audit audit, boolean strict);
    public abstract String format(Object[] values, Formatter formatter, Audit audit, boolean strict);

    private static class MultivalueDelimiterSyntax extends Syntax {
        private final String delimiter;
        private MultivalueDelimiterSyntax(String name, String delimiter) {
            super(name);
            this.delimiter = delimiter;
        }

        public Object[] filter(Object[] values, Audit audit, boolean strict) {
            return values;
        }

        public String format(final Object[] values, final Formatter formatter, final Audit audit, final boolean strict) {
            if (values == null || values.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value != null) sb.append(formatter.format(this, value, audit, strict));
                if (i < values.length-1) sb.append(delimiter);
            }
            return sb.toString();
        }
    }

    private static class MultivalueArraySubscriptSyntax extends Syntax {
        private final int subscript;
        private MultivalueArraySubscriptSyntax(String name, int subscript) {
            super(name);
            this.subscript = subscript;
        }

        public Object[] filter(Object[] values, Audit audit, boolean strict) {
            if (subscript > values.length-1) {
                audit.logAndAudit(CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE, Integer.toString(subscript), remainingName, Integer.toString(values.length));
                if (strict)
                    throw new IllegalArgumentException(MessageFormat.format(CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE.getMessage(), Integer.toString(subscript), remainingName, Integer.toString(values.length)));
                else
                    return null;
            }
            return new Object[] { values[subscript] };
        }

        public String format(Object[] values, Formatter formatter, Audit audit, boolean strict) {
            if (values == null || values.length != 1) return "";
            return formatter.format(this, values[0], audit, strict);
        }
    }

}
