/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    /**
     * Check if the specified string is matched by at least one regexp.
     *
     * @param string  the string to match.  If null, this method always fails.
     * @param patterns  the patterns to check against.  If null or empty, this method always fails.
     * @return true if and only if the provided string was matched by at least one pattern.
     */
    public static boolean matchesAny(String string, Pattern[] patterns) {
        for (int i = 0; i < patterns.length; i++)
            if (patterns[i].matcher(string).matches())
                return true;
        return false;
    }

    /**
     * breaks a string in multiple lines based on the max length of each line
     */
    public static String breakOnMultipleLines(String input, int maxlinelength) {
        input = input.trim();
        if (input.length() <= maxlinelength) return input;
        StringBuffer output = new StringBuffer();
        int pos = 0;
        while ((input.length() - pos) > maxlinelength) {
            int tmp = input.indexOf(' ', pos);
            if (tmp < 0) break;
            int lastspace = tmp;
            while (true) if ((lastspace - pos) > maxlinelength) {
                output.append(input.substring(pos, lastspace)).append("\n");
                pos = lastspace + 1;
                break;
            } else if (tmp < 0 && lastspace > 0) {
                output.append(input.substring(pos, lastspace)).append("\n");
                pos = lastspace + 1;
                break;
            } else if ((tmp - pos) == maxlinelength) {
                output.append(input.substring(pos, tmp)).append("\n");
                pos = tmp + 1;
                break;
            } else if ((tmp - pos) > maxlinelength && (lastspace - pos) < maxlinelength) {
                output.append(input.substring(pos, lastspace)).append("\n");
                pos = lastspace + 1;
                break;
            } else {
                lastspace = tmp;
                tmp = input.indexOf(' ', tmp + 1);
            }
        }
        output.append(input.substring(pos));
        return output.toString();
    }

    /**
     * Check if the given pattern matches the given text.
     *
     * <p>This uses a simplified regex format where '*' matches anything.</p>
     *
     * @param pattern The pattern (my contain '*', must not be null)
     * @param text The text to match (mut not be null)
     * @param caseSensitive True for a case sensitive match
     * @param fullMatch True is the pattern must match the string (else just the start)
     * @return True if the pattern matches the text
     * @throws IllegalArgumentException if pattern or text is null
     */
    public static boolean matches(String pattern, String text, boolean caseSensitive, boolean fullMatch) {
        if (pattern==null) throw new IllegalArgumentException("pattern must not be null");
        if (text==null) throw new IllegalArgumentException("text must not be null");

        boolean match = false;

        if ( !caseSensitive ) {
            pattern = pattern.toLowerCase();
            text = text.toLowerCase();
        }

        pattern = pattern.replaceAll("\\\\", "\\\\\\\\");
        pattern = pattern.replaceAll("\\{", "\\\\\\{");
        pattern = pattern.replaceAll("\\}", "\\\\\\}");
        pattern = pattern.replaceAll("\\[", "\\\\\\[");
        pattern = pattern.replaceAll("\\]", "\\\\\\]");
        pattern = pattern.replaceAll("\\(", "\\\\\\(");
        pattern = pattern.replaceAll("\\)", "\\\\\\)");
        pattern = pattern.replaceAll("\\.", "\\\\\\.");
        pattern = pattern.replaceAll("\\|", "\\\\\\|");
        pattern = pattern.replaceAll("\\^", "\\\\\\^");
        pattern = pattern.replaceAll("\\$", "\\\\\\$");
        pattern = pattern.replaceAll("\\?", "\\\\\\?");
        pattern = pattern.replaceAll("\\*", ".*");
        pattern = "^" + pattern;

        Matcher matcher = Pattern.compile(pattern).matcher(text);

        if ( fullMatch ) {
            match = matcher.matches();
        } else {
            match = matcher.find();
        }

        return match;
    }

    /**
     * Extracts trailing lines of text. This will handle Unix, Mac and Windows
     * line endings. Any trailing line ending is trimmed from the result and
     * is not counted as one line.
     *
     * @param text      multi-line text
     * @param numLines  maximum number of trailing lines to extract
     * @return          at most <tt>numLines</tt> lines of text with any trailing
     *                  line end character(s) trimmed
     * @since SecureSpan 4.3
     */
    public static String tail(String text, int numLines) {
        if (numLines <= 0)
            throw new IllegalArgumentException("number of lines must be greater than zero");

        int lines = 0;
        int i = text.length() - 1;
        boolean wasLF = false;

        int numLineEndCharsAtEnd = 0;
        if (text.endsWith("\r\n")) {
            numLineEndCharsAtEnd = 2;
            i -= 2;
        } else if (text.endsWith("\r") || text.endsWith("\n")) {
            numLineEndCharsAtEnd = 1;
            --i;
        }

        for (; i >= 0; --i) {
            final char c = text.charAt(i);
            if (c == '\n') {
                ++ lines;
                if (lines == numLines)
                    break;
                wasLF = true;
            } else {
                if (c == '\r') {
                    if (! wasLF) {
                        ++ lines;
                        if (lines == numLines)
                            break;
                    }
                }
                wasLF = false;
            }
        }

        if (i < 0) {
            if (numLineEndCharsAtEnd == 0) {
                return text;
            } else {
                return text.substring(0, text.length() - numLineEndCharsAtEnd);
            }
        } else {
            if (numLineEndCharsAtEnd == 0) {
                return text.substring(i + 1);
            } else {
                return text.substring(i + 1, text.length() - numLineEndCharsAtEnd);
            }
        }
    }

    /**
     * Convert all line breaks in a string to the same kind of line break.
     * This method uses a single pass processing and will not create any new string
     * unless absolutely necessary.
     *
     * @param text          string with possible line breaks
     * @param lineBreak     must be one of: "\r", "\n", or "\r\n"
     * @return converted string; <code>text</code> is returned if no conversion needed
     * @since SecureSpan 4.3
     */
    public static String convertLineBreaks(final String text, final String lineBreak) {
        if (text == null || text.length() == 0) return text;

        final boolean toCR = "\r".equals(lineBreak);
        final boolean toLF = "\n".equals(lineBreak);
        final boolean toCRLF = "\r\n".equals(lineBreak);
        if (!(toCR || toLF || toCRLF)) throw new IllegalArgumentException("lineBreak not one of \"\\r\", \"\\n\", or \"\\r\\n\"");

        StringBuilder converted = null; // Delays construction until absolutely necessary.
        boolean wasCR = false;
        final int sLength = text.length();
        for (int i = 0; i < sLength; ++i) {
            final char c = text.charAt(i);
            if (c == '\r') { // --------------------- strict CR or CR in a CR-LF
                // Delays handling of this CR until we see the next character.
                wasCR = true;
            } else if (c == '\n') {
                if (wasCR) { // ---------------------------------- LF in a CR-LF
                    if (toCRLF) {
                        // pass through
                        if (converted != null) converted.append('\r').append(c);
                    } else {
                        // convert
                        if (converted == null) converted = new StringBuilder().append(text, 0, i - 1);
                        converted.append(lineBreak);
                    }
                } else { // ------------------------------------------ strict LF
                    if (toLF) {
                        // pass through
                        if (converted != null) converted.append(c);
                    } else {
                        // convert
                        if (converted == null) converted = new StringBuilder().append(text, 0, i);
                        converted.append(lineBreak);
                    }
                }
                wasCR = false;
            } else { // --------------------------------------------- other char
                // First, handles any previous strict CR.
                if (wasCR) {
                    if (toCR) {
                        // pass through
                        if (converted != null) converted.append('\r');
                    } else {
                        // convert
                        if (converted == null) converted = new StringBuilder().append(text, 0, i - 1);
                        converted.append(lineBreak);
                    }
                }

                // Second, passes through this character.
                if (converted != null) {
                    converted.append(c);
                }
                wasCR = false;
            }
        }

        // Handles any trailing CR.
        if (wasCR) {
            if (toCR) {
                if (converted != null) converted.append('\r');
            } else {
                if (converted == null) converted = new StringBuilder().append(text, 0, text.length() - 1);
                converted.append(lineBreak);
            }
        }

        return converted == null? text : converted.toString();
    }
}
