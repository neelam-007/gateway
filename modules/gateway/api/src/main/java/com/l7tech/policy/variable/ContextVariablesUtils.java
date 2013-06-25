package com.l7tech.policy.variable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
* A utility class for context variables.
*/
public final class ContextVariablesUtils {

    private static final String REGEX_PREFIX = "(?:\\$\\{)";
    private static final String REGEX_SUFFIX = "(?:\\})";
    private static final Pattern regexPattern = Pattern.compile(REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX);

    /**
     * Get the entire string contained within each set of ${...} in the String s.
     * Variables referenced like ${variablename.mainpart} will be returned as variablename.mainpart.
     * Variables referenced like ${variablename[0]} will be returned as variablename.
     *
     * @param s String to find out what variables are referenced using our syntax of ${}. May be null.
     * @return all the variable names which are contained within our variable reference syntax of ${...} in String s
     */
    public static String[] getReferencedNames(final String s) {
        final List<String> vars = new ArrayList<String>();
        if ( s != null ) {
            final Matcher matcher = regexPattern.matcher(s);
            while (matcher.find()) {
                final int count = matcher.groupCount();
                if (count != 1) {
                    throw new IllegalStateException("Expecting 1 matching group, received: " + count);
                }
                String var = matcher.group(1);
                if (var != null) {
                    vars.add(ContextVariablesUtils.parse(var.trim()));
                }
            }
        }
        return vars.toArray(new String[vars.size()]);
    }

    /**
     * Parses out the variable name for the specified String rawName.
     *
     * @param rawName the raw name. ${variablename}
     * @return the variable name contained within our variable reference syntax of ${...} in String rawName
     */
    private static String parse(String rawName) {
        int ppos = rawName.indexOf("|");
        if (ppos == 0) {
            throw new VariableNameSyntaxException("Variable names must not start with '|'");
        }

        if (ppos > 0) {
            return rawName.substring(0,ppos);
        } else {
            int lbpos = rawName.indexOf("[");
            if (lbpos == 0) {
                throw new VariableNameSyntaxException("Variable names must not start with '['");
            }
            if (lbpos > 0) {
                int rbpos = rawName.indexOf("]", lbpos+1);
                if (rbpos == 0) {
                    throw new VariableNameSyntaxException("Array subscript must not be empty");
                }
                if (rbpos > 0) {
                    String ssub = rawName.substring(lbpos+1, rbpos);
                    int subscript;
                    try {
                        subscript = Integer.parseInt(ssub);
                    } catch (NumberFormatException e) {
                        throw new VariableNameSyntaxException("Array subscript not an integer", e);
                    }
                    if (subscript < 0) {
                        throw new VariableNameSyntaxException("Array subscript must be positive");
                    }
                    return rawName.substring(0, lbpos);
                } else {
                    throw new VariableNameSyntaxException("']' expected but not found");
                }
            } else {
                return rawName;
            }
        }
    }
}