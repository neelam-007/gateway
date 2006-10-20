/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class replaces the variables placeholders in the string that is passed to the
 * {@link ExpandVariables#process} method.
 * The variables placeholders are by default of format <code>${var.name}</code> where
 * <code>var.name</code> is the variable name.
 * The variables are passed in the <code>Map</code> of string key-value pairs. The default
 * variables are passed in constructor and optional overriding variables can be passed in
 * {@link ExpandVariables#process(String, java.util.Map)} method.
 *
 * @author emil
 * @version Apr 8, 2005
 */
public class ExpandVariables {
    private static final Logger logger = Logger.getLogger(ExpandVariables.class.getName());

    public static final String SYNTAX_PREFIX = "${";
    public static final String SYNTAX_SUFFIX = "}";
    
    private static final String REGEX_PREFIX = "(?:\\$\\{)";
    private static final String REGEX_SUFFIX = "(?:\\})";
    private static final Pattern regexPattern = Pattern.compile(REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX);

    public static String[] getReferencedNames(String s) {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        ArrayList vars = new ArrayList();
        Matcher matcher = regexPattern.matcher(s);
        while (matcher.find()) {
            int count = matcher.groupCount();
            if (count != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+count);
            }
            String var = matcher.group(1);
            vars.add(var);
        }
        return (String[])vars.toArray(new String[0]);
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the varaible is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s the input message as a message
     * @param vars the caller supplied varialbes map that is consulted first
     * @return the message with expanded/resolved varialbes
     */
    public static String process(String s, Map vars) {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        Matcher matcher = regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+matchingCount);
            }

            String name = matcher.group(1);
            Object value = vars.get(name);
            String replacement;

            if (value == null) {
                replacement = "";
            } else if (value instanceof String[]) {
                // TODO let user supply delimiter?
                replacement = Arrays.asList((String[])value).toString();
            } else if (value instanceof String) {
                replacement = (String)value;
            } else {
                // TODO typed data anbd interpolation don't mix
                logger.warning("Variable '" + name + "' is a " + value.getClass().getName() + ", not a String; using .toString() instead");
                replacement = value.toString();
            }
            replacement = makeDollarExplicit(replacement); // bugzilla 3022
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String makeDollarExplicit(String in) {
        if (in == null) return null;
        if (in.indexOf('$') < 0) return in;
        return in.replace("$", "\\$");
    }

    private ExpandVariables() {
    }
}