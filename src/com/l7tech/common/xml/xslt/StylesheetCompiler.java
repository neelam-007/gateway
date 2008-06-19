package com.l7tech.common.xml.xslt;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariCompiledStylesheet;
import com.l7tech.server.url.UrlCacheEntryType;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.StylesheetRoot;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class which takes care of compiling strings full of XSLT into CompiledStylesheet instances, including Tarari
 * capabilities if the hardware is currently available.
 */
public class StylesheetCompiler {
    protected static final Logger logger = Logger.getLogger(StylesheetCompiler.class.getName());
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Compile the specified XSLT stylesheet.
     *
     * @param xslt a String containing valid XSLT code.  Required.
     * @return a CompiledStylesheet instance ready to apply this stylesheet to TransformInput instances.  Never null.
     * @throws java.text.ParseException if this stylesheet cannot be compiled
     */
    public static CompiledStylesheet compileStylesheet(String xslt) throws ParseException {
        final GlobalTarariContext gtc = TarariLoader.getGlobalContext();
        TarariCompiledStylesheet tarariStylesheet =
                gtc == null ? null : gtc.compileStylesheet(xslt);
        final Templates templates = compileSoftware(xslt);
        return new CompiledStylesheet(templates, getVariablesUsed(templates), tarariStylesheet);
    }

    /**
     * @param thing  the stylesheet to parse
     * @return successfully compiled stylesheet.  Never null
     * @throws java.text.ParseException if the stylesheet can't be parsed
     */
    private static Templates compileSoftware(String thing) throws ParseException {
        // Prepare a software template
        try {
            TransformerFactory transfactory = TransformerFactory.newInstance();
            transfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transfactory.setURIResolver(XmlUtil.getSafeURIResolver());
            final List<TransformerException> fatals = new ArrayList<TransformerException>();
            transfactory.setErrorListener(new ErrorListener(){
                public void warning(TransformerException exception) throws TransformerException { }
                public void error(TransformerException exception) throws TransformerException { }
                public void fatalError(TransformerException exception) throws TransformerException { fatals.add(exception); }
            });
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver( XmlUtil.getSafeEntityResolver() );
            Document document = db.parse(new InputSource(new StringReader(thing)));

            // remove any output elements that use xalan extensions from the stylesheet doc before compiling
            removeOutputWithXalanExtension(document);

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


    /* Namespace attributes used for xalan extension */
    private static final String XALAN_EXTENSION_NS1 = "http://xml.apache.org/xalan";
    private static final String XALAN_EXTENSION_NS2 = "http://xml.apache.org/xslt";

    private static void removeOutputWithXalanExtension(Document document) {

        Map nsMap = XmlUtil.getNamespaceMap(document.getDocumentElement());
        if (nsMap.containsValue(XALAN_EXTENSION_NS1) || nsMap.containsValue(XALAN_EXTENSION_NS2)){

            // mark nodes for deletion
            List<Node> tobeDeleted = new ArrayList<Node>();

            // find the output elements that use xalan extensions
            NodeList nodes = document.getElementsByTagNameNS(UrlCacheEntryType.XSLT.getNamespaceUri(), "output");
            for (int i=0; i<nodes.getLength(); i++) {

                // check the element
                NamedNodeMap map = nodes.item(i).getAttributes();
                for (int j=0; j<map.getLength(); j++) {

                    if (XALAN_EXTENSION_NS1.equals(map.item(j).getNamespaceURI()) ||
                        XALAN_EXTENSION_NS2.equals(map.item(j).getNamespaceURI()))
                    {
                        // mark node for removal
                        tobeDeleted.add(nodes.item(i));
                    }
                }
            }

            // delete the output nodes from the document
            for (Node n : tobeDeleted) {
                n.getParentNode().removeChild(n);
            }
            // should we log?
//            try {
//                System.out.println("trimmed xsl:" + new String(XmlUtil.toByteArray(document.getDocumentElement())));
//            } catch (java.io.IOException ioe) {}
        }
    }
}
