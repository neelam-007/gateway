
package com.l7tech.xml.tarari.util;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import com.l7tech.util.ComparisonOperator;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.xml.xpath.FastXpath;

import java.io.*;
import java.text.ParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TarariXpathConverter {
    private static final Pattern FINDPOS = Pattern.compile("^line \\d+:(\\d+): (.*)");
    private static final Pattern FIND_COUNT_BEFORE = Pattern.compile("^count\\((.*)\\)\\s*(\\=|\\!\\=)\\s*(\\d+)$");
    private static final Pattern FIND_COUNT_AFTER = Pattern.compile("^(\\d+)\\s*(\\=|\\!\\=)\\s*count\\((.*)\\)$");

    /**
     * Convert the specified prefix-adorned XPath expression, which may also compare the count
     * of selected nodes with an integer, into a Tarari-friendly
     * expression that uses predicates instead of namespace prefixes.
     * Examples:
     * <ul>
     * <li><pre>/e:employees/e:emp</pre> becomes:
     *     expr="<pre>/employees[namespace-uri() ="http://junk.com/emp" ]/emp[namespace-uri() ="http://junk.com/emp" ]</pre>", operator=null, value=null
     * <li><pre>/foo:ducksoup[1]/foo1:* /foo:ducksoup[position()=1]</pre> becomes:
     *     expr="<pre>/ducksoup[namespace-uri() ="http://www.foo.com"  and (position() = 1)]/*[namespace-uri() ="http://www.food.com" ]/ducksoup[namespace-uri() ="http://www.foo.com"  and (position()=1)]</pre>", operator=null, val=null
     * <li><pre>count(/blah/blat) &lt; 7</pre> becomes:
     *     expr="<pre>/blah/blat</pre>", operator=LT, value=7
     * </ul>
     *
     * @param nsmap a Map associating namespace prefixes with namespace URIs.  Must not be null.
     *              Must contain one entry per unique namespace prefix in xpath.
     * @param xpath the XPath expression to convert.  Must not be null.
     *              Namespace prefixes must be declared in nsmap.
     * @return the converted fully-spelled-out XPath expression, ready to feed into Tarari.  Never null.
     * @throws ParseException if the provided xpath could not be converted using the provided nsmap.
     */
    public static FastXpath convertToFastXpath(Map nsmap, String xpath) throws ParseException {
        // Convert this Xpath into tarari format
        final ParseException outerPe;
        try {
            // First try it the simple way
            return new FastXpath(convertToTarariXpath(nsmap, xpath), null, null);
        } catch (ParseException e) {
            outerPe = e;
        }

        // Didn't work.  See if we can match something like "count(TNF) < 17"
        Matcher m = FIND_COUNT_BEFORE.matcher(xpath);
        String tnf = null;
        String cmp = null;
        String num = null;
        if (m.matches()) {
            tnf = m.group(1);
            cmp = m.group(2);
            num = m.group(3);
        } else {
            m = FIND_COUNT_AFTER.matcher(xpath);
            if (m.matches()) {
                tnf = m.group(3);
                cmp = m.group(2);
                num = m.group(1);
            }
        }

        final ComparisonOperator op;
        if ("=".equals(cmp)) {
            op = ComparisonOperator.EQ;
        } else if (">".equals(cmp)) {
            op = ComparisonOperator.GT;
        } else if ("<".equals(cmp)) {
            op = ComparisonOperator.LT;
        } else if (">=".equals(cmp)) {
            op = ComparisonOperator.GE;
        } else if ("<=".equals(cmp)) {
            op = ComparisonOperator.LT;
        } else if ("!=".equals(cmp)) {
            op = ComparisonOperator.NE;
        } else {
            // Some other comparison operator -- not supported
            throw new ParseException("Expression not supported by simultaneous XPath (operator=" + cmp + ")", 0);
        }

        try {
            int val = Integer.parseInt(num);
            if (tnf != null && tnf.startsWith("/"))
                //noinspection UnnecessaryBoxing
                return new FastXpath(convertToTarariXpath(nsmap, tnf), op, Integer.valueOf(val));
        } catch (NumberFormatException nfe) {
            // Non-integer comparison target -- not supported
            throw outerPe;
        }

        // Nope -- give it up
        throw outerPe;
    }

    static String convertToTarariXpath(Map nsmap, String xpath) throws ParseException {
        // Reject multiline xpaths
        int lf = xpath.indexOf('\n');
        if (lf >= 0) throw new ParseException("contains newline", lf);
        int cr = xpath.indexOf('\r');
        if (cr >= 0) throw new ParseException("contains carriage return", cr);

        if (!xpath.startsWith("/")) throw new ParseException("must start with a slash", 0);

        // Build input stream
        StringBuffer in = new StringBuffer();
        in.append(xpath);
        in.append('\n');

        // Prepare the lexer
        XprLexer xprLexer = new XprLexer(new StringReader(in.toString()));

        // Build output stream
        BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream();

        // Convert the xpath
        try {
            PrintWriter outWriter = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            XprParser parser = new XprParser(xprLexer);
            parser.setPrefixTab(nsmap);
            parser.setOutFile(outWriter);
            parser.mainModule();
            outWriter.flush();

            return out.toString("UTF-8").trim();

        } catch (TokenStreamException e) {
            throw makeParseException(e);
        } catch (RecognitionException e) {
            throw makeParseException(e);
        } catch (UnsupportedEncodingException e) {
            throw (ParseException)new ParseException("Unsupported encoding: " + e.getMessage(), 0).initCause(e); // can't happen
        } finally {
            out.close();
        }
    }

    private static ParseException makeParseException(Throwable e) {
        String tseMess = e.getMessage();
        Matcher matcher = FINDPOS.matcher(tseMess);
        int pos = -1;
        String outMess = e.getMessage();
        if (matcher.find()) {
            try {
                pos = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException nfe) {
                // Leave as -1
            }
            outMess = matcher.group(2);
        }
        String spos = pos >= 0 ? "Position " + pos + ": " : "";
        return (ParseException)new ParseException(spos + outMess, pos).initCause(e);
    }
}
