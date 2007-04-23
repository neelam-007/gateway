package com.l7tech.common.xml.xslt;

import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariCompiledStylesheet;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;
import java.text.ParseException;
import java.io.StringReader;

/**
 * Class which takes care of compiling strings full of XSLT into CompiledStylesheet instances, including Tarari
 * capabilities if the hardware is currently available.
 */
public class StylesheetCompiler {

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
        return new CompiledStylesheet(compileSoftware(xslt), tarariStylesheet);
    }

    /**
     * @param thing  the stylesheet to parse
     * @return successfully compiled stylesheet.  Never null
     * @throws java.text.ParseException if the stylesheet can't be parsed
     */
    private static Templates compileSoftware(String thing) throws ParseException {
        // Prepare a software template
        try {
            TransformerFactory transfoctory = TransformerFactory.newInstance();
            transfoctory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transfoctory.setURIResolver(XmlUtil.getSafeURIResolver());
            transfoctory.setErrorListener(new ErrorListener(){
                public void warning(TransformerException exception) throws TransformerException {}
                public void error(TransformerException exception) throws TransformerException {}
                public void fatalError(TransformerException exception) throws TransformerException {}
            });
            StreamSource xsltsource = new StreamSource(new StringReader(thing));
            return transfoctory.newTemplates(xsltsource);
        } catch (TransformerConfigurationException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        }
    }
}
