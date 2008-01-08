package com.l7tech.common.xml.xslt;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.tarari.TarariCompiledStylesheet;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents an XSLT that has been compiled and is ready to apply to a Message or Message part, using
 * Tarari if it is available.  Use {@link StylesheetCompiler#compileStylesheet} to obtain an instance of this class.
 */
public class CompiledStylesheet {
    protected static final Logger logger = Logger.getLogger(CompiledStylesheet.class.getName());

    private final TarariCompiledStylesheet tarariStylesheet;
    private final Templates softwareStylesheet;
    private final String[] varsUsed;

    /**
     * Produce a CompiledStylesheet using the specified software stylesheet and Tarari compiled stylesheet.
     *
     * @param softwareStylesheet  a software stylesheet to use when Tarari can't be used for whatever reason.  Required.
     * @param tarariStylesheet   a Tarari compiled stylesheet to use when possible, or null to always use software.
     */
    CompiledStylesheet(Templates softwareStylesheet, String[] varsUsed, TarariCompiledStylesheet tarariStylesheet) {
        this.tarariStylesheet = tarariStylesheet;
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
     * @param errorListener  ErrorListener to which DOM-based transformation errors should be reported.
     *                       Will not be used if a Tarari transformation is attempted.  Optional.
     * @throws IOException   if there is a problem reading the intput or writing the output
     * @throws SAXException  if there is a problem parsing the source document
     * @throws TransformerException if there is a problem performing the transformation that isn't due to one of the
     *                              above problems
     */
    public void transform(TransformInput input, TransformOutput output, ErrorListener errorListener)
            throws SAXException, IOException, TransformerException {
        if (tarariStylesheet != null) {
            ElementCursor ec = input.getElementCursor();
            TarariMessageContext tmc = ec.getTarariMessageContext();
            if (tmc != null) {
                transformTarari(input, tmc, output);
                return;
            }
        }

        transformDom(input, output, errorListener);
    }

    private void transformTarari(TransformInput t, TarariMessageContext tmc, TransformOutput output)
            throws IOException, SAXException
    {
        assert tmc != null;
        assert tarariStylesheet != null;

        BufferPoolByteArrayOutputStream os = new BufferPoolByteArrayOutputStream(4096);
        try {
            tarariStylesheet.transform(tmc, os, varsUsed, t.getVariableGetter());
            output.setBytes(os.toByteArray());
            logger.finest("Tarari xsl transformation completed");
        } finally {
            os.close();
        }
    }

    private void transformDom(TransformInput t,
                              TransformOutput output,
                              ErrorListener errorListener)
            throws SAXException, IOException, TransformerException
    {
        final ElementCursor ec = t.getElementCursor();
        ec.moveToDocumentElement();
        final Document doctotransform = ec.asDomElement().getOwnerDocument();

        final BufferPoolByteArrayOutputStream os = new BufferPoolByteArrayOutputStream(4096);
        final StreamResult sr = new StreamResult(os);

        try {
            Transformer transformer = softwareStylesheet.newTransformer();
            transformer.setURIResolver(XmlUtil.getSafeURIResolver());
            if (errorListener != null) transformer.setErrorListener(errorListener);
            for (String variableName : varsUsed) {
                Object value = t.getVariableValue(variableName);
                if (value != null) transformer.setParameter(variableName, value);
            }
            transformer.transform(new DOMSource(doctotransform), sr);
            output.setBytes(os.toByteArray());
            logger.finest("software xsl transformation completed");
        } finally {
            os.close();
        }
    }

}
