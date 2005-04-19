
package com.l7tech.common.xml.tarari.util;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import java.io.*;
import java.text.ParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TarariXpathConverter {

    private static final Pattern FINDPOS = Pattern.compile("^line \\d+:(\\d+): (.*)");

    /**
     * Convert the specified prefix-adorned XPath expression into a Tarari-friendly
     * expression that uses predicates instead of namespace prefixes.
     * <p>
     * Examples:
     * <ul>
     * <li><pre>/e:employees/e:emp</pre> becomes:
     *     <pre>/employees[namespace-uri() ="http://junk.com/emp" ]/emp[namespace-uri() ="http://junk.com/emp" ]</pre>
     * <li><pre>/foo:ducksoup[1]/foo1:* /foo:ducksoup[position()=1]</pre> becomes:
     *     <pre>/ducksoup[namespace-uri() ="http://www.foo.com"  and (position() = 1)]/*[namespace-uri() ="http://www.food.com" ]/ducksoup[namespace-uri() ="http://www.foo.com"  and (position()=1)]</pre>
     * </ul>
     *
     * @param nsmap a Map associating namespace prefixes with namespace URIs.  Must not be null.
     *              Must contain one entry per unique namespace prefix in xpath.
     * @param xpath the XPath expression to convert.  Must not be null.
     *              Namespace prefixes must be declared in nsmap.
     * @return the converted fully-spelled-out XPath expression, ready to feed into Tarari.  Never null.
     * @throws ParseException if the provided xpath could not be converted using the provided nsmap.
     */
    public static String convertToTarariXpath(Map nsmap, String xpath) throws ParseException {
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();

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
        }
    }

    private static ParseException makeParseException(Throwable e) {
        String tseMess = e.getMessage();
        Matcher matcher = FINDPOS.matcher(tseMess);
        int pos = -1;
        String outMess = e.getMessage();
        if (matcher.find()) {
            try { pos = Integer.parseInt(matcher.group(1)); } catch (NumberFormatException nfe) {}
            outMess = matcher.group(2);
        }
        String spos = pos >= 0 ? "Position " + pos + ": " : "";
        ParseException pe = (ParseException)new ParseException(spos + outMess, pos).initCause(e);
        return pe;
    }
}
