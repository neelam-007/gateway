package com.l7tech.xml.xslt;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.SaxonUtils;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.om.StructuredQName;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xml.utils.DefaultErrorHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with XSL transformations.
 */
public class XsltUtil {
    private static final Logger logger = Logger.getLogger(XsltUtil.class.getName());

    /**
     * Get the variables used by the specified XSLT transformation, expressed as an XML string.
     *
     * @param xslSrc the stylesheet XML to examine.  Required.
     * @param xsltVersion the XSLT version to assume, or null if not known.  A null value will cause the system default TransformerFactory to be used.
     * @return a list of variables used.  Never null but may be empty.
     * @throws TransformerConfigurationException if the stylesheet could not be compiled
     * @throws ParseException if the stylesheet could not be compiled
     * @throws IOException if an I/O error occurs while parsing the stylesheet XML.  This is probably impossible in practice given that the input is a string.
     * @throws SAXException if the stylesheet could not be parsed as XML.
     */
    public static List<String> getVariablesUsedByStylesheet(String xslSrc, String xsltVersion) throws TransformerConfigurationException, IOException, SAXException, ParseException {
        final List<TransformerException> fatals = new ArrayList<TransformerException>();
        TransformerFactory tf = createTransformerFactory(xsltVersion, fatals);
        tf.setURIResolver( new URIResolver(){
            public Source resolve( String href, String base ) throws TransformerException {
                return new StreamSource(new StringReader("<a xsl:version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>"));
            }
        } );

        final DOMSource source = new DOMSource(XmlUtil.parse(new StringReader(xslSrc), false));
        return getVariablesUsedByTemplates(compileStylesheet(tf, source, fatals));
    }

    private static Templates compileStylesheet(TransformerFactory tf, Source source, List<TransformerException> fatals) throws ParseException {
        Templates templates;
        try {
            templates = tf.newTemplates(source);
        } catch (TransformerConfigurationException e) {
            throwParseException(ExceptionUtils.getMessage(e), e, fatals);
            /* NOTREACHED */
            throw new IllegalStateException("NOTREACHED");
        }
        return templates;
    }

    /**
     * Get variables used by the specified compiled stylesheet, which must use either Xalan or Saxon as the underlying
     * implementation.
     *
     * @param templates a compiled stylesheet.  Required.
     * @return the variables used by this stylesheet.  May be empty but never null.
     */
    public static List<String> getVariablesUsedByTemplates(@NotNull Templates templates) {
        List<String> vars = new ArrayList<String>();
        if (templates instanceof StylesheetRoot) {
            StylesheetRoot stylesheetRoot = (StylesheetRoot)templates;
            @SuppressWarnings("unchecked") Vector<ElemVariable> victor = stylesheetRoot.getVariablesAndParamsComposed();
            for (ElemVariable var : victor) {
                vars.add(var.getName().getLocalName());
            }
        } else if (templates instanceof PreparedStylesheet) {
            PreparedStylesheet ps = (PreparedStylesheet) templates;
            List<StructuredQName> qns = ps.getGlobalVariableMap().getVariableMap();
            for (StructuredQName qn : qns) {
                vars.add(qn.getLocalPart());
            }
        } else {
            throw new IllegalArgumentException("Compiled XSLT was not a recognized Xalan or Saxon stylesheet class -- can't get declared variables");
        }
        return vars;
    }

