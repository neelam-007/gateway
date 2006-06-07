/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.Closeable;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariSchemaHandler;
import static com.l7tech.server.communityschemas.CompiledSchema.HardwareStatus.OFF;
import static com.l7tech.server.communityschemas.CompiledSchema.HardwareStatus.ON;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

class CompiledSchema implements Closeable {
    private static final Logger logger = Logger.getLogger(CompiledSchema.class.getName());
    public static final String PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS =
            "com.l7tech.server.schema.suppressSoftwareRecheckIfHardwareFlagsInvalidXml";
    public static final boolean SUPPRESS_FALLBACK_IF_TARARI_FAILS = Boolean.getBoolean(PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS);

    public static enum HardwareStatus {
        /** The schema is loaded on hardware and validation will be accelerated. */
        ON,
        /** The schema is not loaded on the hardware. */
        OFF,
        /** The schema cannot be hardware accelerated. */
        BAD
    }

    private final String targetNamespace;
    private final String systemId;
    private final Schema softwareSchema;
    private final String schemaDocument;
    private final CompiledSchemaManager manager;
    private final AtomicInteger refcount = new AtomicInteger(0);
    private boolean closed = false;

    private HardwareStatus hardwareStatus = OFF;

    SchemaHandle ref() {
        refcount.incrementAndGet();
        return new SchemaHandle(this);
    }

    void unref() {
        int nval = refcount.decrementAndGet();
        if (nval <= 0) {
            close();
        }
    }

    private synchronized boolean isClosed() {
        return closed;
    }

    CompiledSchema(String targetNamespace, String systemId, String schemaDocument, Schema softwareSchema, CompiledSchemaManager manager) {
        if (targetNamespace == null || softwareSchema == null || schemaDocument == null)
            throw new NullPointerException();

        this.targetNamespace = targetNamespace;
        this.systemId = systemId;
        this.softwareSchema = softwareSchema;
        this.schemaDocument = schemaDocument.intern();
        this.manager = manager;
    }

    String getTargetNamespace() {
        return targetNamespace;
    }

    String getSystemId() {
        return systemId;
    }

    Schema getSoftwareSchema() {
        return softwareSchema;
    }

    String getSchemaDocument() {
        return schemaDocument;
    }

    synchronized HardwareStatus getHardwareStatus() {
        return hardwareStatus;
    }

    synchronized void setHardwareStatus(HardwareStatus hardwareStatus) {
        this.hardwareStatus = hardwareStatus;
    }

    /**
     * Validate the specified message with this schema, using Tarari if possible.
     *
     * @param msg           the message to validate.  Must not be null.
     * @param errorHandler  error handler for accumulating error information.  Must not be null.
     * @throws NoSuchPartException  if a required MIME part has vanished
     * @throws IOException          if the message cannot be read
     * @throws SAXException         if the message is found to be invalid
     */
    void validateMessage(Message msg, SchemaValidationErrorHandler errorHandler) throws NoSuchPartException, IOException, SAXException {
        if (isClosed()) throw new IllegalStateException("CompiledSchema has already been closed");
        TarariSchemaHandler schemaHandler = TarariLoader.getSchemaHandler();
        boolean isSoap = msg.isSoap();

        TarariKnob tk = (TarariKnob) msg.getKnob(TarariKnob.class);
        TarariMessageContext tmc = tk == null ? null : tk.getContext();
        if (schemaHandler == null || tk == null || tmc == null || hardwareStatus != ON || targetNamespace == null) {
            // Hardware impossible or inappropriate in this case
            validateMessageDom(msg, isSoap, errorHandler);
            return;
        }

        // Do it in Tarari
        try {
            boolean result = schemaHandler.validate(tk.getContext());
            if (!result) throw new SAXException("Validation failed: message was not valid");

            // Hardware schema val succeeded.
            if (isSoap) {
                checkPayloadNamespaceUris(tk);
            } else {
                checkRootNamespace(tmc);
            }
        } catch (SAXException e) {
            if (SUPPRESS_FALLBACK_IF_TARARI_FAILS) {
                // Don't recheck with software -- just record the failure and be done
                throw e;
            }

            // Recheck failed validations with software, in case the Tarari failure was spurious
            errorHandler.recordedErrors().clear();
            validateMessageDom(msg, isSoap, errorHandler);
            logger.info("Hardware schema validation failed. The assertion will " +
                    "fallback on software schema validation");
        }

    }

