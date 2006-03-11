/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

import java.util.StringTokenizer;

/**
 * Utilities for text mode programs.
 */
public class TextUtils {
    /**
     * Pad the specified string to the specified width by appending spaces.
     * If the input string is already at least as long as the desired width it will
     * be returned unmodified.
     *
     * @param str    The string to pad.  null is considered to be the same as the empty string.
     * @param width  the width to pad the string.  Must be nonnegative.
     * @return the padded string.  Never null.
     */
    public static String pad(String str, int width) {
        assert width >= 0;
        if (str == null) str = "";
        StringBuffer sb = new StringBuffer(str);
        for (int i = str.length(); i < width; ++i)
            sb.append(' ');
        return sb.toString();
    }

    /**
     * Pluralize the specified regular English noun.
     * This method simply appends "s" to noun if the count is any value other than 1.
     *
     * @param count  how many nouns there are.
     * @param noun   the noun to pluralize.  Shouldn't be null or empty.
     * @return the pluralized noun.  Never null.
     */
    public static String plural(long count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    /** Insert carriage returns into the given string before every columns characters, preferring to insert them before new words begin. */
    public static String wrapString(String in, int maxcol, int maxlines, String wrapSequence) {
        if (in == null) return "null";
        StringBuffer out = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(in);
        int col = 0;
        int line = 0;
        while (tok.hasMoreTokens()) {
            String next = tok.nextToken();
            int len = next.length();
            if (col + len >= (maxcol - 1)) {
                out.append(wrapSequence);
                col = 0;
                line++;
                if (line > maxlines)
                    return out.toString();

            }
            out.append(next);
            col += len;
            if (tok.hasMoreTokens()) {
                out.append(" ");
                col++;
            }
        }
        return out.toString();
    }
}
