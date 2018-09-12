package com.l7tech.xml.xslt;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.XmlKnob;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.xml.ElementCursor;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents an XSLT that has been compiled and is ready to apply to a Message or Message part.
 * Use {@link StylesheetCompiler#compileStylesheet} to obtain an instance of this class.
 */
public class CompiledStylesheet {
    protected static final Logger logger = Logger.getLogger(CompiledStylesheet.class.getName());

    private static final String SYSTEM_ID_MESSAGE = "http://layer7tech.com/message"; // Dummy system identifier used to identify errors parsing a message.

    private final Templates softwareStylesheet;
    private final String[] varsUsed;

    /**
     * Produce a CompiledStylesheet using the specified software stylesheet.
     *
     * @param softwareStylesheet  a software stylesheet to use Required.
     * @param varsUsed     context variables used by the stylesheet
     */
    CompiledStylesheet(Templates softwareStylesheet, String[] varsUsed) {
        this.softwareStylesheet = softwareStylesheet;
        this.varsUsed = varsUsed;
        if (softwareStylesheet == null)
            throw new IllegalArgumentException("softwareStylesheet must be provided");
    }

    /**
     * Perform an XSL transformation on the specified TransformInput, sending output to the specified TransformOutput.
     *
     * @param input    the input document for the transformation.  Required.
     * @param output   the output for the transformation.  Required.
     * @param errorListener  ErrorListener to which DOM-based transformation errors should be reported. Optional.
     * @throws IOException   if there is a problem reading the intput or writing the output
     * @throws SAXException  if there is a problem parsing the source document
     * @throws TransformerException if there is a problem performing the transformation that isn't due to one of the
     *                              above problems
     */
    public void transform(TransformInput input, TransformOutput output, ErrorListener errorListener)
            throws SAXException, IOException, TransformerException {
        XmlKnob xmlKnob = input.getXmlKnob();
        if (xmlKnob.isDomParsed() ) {
            transformDom(input, output, errorListener);
        } else {
            transformSax(input, output, errorListener);
        }
    }

    private void transformDom(TransformInput t,
                              TransformOutput output,
                              ErrorListener errorListener)
            throws SAXException, IOException, TransformerException
    {
        final ElementCursor ec = t.getXmlKnob().getElementCursor();
        ec.moveToDocumentElement();
        final Source source = new DOMSource(ec.asDomElement().getOwnerDocument());
        transformUsingSoftwareStylesheet(source, t, output, errorListener);
    }

    private void transformSax(TransformInput t, TransformOutput output, ErrorListener errorListener) throws SAXException, IOException, TransformerException {
        // Read the part content non-destructively so that content will not be lost if parser/transformer fails for any reason.
        final InputSource input = t.getXmlKnob().getInputSource(false);
        input.setSystemId( SYSTEM_ID_MESSAGE ); // used to identify parse errors in message
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature( "http://xml.org/sax/features/namespaces", true );
        xmlReader.setFeature( XmlUtil.XERCES_DISALLOW_DOCTYPE, true );
        xmlReader.setEntityResolver( XmlUtil.getSafeEntityResolver() );
        xmlReader.setErrorHandler( XmlUtil.getStrictErrorHandler() );
        final Source source = new SAXSource( xmlReader, input );
        transformUsingSoftwareStylesheet( source, t, output, errorListener );
    }

    private void transformUsingSoftwareStylesheet(Source source, TransformInput t, TransformOutput output, ErrorListener errorListener) throws SAXException, TransformerException, IOException {
        final PoolByteArrayOutputStream os = new PoolByteArrayOutputStream(4096);
        final StreamResult sr = new StreamResult(os);

        try {
            Transformer transformer = softwareStylesheet.newTransformer();
            transformer.setURIResolver(XmlUtil.getSafeURIResolver());
            if (errorListener != null)
                transformer.setErrorListener(errorListener);
            for (String variableName : varsUsed) {
                Object value = t.getVariableValue(variableName);
                if (value != null)
                    transformer.setParameter(variableName, value);
            }
            transformer.transform(source, sr);
            output.setBytes(os.toByteArray());
            logger.finest("software xsl transformation completed");
        } catch ( TransformerException e ) {
            final SourceLocator locator = e.getLocator();
            final boolean xalanEmptySax = e.getCause() == null && locator != null && (SYSTEM_ID_MESSAGE.equals(locator.getSystemId()) || -1 == locator.getLineNumber());
            if (xalanEmptySax) {
                // translate to a parse error for consistency
                final LocatorImpl saxLocator = new LocatorImpl();
                saxLocator.setColumnNumber( locator.getColumnNumber() );
                saxLocator.setLineNumber( locator.getLineNumber() );
                saxLocator.setPublicId( locator.getPublicId() );
                saxLocator.setSystemId( locator.getSystemId() );
                throw new SAXParseException( ExceptionUtils.getMessage(e), saxLocator, e );
            }
            final boolean saxonEmptySax = locator == null && e.getCause() != null && e.getCause().getMessage() != null && e.getCause().getMessage().contains("Premature end of file");
            if (saxonEmptySax) {
                // translate to a parse error for consistency (Bug #13235)
                throw new SAXParseException( ExceptionUtils.getMessage(e), null, e );
            }
            throw e;
        } finally {
            os.close();
        }
    }
}
