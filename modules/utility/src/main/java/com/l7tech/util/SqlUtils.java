/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 17, 2009
 * Time: 2:00:17 PM
 */
package com.l7tech.util;

/**
 * Common place for sql utility functions
 */
public class SqlUtils {

    /**
     * Escape a string for safe use in a sql statement for a mysql database. The following characters are escaped
     * with a backslash in mysql's mysql_real_escape_string method: \x00, \n, \r, \, ', " and \x1a.
     * <p/>
     * This method does the following:<br>
     * \x00 and \x1a are removed<br>
     * \n, \r, ', " and \ are escaped with a backslash<br>
     * <p/>
     * <p/>
     * See http://ca2.php.net/mysql_real_escape_string
     * <p/>
     * mysql_real_escape_string() calls MySQL's library function mysql_real_escape_string,
     * which prepends backslashes to the following characters: \x00, \n, \r, \, ', " and \x1a.
     *
     * @param stringToEscape string to escape
     * @return safe string to use, escaped with a backslash for the following chars: \x00, \n, \r, \, ', " and \x1a
     */
    public static String mySqlEscapeIllegalSqlChars(String stringToEscape) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stringToEscape.length(); i++) {
            char c = stringToEscape.charAt(i);

            switch (c) {
                case '\'':
                    sb.append("\\'");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\r':
                    sb.append("\\\\r");
                    break;
                case '\n':
                    sb.append("\\\\n");
                    break;
                case '\u0000':
                    break;
                case '\u001a':
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
