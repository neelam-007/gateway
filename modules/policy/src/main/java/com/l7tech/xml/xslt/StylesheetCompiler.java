package com.l7tech.xml.xslt;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContext;
import com.l7tech.xml.tarari.TarariCompiledStylesheet;

import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.StylesheetRoot;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import java.io.StringReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class which takes care of compiling strings full of XSLT into CompiledStylesheet instances, including Tarari
 * capabilities if the hardware is currently available.
 */
public class StylesheetCompiler {
    protected static final Logger logger = Logger.getLogger(StylesheetCompiler.class.getName());
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    /* XSLT namespace */
    private static final String NAMESPACE_XSLT = "http://www.w3.org/1999/XSL/Transform";
    /* Namespace attributes used for xalan extension */
    private static final String XALAN_EXTENSION_NS1 = "http://xml.apache.org/xalan";
    private static final String XALAN_EXTENSION_NS2 = "http://xml.apache.org/xslt";
    private static final String PROP_DISABLE_FORCE_ENCODING = "com.l7tech.xml.disableForceXsltEncoding"; 

    /**
     * Compile the specified XSLT stylesheet.
     *
     * @param xslt a String containing valid XSLT code.  Required.
     * @return a CompiledStylesheet instance ready to apply this stylesheet to TransformInput instances.  Never null.
     * @throws java.text.ParseException if this stylesheet cannot be compiled
     */
    public static CompiledStylesheet compileStylesheet(String xslt) throws ParseException {
        final GlobalTarariContext gtc = TarariLoader.getGlobalContext();
        final Document cleanXslt = preprocessStylesheet( xslt );
        TarariCompiledStylesheet tarariStylesheet =
                gtc == null ? null : gtc.compileStylesheet(toString(cleanXslt));
        final Templates templates = compileSoftware(cleanXslt);
        return new CompiledStylesheet(templates, getVariablesUsed(templates), tarariStylesheet);
    }

    /**
     * Convert the DOM to string
     */
    private static String toString( final Document document ) throws ParseException {
        try {
            return XmlUtil.nodeToString( document );
        } catch (IOException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        }
    }

    /**
     * @param document the stylesheet to compile
     * @return successfully compiled stylesheet.  Never null
     * @throws java.text.ParseException if the stylesheet can't be parsed
     */
    private static Templates compileSoftware( final Document document ) throws ParseException {
        // Prepare a software template
        try {
            // Configure transformer
            TransformerFactory transfactory = TransformerFactory.newInstance();
            transfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transfactory.setURIResolver(XmlUtil.getSafeURIResolver());
            final List<TransformerException> fatals = new ArrayList<TransformerException>();
            transfactory.setErrorListener(new ErrorListener(){
                public void warning(TransformerException exception) throws TransformerException { }
                public void error(TransformerException exception) throws TransformerException { }
                public void fatalError(TransformerException exception) throws TransformerException { fatals.add(exception); }
            });

            // create the XSL transform template
            Templates result = transfactory.newTemplates(new DOMSource(document));
            if (result == null) {
                if (!fatals.isEmpty()) {
                    TransformerException te = fatals.iterator().next();
                    throw (ParseException)new ParseException(ExceptionUtils.getMessage(te), 0).initCause(te);
                }
                throw new ParseException("Unable to parse stylesheet: transformer factory returned null", 0);
            }
            
            return result;
        } catch (TransformerConfigurationException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        } catch (Exception e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        }
    }

    /**
     * Preprocess the given XSLT and remove any XALAN specifics, and ensure output encoding is UTF-8.
     */
    private static Document preprocessStylesheet(String thing) throws ParseException {
        // Prepare a software template
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver( XmlUtil.getSafeEntityResolver() );
            Document document = db.parse(new InputSource(new StringReader(thing)));

            // remove any output elements that use xalan extensions from the stylesheet doc before compiling
            removeOutputXalanExtensions(document);

            // remove any output elements that use xalan extensions from the stylesheet doc before compiling
            if ( !ConfigFactory.getBooleanProperty( PROP_DISABLE_FORCE_ENCODING, false ) ) {
                configureOutputEncoding(document);
            }

            return document;
        } catch (IOException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        } catch (SAXException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        } catch (ParserConfigurationException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        }
    }

    private static String[] getVariablesUsed(Templates temp) {
        String[] varsUsed;
        try {
            if (temp instanceof StylesheetRoot) {
                List<String> vars = new ArrayList<String>();
                StylesheetRoot stylesheetRoot = (StylesheetRoot)temp;
                List victor = stylesheetRoot.getVariablesAndParamsComposed();
                for (Object aVictor : victor) {
                    ElemVariable elemVariable = (ElemVariable)aVictor;
                    vars.add(elemVariable.getName().getLocalName());
                }
                varsUsed = vars.toArray(new String[vars.size()]);
            } else {
                logger.warning("XSLT was not a " + StylesheetRoot.class.getName() + ", can't get declared variables");
                varsUsed = EMPTY_STRING_ARRAY;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't get declared variables from stylesheet", e);
            varsUsed = EMPTY_STRING_ARRAY;
        }

        return varsUsed;
    }

    private static void removeOutputXalanExtensions(final Document document) {

        // find the output elements that use xalan extensions
        NodeList nodes = document.getDocumentElement().getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ( node.getNodeType() == Node.ELEMENT_NODE &&
                 NAMESPACE_XSLT.equals(node.getNamespaceURI()) &&
                 "output".equals(node.getLocalName())) {

                // check the element
                NamedNodeMap map = node.getAttributes();
                List<Node> toRemove = null;
                for (int j=0; j<map.getLength(); j++) {

                    if (XALAN_EXTENSION_NS1.equals(map.item(j).getNamespaceURI()) ||
                        XALAN_EXTENSION_NS2.equals(map.item(j).getNamespaceURI()))
                    {
                        if ( toRemove == null ) toRemove = new ArrayList<Node>();
                        // mark node for removal
                        toRemove.add(map.item(j));
                    }
                }

                if ( toRemove != null ) {
                    for ( Node attrNode : toRemove ) {
                        map.removeNamedItemNS( attrNode.getNamespaceURI(), attrNode.getLocalName() );
                    }
                }
            }
        }
    }

    private static void configureOutputEncoding(final Document document) {

        // find the output elements with charset and ensure utf-8
        NodeList nodes = document.getDocumentElement().getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ( node.getNodeType() == Node.ELEMENT_NODE &&
                 NAMESPACE_XSLT.equals(node.getNamespaceURI()) &&
                 "output".equals(node.getLocalName())) {

                Element output = (Element) node;

                // check the element
                String outputEnc = output.getAttribute("encoding");
                if ( output.hasAttribute("encoding") && !outputEnc.equalsIgnoreCase("UTF-8") ) {
                    if ( logger.isLoggable(Level.FINE) ) {
                        logger.fine("Modifying output encoding for XSLT from '"+outputEnc+"' to 'UTF-8'.");
                    }
                    output.setAttribute("encoding", "UTF-8");
                }
            }
        }
    }

}
