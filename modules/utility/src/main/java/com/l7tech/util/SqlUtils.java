/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 17, 2009
 * Time: 2:00:17 PM
 */
package com.l7tech.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Collection;

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

    /**
     * Read SQL statements from the given reader.
     *
     * <p>The returned statement array will not include the terminating ';'
     * character.</p>
     *
     * <p>Currently C style comments are not supported, only lines beginning
     * with '--' are recognised as comments.</p>
     *
     * @param reader The source for statements.
     * @return The array of statements (may be empty, never null)
     * @throws IOException If an IO error occurs
     */
    public static String[] getStatementsFromReader( final Reader reader ) throws IOException {
        final Collection<String> statements = new ArrayList<String>();

        final StreamTokenizer tokenizer;
        tokenizer = new StreamTokenizer( reader );
        tokenizer.resetSyntax();
        tokenizer.eolIsSignificant(true);
        tokenizer.wordChars(33, 38); // 39 is '
        tokenizer.wordChars(40, 58); // 59 is ;
        tokenizer.wordChars(60, 91); // 92 is \
        tokenizer.wordChars(93,255);

        boolean inString = false;
        boolean escaped = false;

        int token;
        final StringBuilder builder = new StringBuilder();
        while( (token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF ) {
            boolean wasEscaped = escaped;
            escaped = false;

            if ( token == StreamTokenizer.TT_WORD ) {
                builder.append( tokenizer.sval );
            } else if ( token == StreamTokenizer.TT_EOL ) {
                // check for comment line
                if ( isCommentLine(builder) ) {
                    builder.setLength( 0 );
                } else {
                    if ( builder.length() > 0 ) {
                        builder.append( "\n" );
                    }
                }
            } else if ( token == '\t' ) {
                builder.append( (char)token );
            } else if ( token <= ' ' ) {
                builder.append( ' ' );
            } else if ( token == '\'' ) {
                if ( !wasEscaped && !isCommentLine(builder) ) inString = !inString;
                builder.append( (char)token );
            } else if ( token == '\\' ) {
                if (inString) escaped = !wasEscaped;
                builder.append( (char)token );
            } else if ( token == ';' ) {
                if ( !inString && !isCommentLine(builder) ) {
                    final String statement = builder.toString();
                    if ( !statement.trim().isEmpty() ) {
                        statements.add( statement );
                    }
                    builder.setLength( 0 );
                } else {
                    builder.append( (char)token );
                }
            }
        }

        return statements.toArray( new String[statements.size()] );
    }

    /**
     * Parse the value of MySQL variable 'innodb_data_file_path' to determine the
     * max table size.
     *
     * @param innodbData String to parse.
     * @return the max possible size of the database in bytes. If the database cannot have
     * a max size due to being allowed to grow unlimited, then -1 is returned.
     */
    public static long getMaxTableSize(String innodbData) {
        int index = innodbData.lastIndexOf(":autoextend:max:");
        if (index > 0) {
            String max = innodbData.substring(index + 16);
            return getLongSize(max);
        } else if (innodbData.indexOf(":autoextend") > 0) {
            return -1;
        } else {
            // use fixed size(es)
            long max = 0;
            String[] datafiles = innodbData.split(";");
            for (String datafile : datafiles) {
                String[] tokens = datafile.split(":");
                if (tokens.length > 1) {
                    max += getLongSize(tokens[1]);
                }
            }
            return max;
        }
    }

    // - PRIVATE

    private static boolean isCommentLine( final CharSequence charSequence ) {
        return charSequence.length() > 1 && charSequence.charAt( 0 )=='-' && charSequence.charAt( 1 )=='-';
    }

    // gets a long out of a mysql / innodb size specification NNNN[M|G]
    private static long getLongSize(String max) {
        long multiplier = 1L;
        if ("m".equalsIgnoreCase(max.substring(max.length()-1)))
            multiplier = 0x100000L;
        else if ("g".equalsIgnoreCase(max.substring(max.length()-1)))
            multiplier = 0x40000000L;
        return multiplier * Long.parseLong(multiplier > 1 ? max.substring(0, max.length() -1) : max);
    }
}
