/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.tarari;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.tarari.xml.XmlParseException;
import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.util.UriResolver;
import com.tarari.xml.xslt11.Stylesheet;
import com.tarari.xml.xslt11.MessageListener;
import com.tarari.xml.xslt11.parser.XsltParseException;
import com.tarari.xml.xslt11.parser.StylesheetParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.ParseException;

/**
 * Implementation of {@link TarariCompiledStylesheet}.  Unlike its parent interface, this class will only be loaded
 * if Tarari is actually present, so it is able to safely reference Tarari classes directly.
 */
public class TarariCompiledStylesheetImpl implements TarariCompiledStylesheet {
    private final Stylesheet master;

    // Empty XSLT document
    private static final byte[] EMPTY_XSL_BYTES;
    static {
        byte[] bytes = null;
        try {
            bytes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"></xsl:stylesheet>".getBytes("UTF-8");
        } catch(java.io.UnsupportedEncodingException e) {
            // Will never happen
        }

        EMPTY_XSL_BYTES = bytes;
    }
    private static final UriResolver NON_FETCHING_URI_RESOLVER = new UriResolver() {
        public XmlSource resolveUri(String s, String s1) throws java.io.IOException {
            return new XmlSource(s, EMPTY_XSL_BYTES);
        }
    };

    // Thread-local XmlSource for transformations.
    private static final ThreadLocal xmlSource = new ThreadLocal() {
        protected Object initialValue() {
            return new XmlSource(new EmptyInputStream());
        }
    };

    public static StylesheetParser getStylesheetParser() {
        StylesheetParser stylesheetParser = new StylesheetParser();
        stylesheetParser.setUriResolver(NON_FETCHING_URI_RESOLVER);
        stylesheetParser.setEntityResolver( XmlUtil.getXss4jEntityResolver());

        return stylesheetParser;
    }

    /**
     * Wrap the specified Tarari Stylesheet instance.
     *
     * @param stylesheet      the stylesheet to compile.  Must not be null.
     * @throws ParseException if the specified stylesheet bytes could not be compiled.
     */
    TarariCompiledStylesheetImpl(String stylesheet) throws ParseException {
        try {
            InputSource is = new InputSource(new StringReader(stylesheet));
            is.setSystemId("");
            master = getStylesheetParser().parseStylesheet(new XmlSource(is));
        } catch (StackOverflowError e) {
            throw new ParseException("Unable to parse XSLT: " + ExceptionUtils.getMessage(e), 0); // Avoid rethrowing the HUGE stack trace
        } catch (XsltParseException e) {
            throw (ParseException)new ParseException("Unable to parse XSLT: " + ExceptionUtils.getMessage(e), 0).initCause(e);
        } catch (IOException e) {
            // Shouldn't be possible
            throw (ParseException)new ParseException("Unable to read XSLT: " + ExceptionUtils.getMessage(e), 0).initCause(e);
        } catch (XmlParseException e) {
            throw (ParseException)new ParseException("Unable to parse XSLT: " + ExceptionUtils.getMessage(e), 0).initCause(e);
        }
    }

    public void transform( final TarariMessageContext input,
                           final OutputStream output,
                           final String[] varsUsed,
                           final Functions.Unary<Object, String> variableGetter,
                           final ErrorListener errorListener ) throws IOException, SAXException {
        XmlSource source = (XmlSource)xmlSource.get();
        RaxDocument raxDocument = ((TarariMessageContextImpl)input).getRaxDocument();
        source.setData(raxDocument);
        transform(source, output, varsUsed, variableGetter, errorListener);
    }

    public void transform( final InputStream input,
                           final OutputStream output,
                           final String[] varsUsed,
                           final Functions.Unary<Object, String> variableGetter,
                           final ErrorListener errorListener ) throws SAXException, IOException {
        transform(new XmlSource(input), output, varsUsed, variableGetter, errorListener);
    }

    private void transform( final XmlSource source,
                            final OutputStream output,
                            final String[] varsUsed,
                            final Functions.Unary<Object, String> variableGetter,
                            final ErrorListener errorListener ) throws IOException, SAXException {
        Stylesheet transformer = new Stylesheet(master);
        transformer.setValidate(false);
        if (varsUsed != null && variableGetter != null) {
            for (String variableName : varsUsed) {
                Object value = variableGetter.call(variableName);
                if (value != null) transformer.setParameter(variableName, value);
            }
        }
        transformer.setMessageListener( new MessageListener() {
            @Override
            public void message( String s ) {
                if ( errorListener != null ) {
                    try {
                        errorListener.warning( new TransformerException( s ) );
                    } catch (TransformerException e) {
                        // listener error will not stop transform with Tarari
                    }
                }
            }
        } );
        XmlResult result = new XmlResult(output);
        try {
            transformer.transform(source, result);
        } catch (XmlParseException e) {
            throw new SAXException(e);
        }
    }
}
