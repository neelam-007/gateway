/**
 * The class is a utility class to validate variable prefix.
 */
package com.l7tech.console.util;

import java.util.regex.Pattern;

/**
 * @auther: ghuang
 */
public final class VariablePrefixUtil {

    //- PUBLIC

    /**
     * Fix a variable name by trimming space and removing any surrounding ${...} or leading $
     *
     * @param variableName The name to fix.
     * @return The fixed name.
     */
    public static String fixVariableName( final String variableName ) {
        String varname = variableName;
        if ( varname != null ) {
            varname = varname.trim();
            varname = FIX_START.matcher(varname).replaceAll("");
            varname = FIX_END.matcher(varname).replaceAll("");
        }
        return varname;
    }

    public static boolean hasDollarOrCurlyOpen(final String variableName) {
        boolean hasDollarOrCurlyOpen = false;
        if (variableName !=  null && variableName.length() > 0) {
            hasDollarOrCurlyOpen = variableName.indexOf('$') >= 0 || variableName.indexOf('{') >= 0;
        }
        return hasDollarOrCurlyOpen;
    }

    public static boolean hasValidDollarCurlyOpenStart(final String variableName) {
        boolean hasValidDollarCurlyOpenStart = false;
        if (variableName !=  null && variableName.length() > 0) {
            String varNameNoWhiteSpaces = variableName.replaceAll("\\s*", "");

            // valid if name starts with only one occurrence of ${
            // and if there's no $ or { in the rest of the name
            hasValidDollarCurlyOpenStart = varNameNoWhiteSpaces.startsWith("${") && !varNameNoWhiteSpaces.startsWith("${", 2)
                    && varNameNoWhiteSpaces.indexOf('$', 2) < 2 && varNameNoWhiteSpaces.indexOf('{', 2) < 2;
        }
        return hasValidDollarCurlyOpenStart;
    }

    public static boolean hasValidCurlyCloseEnd(final String variableName) {
        boolean hasValidCurlyCloseEnd = false;
        if (variableName !=  null && variableName.length() > 0) {
            String varNameNoWhiteSpaces = variableName.replaceAll("\\s*", "");

            // valid if name ends with only one occurrence of }
            hasValidCurlyCloseEnd = varNameNoWhiteSpaces.indexOf('}') == varNameNoWhiteSpaces.length() - 1 ;
        }
        return hasValidCurlyCloseEnd;
    }

    //- PRIVATE

    private static final Pattern FIX_START = Pattern.compile("\\s*(?:\\$\\{?)?\\s*");
    private static final Pattern FIX_END = Pattern.compile("\\s*(?:\\})?\\s*");
}
