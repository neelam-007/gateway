package com.l7tech.util;

import com.l7tech.util.Functions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.CollectionUtils.toList;
import static com.l7tech.util.Functions.*;

/**
 * Utilities for text mode programs.
 */
public class TextUtils {
    /**
     * String split pattern for URIs which splits on white space [ \t\n\x0B\f\r]. None of these values are
     * valid in a URI, so can safely be used to split them.
     */
    public final static Pattern URI_STRING_SPLIT_PATTERN = Pattern.compile("\\s+");

    /** Number of characters in front of the pattern being searched for to log when detected. */
    private static final int EVIDENCE_MARGIN_BEFORE = 16;

    /** Number of characters behind the pattern being searched for to log when detected. */
    private static final int EVIDENCE_MARGIN_AFTER = 24;

    /**
     * String split pattern for comma separated values.
     *
     * <p>Splits on a comma with optional preceding and trailing whitespace [ \t\n\x0B\f\r]</p>
     *
     * <p>Note that this allows multiple lines.</p>
     */
    public final static Pattern CSV_STRING_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");

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
        StringBuilder sb = new StringBuilder( str );
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
        StringBuilder out = new StringBuilder();
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
        for (Pattern pattern : patterns)
            if (pattern.matcher(string).matches())
                return true;
        return false;
    }

    /**
     * breaks a string in multiple lines based on the max length of each line
     * @param input String to break up into multiple lines
     * @param maxlinelength int max line length. Note this will not break continuous lines. A space must be found to
     * break a line.
     */
    public static String breakOnMultipleLines(String input, int maxlinelength) {
        return breakOnMultipleLines(input, maxlinelength, "\n");
    }

    /**
     * breaks a string in multiple lines based on the max length of each line
     * @param input String to break up into multiple lines
     * @param maxlinelength int max line length. Note this will not break continuous lines. A space must be found to
     * break a line.
     * @param breakCharacters String to insert when ever a line should be broken
     */
    public static String breakOnMultipleLines(String input, int maxlinelength, String breakCharacters) {
        input = input.trim();
        if (input.length() <= maxlinelength) return input;
        StringBuilder output = new StringBuilder();
        int pos = 0;
        while ((input.length() - pos) > maxlinelength) {
            int tmp = input.indexOf(' ', pos);
            if (tmp < 0) break;
            int lastspace = tmp;
            while (true) if ((lastspace - pos) > maxlinelength) {
                output.append(input.substring(pos, lastspace)).append(breakCharacters);
                pos = lastspace + 1;
                break;
            } else if (tmp < 0 && lastspace > 0) {
                output.append(input.substring(pos, lastspace)).append(breakCharacters);
                pos = lastspace + 1;
                break;
            } else if ((tmp - pos) == maxlinelength) {
                output.append(input.substring(pos, tmp)).append(breakCharacters);
                pos = tmp + 1;
                break;
            } else if ((tmp - pos) > maxlinelength && (lastspace - pos) < maxlinelength) {
                output.append(input.substring(pos, lastspace)).append(breakCharacters);
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
     * Enforce to break a string into multiple lines.  Any long strings without spaces will be nicely formatted into multiple
     * lines.  Note: in this method, new line characters will be considered first (See the for loop.)
     *
     * @param input String to break up into multiple lines
     * @param maxLineLength int max line length.
     * @param breakCharacters String to insert when ever a line should be broken.
     * @param escapeHtml: a flag to indicate whether to replace html special characters (such as <, >, /, &, ", ', etc) with html codes.
     *               Note: we do not apply escaping html on breakCharacters, because it might contain some html special characters.  For example, breakCharacters are "<br>", a break line.
     * @return a well-formatted string with multiple lines.
     */
    public static String enforceToBreakOnMultipleLines(String input, int maxLineLength, String breakCharacters, boolean escapeHtml) {
        StringBuilder strBuilder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(input));

        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.length() <= maxLineLength) {
                    if (escapeHtml) line = escapeHtmlSpecialCharacters(line);
                    // Note: we do not apply escaping html on breakCharacters, because it might contain some html special characters.
                    // For example, breakCharacters are "<br>", a break line.  This rule will be applied below as well.
                    strBuilder.append(line).append(breakCharacters);
                } else {
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    StringBuilder buff = new StringBuilder(0);
                    String token;

                    while (tokens.hasMoreTokens()) {
                        token = tokens.nextToken();

                        if (buff.length() + token.length() <= maxLineLength) { // Continue to accept more tokens if possible
                            buff.append(token).append(" ");
                        } else if (token.length() <= maxLineLength) { // Flush buff and the current token will be in next line.
                            String outputLine = buff.toString();  // The current buff will be output.
                            buff.setLength(0); // Clean buff
                            buff.append(token).append(" "); // Add the current token into buff

                            if (escapeHtml) outputLine = escapeHtmlSpecialCharacters(outputLine);
                            strBuilder.append(outputLine).append(breakCharacters);
                        } else { // If the toke is too long (i.e., greater than maxLineLength), then we enforce to break the current token and put the first trunk of the token into the buff.
                            String outputLine = buff.toString() + token.subSequence(0, maxLineLength - buff.length());
                            token = token.substring(maxLineLength - buff.length());

                            if (escapeHtml) outputLine = escapeHtmlSpecialCharacters(outputLine);
                            strBuilder.append(outputLine).append(breakCharacters);

                            while (token.length() > maxLineLength) {
                                outputLine = token.substring(0, maxLineLength);
                                token = token.substring(maxLineLength);

                                if (escapeHtml) outputLine = escapeHtmlSpecialCharacters(outputLine);
                                strBuilder.append(outputLine).append(breakCharacters);
                            }

                            buff.setLength(0);
                            buff.append(token).append(" ");
                        }
                    }

                    if (buff.length() > 0) {
                        String outputLine = buff.toString().trim();
                        if (escapeHtml) outputLine = escapeHtmlSpecialCharacters(outputLine);

                        strBuilder.append(outputLine).append(breakCharacters);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read a line from a string input, '" + input + "'", e);
        }

        return strBuilder.toString();
    }

    /**
     * Escape some special HTML characters in a given text.  So far, these characters are &, <, >, /, ", and '.
     * @param text: a string to be processed.
     * @return a string escaping HTML.
     */
    public static String escapeHtmlSpecialCharacters(String text) {
        if (text == null) throw new IllegalArgumentException("The text for escaping HTML must not be null.");

        // Note: replacing '&' must happen first before other chars replacement.
        // Otherwise, some '&'s with special meaning (such as in "&lt;") will be incorrectly replaced.
        text = text.replaceAll("&", "&amp;");
        text = text.replaceAll("<", "&lt;");
        text = text.replaceAll(">", "&gt;");
        text = text.replaceAll("/", "&#47;");  // "&frasl;" and forward slash have different unicode ("&frasl;" is unicode U+2044 whereas forward slash is U+002F).
        text = text.replaceAll("\"", "&quot;");
        text = text.replaceAll("\'", "&#39;");

        return text;
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

        boolean match;

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
     * Converts a simple glob pattern match, as used by MS-DOS and Unix shells, into a compiled
     * regular expression that matches the same filenames.
     *
     * @param glob a filename that may contain an asterisk to match any sequence of characters or a question
     *             mark to match a single character.  Required.
     * @return a ready-to-compile regular expression that will match the specified glob when tested with .matches().
     */
    public static String globToRegex(CharSequence glob) {
        StringBuilder sb = new StringBuilder(glob.length() * 3 / 2);
        int len = glob.length();
        for (int i = 0; i < len; ++i) {
            char c = glob.charAt(i);
            switch (c) {
            case '*':
                sb.append(".*");
                break;
            case '?':
                sb.append('.');
                break;
            default:
                if (!Character.isLetterOrDigit(c))
                    sb.append('\\');
                sb.append(c);
                break;
            }
        }
        return sb.toString();
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

    /**
     * Join the specified array of CharSequence into a single StringBuffer, joined with the specified delimiter.
     * @param start   a StringBuffer containing the starting text.  If null, a new StringBuffer will be created.
     * @param delim   delimiter to join with.  may not be null.
     * @param tojoin  array of eg. String to join.  May be null or empty.
     * @return start, after having XdYdZ appended to it where X, Y and Z were memebers of tojoin and d is delim.
     *         Returns a StringBuffer containing the empty string if tojoin is null or empty.
     */
    public static StringBuffer join(StringBuffer start, String delim, CharSequence[] tojoin) {
        if (start == null)
            start = new StringBuffer();
        if (tojoin == null)
            return start;
        for (int i = 0; i < tojoin.length; i++) {
            if (i > 0)
                start.append(delim);
            start.append(tojoin[i]);
        }
        return start;
    }

    /**
     * Join the specified array of CharSequence into a single StringBuffer, joined with the specified delimiter.
     * @param delim   delimiter to join with.  may not be null.
     * @param tojoin  array of eg. String to join. may be null or empty.
     * @return a new StringBuffer containing XdYdZ where X, Y and Z were members of tojoin and d is delim.  Returns
     *         a StringBuffer containing the empty string if tojoin is null or empty.
     */
    public static StringBuffer join(String delim, CharSequence... tojoin) {
        return join(null, delim, tojoin);
    }

    /**
     * Join the specified array of objects toString() representations into a single StringBuffer, joined with
     * the specified delimiter.
     *
     * @param delim   delimiter to join with.  may not be null.
     * @param tojoin  array of Objects whose default toString representations to join. may be null or empty.
     * @return a new StringBuffer containing XdYdZ where X, Y and Z were members of tojoin and d is delim.  Returns
     *         a StringBuffer containing the empty string if tojoin is null or empty.
     */
    public static StringBuffer join(String delim, Collection tojoin) {
        CharSequence[] seqs = new CharSequence[tojoin == null ? 0 : tojoin.size()];
        if (tojoin != null) {
            int idx = 0;
            for (Object o : tojoin)
                seqs[idx++] = o == null ? "" : o.toString();
        }
        return join(null, delim, seqs);
    }

    /**
     * if the passed string is not null and is longer than the max passed, then it will be truncated
     * in the middle with "..." in place of the truncated portion. the returning string has a length
     * of max or a little shorter. if the incoming string does not exceed desired max or is null, it
     * will simply be returned as is (untouched)
     * @return truncated string
     * @param s string to trim
     * @param max lenght beyond which the string is trimmed
     */
    public static String truncStringMiddle(String s, int max) {
        if (s != null && s.length() > max) {
            s = s.substring(0, ((max/2) - 3)) + "..." + s.substring(s.length() - ((max/2) - 3));
        }
        return s;
    }

    public static String truncateBelowActualScreenSize(final FontMetrics fontMetrics, final String s, final int maxSize) {
        final int width = SwingUtilities.computeStringWidth(fontMetrics, s);

        if (width <= maxSize) {
            return s;
        }

        final int maxCharSizeToRemove = width - maxSize; // need to remove this much width from String s
        // Use a wide character
        final int wideChar = SwingUtilities.computeStringWidth(fontMetrics, "w");
        final int howManyCharsToRemove = maxCharSizeToRemove / wideChar; // how many chars to remove from end?

        return truncateStringAtEnd(s, s.length() - howManyCharsToRemove);
    }

    /**
     * Same idea as truncStringMiddle, except that the string is guaranteed to be the length of sizeOfReturnString,
     * except when s is shorter than sizeOfReturnString, or is less than 5 char's, in which cases, s is returned.
     * The return string has '...' in the middle.
     * @param s return string should be a truncated representation of s
     * @param sizeOfReturnString the size the returned string should be
     * @return a truncated string, whose size is equal to sizeOfReturnString, which has '...' representing all characters
     * which were left out for display purposes
     */
    public static String truncStringMiddleExact(String s, int sizeOfReturnString){

        if (s == null || s.length() <= sizeOfReturnString) return s;

        //5 is used as we will have a start, end and 3 '.'s in the middle
        if(s.length() < 5 || sizeOfReturnString < 5){
            return s;
        }

        char [] strChar = new char[sizeOfReturnString];

        int middle = (strChar.length / 2);
        boolean pastMiddle = false;
        for(int i = 0; i < strChar.length; i++){

            if(i >= middle-1 && i <= middle+1){
                strChar[i] = '.';
                pastMiddle = true;
                continue;
            }

            if(pastMiddle){
                int diff = s.length() - (sizeOfReturnString - i);
                strChar[i] = s.charAt(diff);
                continue;
            }
            
            strChar[i] = s.charAt(i);
        }

        return new String(strChar); 
    }

    /**
     * Truncate the string at the end by adding '...' characters.
     *
     * @param s String to truncate
     * @param maxSize Maximum size, ignore if less than 4
     * @return truncated String if the length of s is > than maxSize with '...' added if it was truncated,
     * otherwise s is returned.
     */
    public static String truncateStringAtEnd(String s, int maxSize){

        if (s == null || s.length() <= maxSize) return s;

        //4 is used as we will have a start and 3 '.'s at the end
        if (s.length() < 4 || maxSize < 4) {
            return s;
        }

        char [] strChar = new char[maxSize - 3];
        for (int i = 0; i < strChar.length; i++) {
            strChar[i] = s.charAt(i);
        }

        return new String(strChar) + "...";
    }

    /**
     * Convert the given object to a string allowing for a null value.
     *
     * @param object The object to toString (may be null)
     * @return The string representation of the object
     * @see Object#toString
     */
    public static String toString( final Object object ) {
        return toString( object, "" );
    }

    /**
     * Convert the given object to a string allowing for a null value.
     *
     * @param object The object to toString (may be null)
     * @param defaultValue The value to use in case of null 
     * @return The string representation of the object
     * @see Object#toString
     */
    public static String toString( final Object object, final String defaultValue ) {
        String value = defaultValue;

        if ( object != null ) {
            value = object.toString();
        }

        return value;
    }

    /**
     * Scans for a string pattern.
     *
     * @param s         string to scan
     * @param pattern   regular expression pattern to search for
     * @param evidence  for passing back snippet of string surrounding the first
     *                  (if found) match, for logging purpose
     * @return starting character index if found (<code>evidence</code> is then populated); -1 if not found
     */
    public static int scanAndRecordMatch(final String s, final Pattern pattern, final StringBuilder evidence) {
        final Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            evidence.setLength(0);

            int start = matcher.start() - EVIDENCE_MARGIN_BEFORE;
            if (start <= 0) {
                start = 0;
            } else {
                evidence.append("...");
            }

            int end = matcher.end() + EVIDENCE_MARGIN_AFTER;
            if (end >= s.length()) {
                end = s.length();
                evidence.append(s.substring(start, end));
            } else {
                evidence.append(s.substring(start, end));
                evidence.append("...");
            }

            return matcher.start();
        } else {
            return -1;
        }
    }

    /**
     * Create a String for the given characters.
     *
     * @param characters The charsacters to use (may be null)
     * @return The new String (never null)
     */
    public static String string( final char[] characters ) {
        return characters == null ? "" : new String(characters);
    }

    /**
     * Trim the given string allowing for a null value.
     *
     * @param text The text to trim (may be null)
     * @return the trimmed text, never null
     */
    public static String trim( final String text ) {
        return text==null ? "" : text.trim();
    }

    /**
     * Split a string into Non-Empty values using the given pattern.
     *
     * <P>Individual values will be trimmed and empty elements will be
     * removed from the resulting list.</P>
     *
     * @param splitPattern The pattern to use (Required)
     * @param text The text to split (May be null, but callers should not knowingly pass null)
     * @return The immutable list of non-empty values
     */
    public static List<String> splitNE( final Pattern splitPattern,
                                        final String text ) {
        return toList( grep( map( list( splitPattern.split( text ) ), trim() ), isNotEmpty() ) );
    }

    /**
     * Modify the StringBuilder replacing any ignorable characters with their unicode value.
     *
     * @param builder string builder to update
     */
    public static void makeIgnorableCharactersViewableAsUnicode(StringBuilder builder) {
        for(int i = 0; i < builder.length(); i++){
            final char c = builder.charAt(i);
            if(Character.isIdentifierIgnorable(c)){
                //%1$04d - %1$ = argument_index, 0 = flags - pad with leading zeros, 4 = width, d = conversion
                builder.replace(i, i+ 1, "\\u" + String.format("%1$04d", (int)c));
            }
        }
    }

    /**
     * First class trim.
     *
     * @return A trim function.
     */
    public static Unary<String,CharSequence> trim() {
        return FUNC_TRIM;
    }

    /**
     * First class lower case conversion.
     *
     * @return A to lower case function.
     */
    public static Unary<String,CharSequence> lower() {
        return FUNC_LOWER;
    }

    /**
     * First class upper case conversion.
     *
     * @return A to upper case function.
     */
    public static Unary<String,CharSequence> upper() {
        return FUNC_UPPER;
    }

    /**
     * First class isEmpty
     *
     * @return An isEmpty function
     */
    public static Unary<Boolean,CharSequence> isEmpty() {
        return FUNC_IS_EMPTY;
    }

    /**
     * Function that tests if a character sequence is not empty.
     *
     * @return An inverted isEmpty function
     */
    public static Unary<Boolean,CharSequence> isNotEmpty() {
        return FUNC_IS_NOT_EMPTY;
    }

    /**
     * Function that splits a string using the given pattern.
     *
     * @param pattern The pattern to use
     * @return The splitter function
     */
    @NotNull
    public static Unary<String[],String> split( @NotNull final Pattern pattern ) {
        return new Unary<String[],String>(){
            @Override
            public String[] call( final String text ) {
                return pattern.split( text );
            }
        };
    }

    /**
     * Get a list of objects obtained by splitting the propValue string and applying the transformation for each non
     * empty value. Null values returned from the transformation are ignored.
     *
     * @param propValue      string value to split. May be null when a property has no value.
     * @param splitPattern   pattern to split propValue on
     * @param transformation function to call for each non empty value. Must return null if a value should be ignored.
     * @param <O>            type of objects transformed from split string
     * @return list of transformed objects. Never null. May be empty.
     */
    @NotNull
    public static <O> List<O> splitAndTransform(@Nullable final String propValue,
                                                @NotNull final Pattern splitPattern,
                                                @NotNull final Unary<O, String> transformation) {

        final List<String> values = grep(map(Arrays.asList(Option.optional(propValue).map(split(splitPattern)).orSome(new String[]{})), trim()), isNotEmpty());
        return grepNotNull(map(values, transformation));
    }

    /**
     * Function that matches a string using the given pattern.
     *
     * @param pattern The pattern to use
     * @return The matching function
     */
    @NotNull
    public static Unary<Boolean,CharSequence> matches( @NotNull final Pattern pattern ) {
        return new Unary<Boolean,CharSequence>(){
            @Override
            public Boolean call( final CharSequence text ) {
                return pattern.matcher( text ).matches();
            }
        };
    }

    /**
     * Function that matches a string using the given prefix.
     *
     * @param prefix The prefix to match
     * @return The matching function
     */
    @NotNull
    public static Unary<Boolean,String> startsWith( @NotNull final String prefix ) {
        return new Unary<Boolean,String>(){
            @Override
            public Boolean call( final String text ) {
                return text.startsWith( prefix );
            }
        };
    }

    /**
     * Create a predicate that will match strings as long as they begin with one of the given prefixes.
     *
     * @param prefixes a list of prefixes to match.  Required but may be empty. Will be flattened into a case-sensitive Set.  Empty prefixes are not supported.
     *                 Overlapping prefixes are supported (eg, both "foo.bar.baz." and "foo.bar." in the prefix list will work as expected).
     * @return a predicate that will return true only for strings that match one of the specified prefixes.
     */
    @NotNull
    public static Unary<Boolean,String> matchesAnyPrefix(Collection<String> prefixes) {
        final NavigableSet<String> prefixSet = new TreeSet<String>(prefixes);
        return new Unary<Boolean, String>() {
            @Override
            public Boolean call(String val) {
                String lower = prefixSet.floor(val);
                while (lower != null && lower.length() > 0 && !val.startsWith(lower)) {
                    lower = prefixSet.lower(lower);
                }
                return lower != null;
            }
        };
    }

    private static final Unary<String,CharSequence> FUNC_TRIM = new Unary<String,CharSequence>() {
            @Override
            public String call( final CharSequence charSequence ) {
                return charSequence.toString().trim();
            }
        };

    private static final Unary<String,CharSequence> FUNC_LOWER = new Unary<String,CharSequence>() {
            @Override
            public String call( final CharSequence charSequence ) {
                return charSequence.toString().toLowerCase();
            }
        };

    private static final Unary<String,CharSequence> FUNC_UPPER = new Unary<String,CharSequence>() {
            @Override
            public String call( final CharSequence charSequence ) {
                return charSequence.toString().toUpperCase();
            }
        };

    private static final Unary<Boolean,CharSequence> FUNC_IS_EMPTY = new Unary<Boolean,CharSequence>() {
            @Override
            public Boolean call( final CharSequence charSequence ) {
                final String text = charSequence.toString();
                return text.isEmpty();
            }
        };

    private static final Unary<Boolean,CharSequence> FUNC_IS_NOT_EMPTY = negate( FUNC_IS_EMPTY );
}
