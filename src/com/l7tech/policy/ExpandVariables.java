/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The class
 *
 * @author emil
 * @version Apr 8, 2005
 */
public class ExpandVariables {
    public static final String DEF_PREFIX = "(\\$\\{)";
    public static final String DEF_SUFFIX = "(\\})";
    private final Pattern regexPattern;
    private final String variablePrefix;
    private final String variableSuffix;
    private final Map variables;


    /**
     * Constructor accepting the
     * @param variables
     */
    public ExpandVariables(Map variables) {
        if (variables == null) {
            throw new IllegalArgumentException();
        }
        this.variables = variables;
        regexPattern = Pattern.compile(DEF_PREFIX+"(.+?)"+DEF_SUFFIX);
        variablePrefix = DEF_PREFIX;
        variableSuffix = DEF_SUFFIX;
    }

    /**
     * Process the input string and expand the variables that were
     * found into it
     *
     * @param s the input message as a message
     * @return the message with expanded/resolved varialbes
     * @throws VariableNotFoundException if the varialbe
     */
    public String process(String s) throws VariableNotFoundException {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        Matcher matcher = regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 3) {
                throw new IllegalStateException("Expecting 3 matching groups received: "+matchingCount);
            }
            String var = matcher.group(2);
            String replacement = (String)variables.get(var);
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
    static class VariableNotFoundException extends Exception {
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