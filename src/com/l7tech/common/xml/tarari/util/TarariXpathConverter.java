
package com.l7tech.common.xml.tarari.util;

import antlr.TokenStreamException;

import java.io.*;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TarariXpathConverter {

    /**
     *
     * @param args
     * @deprecated use {@link #convertToTarariXpath(java.util.Map, String)} instead.
     * TODO remove this
     */
    public static void main(String args[])
    {
        if ( args.length != 2 ) {
            System.err.println("Usage: java TarariXpathConverter inputFile outputFile.") ;
            System.err.println("Make sure that antlr.jar is included in your CLASSPATH") ;
        }
        try {
            XprLexer lex = new XprLexer( new FileInputStream( args[0] ) ) ;
            PrintWriter outf = new PrintWriter( new FileWriter( args[1] ) ) ;
            XprParser parser = new XprParser( lex ) ;
            parser.setOutFile( outf ) ;
            parser.mainModule() ;
            outf.flush() ;
            System.out.println("Wrote output to file " + args[1] ) ;
        } catch ( Exception e ) {
            System.err.println( e.getMessage() ) ;
            e.printStackTrace( System.err ) ;
        }
    }

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
        if (lf >= 0) throw new ParseException(xpath, lf);
        int cr = xpath.indexOf('\r');
        if (cr >= 0) throw new ParseException(xpath, cr);

        // Build input stream
        StringBuffer in = new StringBuffer();
        Set nsEntries = nsmap.entrySet();
        for (Iterator i = nsEntries.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String prefix = (String)entry.getKey();
            String uri = (String)entry.getValue();
            in.append("declare namespace ");
            in.append(prefix);
            in.append('=');
            in.append('"');
            in.append(uri);
            in.append('"');
            in.append(';');
            in.append('\n');
        }
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
            parser.setOutFile(outWriter);
            parser.mainModule();
            outWriter.flush();

            return out.toString("UTF-8").trim();

        } catch (TokenStreamException e) {
            throw (ParseException)new ParseException(xpath, 0).initCause(e);
        } catch (UnsupportedEncodingException e) {
            throw (ParseException)new ParseException(xpath, 0).initCause(e); // can't happen
        }
    }
}
