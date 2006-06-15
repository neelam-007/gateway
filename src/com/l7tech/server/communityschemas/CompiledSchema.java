/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.LSInputImpl;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariSchemaHandler;
import com.l7tech.common.xml.tarari.TarariSchemaSource;
import com.l7tech.server.util.AbstractReferenceCounted;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Logger;

public final class CompiledSchema extends AbstractReferenceCounted<SchemaHandle> implements TarariSchemaSource {
    private static final Logger logger = Logger.getLogger(CompiledSchema.class.getName());
    public static final String PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS =
            "com.l7tech.server.schema.suppressSoftwareRecheckIfHardwareFlagsInvalidXml";
    public static final boolean SUPPRESS_FALLBACK_IF_TARARI_FAILS = Boolean.getBoolean(PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS);

    private final String targetNamespace;
    private final String systemId;
    private final Schema softwareSchema;
    private final String schemaDocument;
    private final byte[] namespaceNormalizedSchemaDocument;
    private final SchemaManagerImpl manager;
    private final Set<SchemaHandle> imports;  // Schemas that we directly use via import
    private final Set<WeakReference<CompiledSchema>> exports = new HashSet<WeakReference<CompiledSchema>>(); // Schemas that use us directly via import
    private boolean rejectedByTarari = false; // true if Tarari couldn't compile this schema
    private boolean uniqueTns = false; // true when we notice that we are the only user of this tns
    private boolean hardwareEligible = false;  // true while (and only while) all this schemas imports are themselves hardwareEligible AND this schema has a unique tns.
    private boolean loaded = false;  // true while (and only while) this schema is loaded on the hardware
    private long lastUsedTime = System.currentTimeMillis();

    CompiledSchema(String targetNamespace,
                   String systemId,
                   String schemaDocument,
                   String namespaceNormalizedSchemaDocument,
                   Schema softwareSchema,
                   SchemaManagerImpl manager,
                   Set<SchemaHandle> imports)
    {
        if (targetNamespace == null || softwareSchema == null ||
                schemaDocument == null || namespaceNormalizedSchemaDocument == null ||
                imports == null || manager == null)
            throw new NullPointerException();

        this.targetNamespace = targetNamespace;
        this.systemId = systemId;
        this.softwareSchema = softwareSchema;
        this.schemaDocument = schemaDocument.intern();
        try {
            this.namespaceNormalizedSchemaDocument = namespaceNormalizedSchemaDocument.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can't happen, it's UTF-8
        }
        this.manager = manager;
        this.imports = imports;
    }

    String getTargetNamespace() {
        return targetNamespace;
    }

    public String getSystemId() {
        return systemId;
    }

    Schema getSoftwareSchema() {
        return softwareSchema;
    }

    String getSchemaDocument() {
        return schemaDocument;
    }

    public byte[] getNamespaceNormalizedSchemaDocument() {
        return namespaceNormalizedSchemaDocument;
    }

    public boolean isRejectedByTarari() {
        return rejectedByTarari;
    }

    public void setRejectedByTarari(boolean rejectedByTarari) {
        this.rejectedByTarari = rejectedByTarari;
    }

    boolean isUniqueTns() {
        return uniqueTns;
    }

    void setUniqueTns(boolean uniqueTns) {
        this.uniqueTns = uniqueTns;
    }

    boolean isHardwareEligible() {
        return hardwareEligible;
    }

    void setHardwareEligible(boolean hardwareEligible) {
        this.hardwareEligible = hardwareEligible;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
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
        boolean tryHardware = true;

        if (schemaHandler == null || tk == null || tmc == null || targetNamespace == null)
            tryHardware = false;

        setLastUsedTime();

        try {
            manager.getReadLock().acquire();

            if (!tryHardware || !isHardwareEligible() || !isLoaded()) {
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
                // Tarari might find a validation failure if a document contained material under
                // an xs:any happens to match the TNS of other schemas in the system that are loaded into hardware,
                // even when those other schemas are totally unrelated to the particular validation task
                // currently being performed by this CompiledSchema instance.
                errorHandler.recordedErrors().clear();
                validateMessageDom(msg, isSoap, errorHandler);
                logger.info("Hardware schema validation failed. The assertion will " +
                        "fallback on software schema validation");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for read lock", e);
        } finally {
            manager.getReadLock().release();
        }

    }

    Set<SchemaHandle> getImports() {
        return Collections.unmodifiableSet(imports);
    }

    void addExport(CompiledSchema schema) {
        exports.add(new WeakReference<CompiledSchema>(schema));
    }

    /**
     * @return a set of this schema's exports.  Caller should avoid retaining references to the CompiledSchemas found within.
     */
    Set<CompiledSchema> getExports() {
        Set<CompiledSchema> set = new HashSet<CompiledSchema>(exports.size());
        for (WeakReference<CompiledSchema> ref : exports) {
            CompiledSchema schema = ref == null ? null : ref.get();
            if (schema != null) set.add(schema);
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Caller must hold the read lock.
     *
     * @param msg
     * @param soap
     * @param errorHandler
     * @throws SAXException
     * @throws IOException
     */
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
        doValidateElements( elements, errorHandler);
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
        setLastUsedTime();
        try {
            manager.getReadLock().acquire();
            doValidateElements(elementsToValidate, errorHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for schema read lock", e);
        } finally {
            manager.getReadLock().release();
        }
    }

    /**
     * Caller must hold the read lock.
     *
     * @param elementsToValidate
     * @param errorHandler
     * @throws IOException
     * @throws SAXException
     */
    private void doValidateElements(Element[] elementsToValidate, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
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

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString()).append(" ");
        if (systemId != null) sb.append("systemId=\"").append(systemId).append("\" ");
        if (targetNamespace != null) sb.append("tns=\"").append(targetNamespace).append("\" ");
        sb.append("hardware=\"").append(isHardwareEligible()).append("\" ");
        sb.append("doc=\"").append(schemaDocument.hashCode()).append("\" ");
        return sb.toString();
    }

    LSInput getLSInput() {
        LSInputImpl lsi =  new LSInputImpl();
        lsi.setStringData(schemaDocument);
        lsi.setSystemId(systemId);
        return lsi;
    }

    protected void doClose() {
        manager.closeSchema(this);
    }

    protected SchemaHandle createHandle() {
        return new SchemaHandle(this);
    }

    private void setLastUsedTime() {
        synchronized (this) {
            lastUsedTime = System.currentTimeMillis();
        }
    }

    synchronized long getLastUsedTime() {
        return lastUsedTime;
    }
}