    private void validateMessageDom(Message msg, boolean soap, SchemaValidationErrorHandler errorHandler) throws SAXException, IOException {
        final Document doc = msg.getXmlKnob().getDocumentReadOnly();
        Element[] elements;
        if (soap) {
            Element bodyel;
            try {
                bodyel = SoapUtil.getBodyElement(doc);
            } catch (InvalidDocumentFormatException e) {
                throw new SAXException("No SOAP Body found", e); // Highly unlikely
            }
            NodeList bodychildren = bodyel.getChildNodes();
            ArrayList<Element> children = new ArrayList<Element>();
            for (int i = 0; i < bodychildren.getLength(); i++) {
                Node child = bodychildren.item(i);
                if (child instanceof Element) {
                    children.add((Element) child);
                }
            }
            elements = new Element[children.size()];
            int cnt = 0;
            for (Iterator i = children.iterator(); i.hasNext(); cnt++) {
                elements[cnt] = (Element) i.next();
            }
        } else {
            elements = new Element[]{doc.getDocumentElement()};
        }
        validateElements( elements, errorHandler);
    }

    /**
     * Ensure that all payload namespaces of this SOAP message match the tarariNamespaceUri.
     */
    private void checkPayloadNamespaceUris(TarariKnob tk) throws SAXException {
        String payloadUris[] = tk.getSoapInfo().getPayloadNsUris();

        if (payloadUris == null || payloadUris.length < 1)
            throw new SAXException("Validation failed: message did not include a recognized payload namespace URI");

        // They must all match up
        for (String payloadUri : payloadUris) {
            if (!targetNamespace.equals(payloadUri))
                throw new SAXException("Validation failed: message contained an unexpected payload namespace URI: " + payloadUri);
        }
    }

    /**
     * Ensure that the root namespace of this XML message matches the tarariNamespaceUri.
     */
    private void checkRootNamespace(TarariMessageContext tmc) throws SAXException {
        // Non-SOAP message.  Ensure root namespace URI matches up.
        ElementCursor cursor = tmc.getElementCursor();
        cursor.moveToDocumentElement();
        String docNs = cursor.getNamespaceUri();
        if (!targetNamespace.equals(docNs))
            throw new SAXException("Hardware schema validation succeeded against non-SOAP message, " +
                    "but the document element namespace URI did not match the asseriton at hand.");
    }


    /**
     * Validate the specified elements.
     *
     * @param elementsToValidate  elements to validate.   Must not be null or empty.
     * @param errorHandler  error handler for accumlating errors.  Must not be null.
     * @throws IOException   if an input element could not be read.
     * @throws SAXException  if at least one element was found to be invalid.  Contains the very first validation error;
     *                       the error handler will have been told about this and any subsequent errors.
     */
    void validateElements(Element[] elementsToValidate, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
        if (isClosed()) throw new IllegalStateException();
        Validator v = softwareSchema.newValidator();
        v.setErrorHandler(errorHandler);
        v.setResourceResolver(XmlUtil.getSafeLSResourceResolver());

        SAXException firstException = null;
        for (Element element : elementsToValidate) {
            try {
                v.validate(new DOMSource(element));
            } catch (SAXException e) {
                if (firstException == null) firstException = e;
            }
        }

        if (firstException != null) throw firstException;

        Collection<SAXParseException> errors = errorHandler.recordedErrors();
        if (!errors.isEmpty()) throw errors.iterator().next();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompiledSchema that = (CompiledSchema) o;

        //noinspection RedundantIfStatement
        if (schemaDocument != null ? !schemaDocument.equals(that.schemaDocument) : that.schemaDocument != null)
            return false;

        return true;
    }

    public int hashCode() {
        return (schemaDocument != null ? schemaDocument.hashCode() : 0);
    }

    /** Sets the {@link #closed} flag and returns the old value. */
    private synchronized boolean setClosed() {
        boolean old = closed;
        closed = true;
        return old;
    }

    public void close() {
        if (setClosed()) return;
        manager.closeSchema(this);
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString()).append(" ");
        if (systemId != null) sb.append("systemId=\"").append(systemId).append("\" ");
        if (targetNamespace != null) sb.append("tns=\"").append(targetNamespace).append("\" ");
        sb.append("hardware=\"").append(hardwareStatus).append("\" ");
        sb.append("doc=\"").append(System.identityHashCode(schemaDocument));
        return sb.toString();
    }
}
