/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;
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
import java.util.*;
import java.util.logging.Logger;

public final class CompiledSchema extends AbstractReferenceCounted<SchemaHandle> implements TarariSchemaSource {
    private static final Logger logger = Logger.getLogger(CompiledSchema.class.getName());
    public static final String PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS =
            "com.l7tech.server.schema.suppressSoftwareRecheckIfHardwareFlagsInvalidXml";
    public static final boolean SUPPRESS_FALLBACK_IF_TARARI_FAILS = Boolean.getBoolean(PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS);

    private static Map<String,Long> systemIdGeneration = new HashMap<String,Long>();

    private final String targetNamespace;
    private final String systemId;
    private final String schemaDocument;
    private final byte[] namespaceNormalizedSchemaDocument;
    private final SchemaManagerImpl manager;
    private final String tnsGen;

    // The sets are final but the contents will change when the schema is recompiled
    private final Set<SchemaHandle> imports;  // Schemas that we directly use via import
    private final Map<CompiledSchema, Object> exports = new WeakHashMap<CompiledSchema, Object>(); // Schemas that use us directly via import
    private Schema softwareSchema;

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
        this.tnsGen = "{" + nextSystemIdGeneratioun(systemId) + "} " + systemId;
    }

    private synchronized static long nextSystemIdGeneratioun(String systemId) {
        Long gen = systemIdGeneration.get(systemId);
        if (gen == null)
            gen = 0L;
        else
            gen = gen + 1;
        systemIdGeneration.put(systemId, gen);
        return gen;
    }

    String getTargetNamespace() {
        return targetNamespace;
    }

    public String getSystemId() {
        return systemId;
    }

    private synchronized Schema getSoftwareSchema() {
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

            if (tryHardware && isHardwareEligible() && isLoaded()) {

                // Do it in Tarari
                try {
                    validateMessageWithTarari(schemaHandler, tk, isSoap, tmc);

                    // Success
                    return;

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
                    logger.info("Hardware schema validation failed. The assertion will " +
                            "fallback on software schema validation");
                    /* FALLTHROUGH and do it in software */
                }
            }
            // Hardware impossible or inappropriate in this case, or was tried and failed
            /* FALLTHROUGH and do it in softare */

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for read lock", e);
        } finally {
            manager.getReadLock().release();
        }

        // Try to run the validation in software
        Schema softwareSchema;
        for (;;) {
            try {
                manager.getReadLock().acquire();
                softwareSchema = getSoftwareSchema();
                if (softwareSchema != null) {
                    validateMessageDom(softwareSchema, msg, isSoap, errorHandler);
                    return;
                }
                /* FALLTHROUGH and recompile this schema */
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for schema read lock", e);
            } finally {
                manager.getReadLock().release();
            }

            //noinspection ConstantConditions
            assert softwareSchema == null;
            recompile();
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
        return;
    }

    Set<SchemaHandle> getImports() {
        return imports;
    }

    void addExport(CompiledSchema schema) {
        exports.put(schema, null);
    }

    /**
     * @return a set of this schema's exports.  Caller should avoid retaining references to the CompiledSchemas found within.
     */
    Set<CompiledSchema> getExports() {
        HashSet<CompiledSchema> ret = new HashSet<CompiledSchema>(exports.size());
        for (CompiledSchema schema : exports.keySet()) {
            if (schema != null) ret.add(schema);
        }
        return ret;
    }

    /**
     * Caller must hold the read lock.
     *
     * @param softwareSchema must not be null
     * @param msg
     * @param soap
     * @param errorHandler
     * @throws SAXException
     * @throws IOException
     */
    private void validateMessageDom(Schema softwareSchema, Message msg, boolean soap, SchemaValidationErrorHandler errorHandler) throws SAXException, IOException {
        if (softwareSchema == null) throw new NullPointerException();
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

        doValidateElements(softwareSchema, elements, errorHandler);
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
        Schema softwareSchema;
        for (;;) {
            try {
                manager.getReadLock().acquire();
                softwareSchema = getSoftwareSchema();
                if (softwareSchema != null) {
                    doValidateElements(softwareSchema, elementsToValidate, errorHandler);
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for schema read lock", e);
            } finally {
                manager.getReadLock().release();
            }

            //noinspection ConstantConditions
            assert softwareSchema == null;

            recompile();
        }
    }

    /** Recompile this schema and any child schemas.  */
    private void recompile() {
        if (getSoftwareSchema() != null)
            return;

        // TODO
        throw new UnsupportedOperationException("TODO recompile");
    }

    /**
     * Trigger recompilation of this schema and all children next time any of them are used.  Caller
     * must hold the global schema write lock.
     */
    void invalidateSoftwareSchema() {
        Set<SchemaHandle> imports = getImports();
        for (SchemaHandle handle : imports)
            handle.getCompiledSchema().invalidateSoftwareSchema();
        setHardwareEligible(false);
        synchronized (this) {
            softwareSchema = null;
        }
    }

    /**
     * Caller must hold the read lock.
     *
     * @param softwareSchema the software schema to use.  Must not be null.
     * @param elementsToValidate
     * @param errorHandler
     * @throws IOException
     * @throws SAXException
     */
    private void doValidateElements(Schema softwareSchema, Element[] elementsToValidate, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
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
