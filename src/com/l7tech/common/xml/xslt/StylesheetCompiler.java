package com.l7tech.common.xml.xslt;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariCompiledStylesheet;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.StylesheetRoot;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
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
            StreamSource xsltsource = new StreamSource(new StringReader(thing));
            Templates result = transfactory.newTemplates(xsltsource);
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
}
