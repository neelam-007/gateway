package com.l7tech.gui.util;

/**
 * HTML utlities
 *
 * User: dlee
 * Date: Mar 11, 2009
 */
public class HtmlUtil {

    /**
     * Escapes HTML characters.
     *
     * @param stringToEscape    String to escape HTML characters
     * @return                  The escaped string
     */
    public static String escapeHtmlCharacters(String stringToEscape) {
        stringToEscape = stringToEscape.replaceAll("&", "&amp;");
        stringToEscape = stringToEscape.replaceAll("<", "&lt;");
        return stringToEscape.replaceAll(">", "&gt;");
    }

}
