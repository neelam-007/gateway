/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.message.Message;
import com.l7tech.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.TarariMessageContext;
import com.l7tech.xml.tarari.TarariSchemaHandler;
import com.l7tech.xml.tarari.TarariSchemaSource;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.util.AbstractReferenceCounted;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

final class CompiledSchema extends AbstractReferenceCounted<SchemaHandle> implements TarariSchemaSource {
    private static final Logger logger = Logger.getLogger(CompiledSchema.class.getName());

    private static Map<String,Long> systemIdGeneration = new HashMap<String,Long>();

    private final String targetNamespace;
    private final String systemId;
    private final String schemaDocument;
    private final byte[] namespaceNormalizedSchemaDocument;
    private final SchemaManagerImpl manager;
    private final String tnsGen;
    private final boolean transientSchema; // if true, this schema was pulled down over the network and should be discarded when it hasn't been used for a while
    private final Map<String, SchemaHandle> imports; // Full URL -> Handles of schemas that we import directly
    private final Map<String, Reference<CompiledSchema>> exports = new WeakHashMap<String, Reference<CompiledSchema>>();
    private final Schema softwareSchema;
    private final boolean softwareFallback;

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
                   Map<String, SchemaHandle> imports,
                   boolean transientSchema,
                   boolean fallback)
    {
        if (softwareSchema == null ||
                schemaDocument == null || namespaceNormalizedSchemaDocument == null ||
                imports == null || manager == null || systemId == null)
            throw new NullPointerException();

        this.transientSchema = transientSchema;
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
        this.tnsGen = "{" + nextSystemIdGeneration(systemId) + "} " + systemId;
        this.softwareFallback = fallback;
    }

    private synchronized static long nextSystemIdGeneration(String systemId) {
        Long gen = systemIdGeneration.get(systemId);
        if (gen == null)
            gen = 0L;
        else
            gen = gen + 1;
        systemIdGeneration.put(systemId, gen);
        return gen;
    }

    public SchemaHandle ref() {
        return super.ref();
    }

    public boolean isTransientSchema() {
        return transientSchema;
    }

    String getTargetNamespace() {
        return targetNamespace;
    }

    public String getSystemId() {
        return systemId;
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

        if (schemaHandler == null || tk == null || tmc == null || (targetNamespace == null || targetNamespace.length() < 1))
            tryHardware = false;

        setLastUsedTime();

        final Lock readLock = manager.getReadLock();
        readLock.lock();
        try {
            if (tryHardware && isHardwareEligible() && isLoaded()) {

                // Do it in Tarari
                try {
                    validateMessageWithTarari(schemaHandler, tk, isSoap, tmc);

                    // Success
                    return;

                } catch (SAXException e) {
                    if (!softwareFallback) {
                        // Don't recheck with software -- just record the failure and be done
                        throw e;
                    }

                    // Recheck failed validations with software, in case the Tarari failure was spurious
                    // Tarari might find a validation failure if a document contained material under
                    // an xs:any happens to match the TNS of other schemas in the system that are loaded into hardware,
                    // even when those other schemas are totally unrelated to the particular validation task
                    // currently being performed by this CompiledSchema instance.
                    errorHandler.recordedErrors().clear();
                    logger.info("Hardware schema validation failed. The assertion will " +
                            "fallback on software schema validation");
                    /* FALLTHROUGH and do it in software */
                }
            }
            // Hardware impossible or inappropriate in this case, or was tried and failed
            /* FALLTHROUGH and do it in softare */

        } finally {
            readLock.unlock();
        }

        // Try to run the validation in software
        readLock.lock();
        try {
            validateMessageDom(msg, isSoap, errorHandler);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Assert message validates with Tarari.  Throws on failure.  Returns normally on success.
     * Caller must hvae checked that schema is currently loaded while holding the read lock,
     * and must still hold the read lock.
     */
    private void validateMessageWithTarari(TarariSchemaHandler schemaHandler,
                                           TarariKnob tk,
                                           boolean soap,
                                           TarariMessageContext tmc)
            throws SAXException, IOException, NoSuchPartException
    {
        boolean result = schemaHandler.validate(tk.getContext());
        if (!result) throw new SAXException("Validation failed: message was not valid");

        // Hardware schema val succeeded.
        if (soap) {
            checkPayloadNamespaceUris(tk);
        } else {
            checkRootNamespace(tmc);
        }

        // Success
    }

    Map<String, SchemaHandle> getImports() {
        return imports;
    }

    void addExport(CompiledSchema schema) {
        exports.put(schema.getSystemId(), new WeakReference<CompiledSchema>(schema));
    }

    /**
     * @return a set of this schema's current exports.  Caller should avoid retaining references to the CompiledSchemas found within.
     */
    Set<CompiledSchema> getExports() {
        HashSet<CompiledSchema> ret = new HashSet<CompiledSchema>(exports.size());
        for (Reference<CompiledSchema> ref : exports.values()) {
            if (ref == null) continue;
            CompiledSchema schema = ref.get();
            if (schema == null || schema.isClosed()) continue;
            ret.add(schema);
        }
        return ret;
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
            } catch ( InvalidDocumentFormatException e) {
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

        doValidateElements(elements, errorHandler);
    }

    /**
     * Ensure that all payload namespaces of this SOAP message match the tarariNamespaceUri.
     */
    private void checkPayloadNamespaceUris(TarariKnob tk) throws SAXException {
        QName payloadNames[] = tk.getSoapInfo().getPayloadNames();

        if (payloadNames == null || payloadNames.length < 1)
            throw new SAXException("Validation failed: message did not include a recognized payload namespace URI");

        // They must all match up
        if (targetNamespace != null) {
            for (QName payloadName : payloadNames) {
                if (!targetNamespace.equals(payloadName.getNamespaceURI()))
                    throw new SAXException("Validation failed: message contained an unexpected payload namespace URI: " + payloadName);
            }
        } else {
            for (QName payloadName : payloadNames) {
                String ns = payloadName.getNamespaceURI();
                if (ns != null && ns.length() > 0)
                    throw new SAXException("Validation failed: message contained an unexpected payload namespace URI: " + payloadName);
            }
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
        if (targetNamespace != null) {
            if (!targetNamespace.equals(docNs))
                throw new SAXException("Hardware schema validation succeeded against non-SOAP message, " +
                        "but the document element namespace URI did not match the assertion at hand.");
        } else {
            if (docNs != null && docNs.length() > 0)
                throw new SAXException("Hardware schema validation succeeded against non-SOAP message, " +
                        "but the document element namespace URI did not match the assertion at hand.");
        }
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
        final Lock readLock = manager.getReadLock();
        readLock.lock();
        try {
            doValidateElements(elementsToValidate, errorHandler);
        } finally {
            readLock.unlock();
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
        v.setResourceResolver( XmlUtil.getSafeLSResourceResolver());

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
        StringBuilder sb = new StringBuilder("CompiledSchema");
        //if (systemId != null) sb.append("systemId=\"").append(systemId).append("\" ");
        //if (targetNamespace != null) sb.append("tns=\"").append(targetNamespace).append("\" ");
        //sb.append("hardware=\"").append(isHardwareEligible()).append("\" ");
        //sb.append("doc=\"").append(schemaDocument.hashCode()).append("\" ");
        sb.append(HexUtils.to8NybbleHexString(System.identityHashCode(this)));
        return sb.toString();
    }

    String getTnsGen() {
        return tnsGen;
    }

    // Overridden to open up access to the communityschemas package, specifically SchemaManagerImpl
    protected boolean isClosed() {
        return super.isClosed();
    }

    protected void doClose() {
        // Close imported references
        for (SchemaHandle impHandle : imports.values())
            impHandle.close();
        imports.clear();
        exports.clear();
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
