/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy;

import java.util.Map;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The class replaces the variables placeholders in the string that is passed to the
 * {@link ExpandVariables#process(String)} method.
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
    public static final String DEF_PREFIX = "(?:\\$\\{)";
    public static final String DEF_SUFFIX = "(?:\\})";
    private final Pattern regexPattern;
    private final String variablePrefix;
    private final String variableSuffix;
    private final Map defaultVariables;

    /**
     *  Default Constructor.  Creates the empty default varialbes map.
     */
    public ExpandVariables() {
        this(Collections.EMPTY_MAP);
    }

    /**
     * Constructor accepting the default variables map
     *
     * @param variables the default variables.
     */
    public ExpandVariables(Map variables) {
        if (variables == null) {
            throw new IllegalArgumentException();
        }
        this.defaultVariables = variables;
        regexPattern = Pattern.compile(DEF_PREFIX+"(.+?)"+DEF_SUFFIX);
        variablePrefix = DEF_PREFIX;
        variableSuffix = DEF_SUFFIX;
    }

    /**
     * Process the input string and expand the variables using the
     * default variables map in this class.
     *
     * @param s the input message as a message
     * @return the message with expanded/resolved varialbes
     * @throws VariableNotFoundException if the varialbe
     */
    public String process(String s) throws VariableNotFoundException {
        return process(s, Collections.EMPTY_MAP);
    }


    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the varaible is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s the input message as a message
     * @param userVariables the caller supplied varialbes map that is consulted first
     * @return the message with expanded/resolved varialbes
     * @throws VariableNotFoundException if the varialbe
     */
    public String process(String s, Map userVariables) throws VariableNotFoundException {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        Matcher matcher = regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 3 matching groups received: "+matchingCount);
            }
            String var = matcher.group(1);
            String replacement = (String)userVariables.get(var);
            if (replacement == null) {
                replacement = (String)defaultVariables.get(var);
            }
            if (replacement == null) {
                throw new VariableNotFoundException(var);
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Signals that the Exception could not be found
     */
    public static class VariableNotFoundException extends Exception {
        private String variable;
        /**
         * @param   variable   the variable that was not found and caused the exception
         */
        VariableNotFoundException(String variable) {
            super("The variable '"+variable+"' not found");
            this.variable = variable;
        }

        /**
         * @return the variable name that was not found
         */
        public String getVariable() {
            return variable;
        }
    }

}