    /**
     * Create a TransformerFactory that can be used to compile a stylesheet.
     *
     * @param xsltVersion the XSLT version we expect to have to compile, or null if not known.  If null, the system default TransformerFactory will be used.
     * @param collectFatals a List in which to collect fatal errors reported during stylesheet compilation, or null to set an error handler that immediately throws on error.
     * @return a new TransformerFactory configured for safety.  Its configuration can be overridden before it is used.  Never null.
     * @throws TransformerConfigurationException if a transformer factory could not be created.
     */
    public static TransformerFactory createTransformerFactory(@Nullable String xsltVersion, @Nullable final List<TransformerException> collectFatals) throws TransformerConfigurationException {
        final boolean useSaxon = shouldUseSaxon(xsltVersion);
        final Class<TransformerFactoryImpl> factoryClass = useSaxon ? TransformerFactoryImpl.class : null;
        TransformerFactory transfactory = factoryClass == null
                ? TransformerFactory.newInstance()
                : TransformerFactory.newInstance(factoryClass.getName(), factoryClass.getClassLoader());

        if (useSaxon) {
            SaxonUtils.configureSecureSaxonTransformerFactory(transfactory);
        }

        transfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transfactory.setURIResolver(XmlUtil.getSafeURIResolver());
        if (collectFatals == null) {
            transfactory.setErrorListener(new DefaultErrorHandler(true));
        } else {
            transfactory.setErrorListener(new FatalErrorCollectingErrorListener(collectFatals));
        }
        return transfactory;
    }

    /**
     * Check if Saxon should be used to compile a stylesheet under the specified explicitly-selected version.
     *
     * @param xsltVersion the XSLT version we are expected, or null if not known.
     * @return true if Saxon should be used to compile this stylesheet.
     */
    private static boolean shouldUseSaxon(String xsltVersion) {
        return "2.0".equals(xsltVersion) || ConfigFactory.getBooleanProperty(SaxonUtils.PROP_ALWAYS_USE_SAXON, false);
    }

    /**
     * Check whether the specified document is an XSLT stylesheet that can be compiled by an XSLT processor capable of
     * at least the specified XSLT version.
     *
     * @param doc a DOM Document that might contain an XSLT stylesheet.  Required.
     * @param xsltVersion minimum XLST version required to compile this stylesheet, or null to assume the system default TransformerFactory can handle it.
     * @throws TransformerConfigurationException if the stylesheet could not be compiled
     * @throws ParseException if the stylesheet could not be compiled
     * @throws IOException if an I/O error occurs while parsing the stylesheet XML.  This is probably impossible in practice given that the input is a string.
     * @throws SAXException if the stylesheet could not be parsed as XML.
     */
    public static void checkXsltSyntax(@NotNull Document doc, @Nullable String xsltVersion) throws TransformerConfigurationException, IOException, SAXException, ParseException {
        final List<TransformerException> fatals = new ArrayList<TransformerException>();
        TransformerFactory tf = createTransformerFactory(xsltVersion, fatals);
        tf.setURIResolver( new URIResolver(){
            public Source resolve( String href, String base ) throws TransformerException {
                return new StreamSource(new StringReader("<a xsl:version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>"));
            }
        } );
        final DOMSource source = new DOMSource(XmlUtil.parse(new StringReader(XmlUtil.nodeToString(doc)), false));
        compileStylesheet(tf, source, fatals);
    }

    /**
     * Report a ParseException, including diagnostic information from the first fatal error, if any are provided.
     *
     * @param message exception message to use
     * @param cause cause of this exception, or null
     * @param fatals collected fatal errors, or null
     * @throws java.text.ParseException always
     */
    static void throwParseException(String message, @Nullable Throwable cause, @Nullable List<TransformerException> fatals) throws ParseException {
        if (fatals != null && !fatals.isEmpty()) {
            TransformerException te = fatals.iterator().next();
            throw (ParseException)new ParseException(message + ": " + ExceptionUtils.getMessage(te), 0).initCause(te);
        }
        throw (ParseException)new ParseException(message, 0).initCause(cause);
    }

    private static class FatalErrorCollectingErrorListener implements ErrorListener {
        private final List<TransformerException> collectFatals;

        public FatalErrorCollectingErrorListener(List<TransformerException> collectFatals) {
            this.collectFatals = collectFatals;
        }

        public void warning(TransformerException exception) throws TransformerException {
            maybeLog(exception);
        }

        public void error(TransformerException exception) throws TransformerException {
            maybeLog(exception);
        }

        public void fatalError(TransformerException exception) throws TransformerException {
            maybeLog(exception);
            collectFatals.add(exception);
        }

        private void maybeLog(TransformerException exception) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, ExceptionUtils.getMessage(exception), exception);
        }
    }
}
