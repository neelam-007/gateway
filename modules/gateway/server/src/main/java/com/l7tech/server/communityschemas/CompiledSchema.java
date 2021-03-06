package com.l7tech.server.communityschemas;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.Message;
import com.l7tech.message.ValidationTarget;
import com.l7tech.server.util.AbstractReferenceCounted;
import com.l7tech.util.*;
import com.l7tech.xml.ElementCursor;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

final class CompiledSchema extends AbstractReferenceCounted<SchemaHandle> {
    private static final Logger logger = Logger.getLogger(CompiledSchema.class.getName());

    private static Map<String,Long> systemIdGeneration = new HashMap<String,Long>();

    private static final AtomicBoolean needMarkUsedPass = new AtomicBoolean(true);
    private static final ConcurrentMap<WeakReference<CompiledSchema>, Object> schemasToMarkAsUsed = new ConcurrentHashMap<WeakReference<CompiledSchema>, Object>();

    private final WeakReference<CompiledSchema> schemaRef = new WeakReference<CompiledSchema>(this);

    private final String targetNamespace; // could be null
    private final String[] payloadElementNames;
    private final String systemId;
    private final String schemaDocument;
    private final String schemaSourceResolverId;
    private final SchemaManagerImpl manager;
    private final String tnsGen;
    private final boolean transientSchema; // if true, this schema was pulled down over the network and should be discarded when it hasn't been used for a while
    private final Map<String, SchemaHandle> imports; // Full URL -> Handles of schemas that we import directly
    private final Map<String, SchemaHandle> includes; // Full URL -> Handles of schemas that we include/redefine directly
    private final Map<String, SchemaHandle> dependencies; // Full URL -> Handles of schemas that we depend on directly
    private final Map<String, Reference<CompiledSchema>> exports = new WeakHashMap<String, Reference<CompiledSchema>>();
    private final Schema softwareSchema;
    private final long createdTime = System.currentTimeMillis();

    // These properties are synchronized by the schema manager
    private boolean conflictingTns = true; // false when we notice that this tns does not conflict

    private final AtomicLong lastUsedTime = new AtomicLong(createdTime);

    CompiledSchema( final String targetNamespace,
                    final String systemId,
                    final String schemaDocument,
                    final String schemaSourceResolverId,
                    final Element namespaceNormalizedSchemaDocument,
                    final Schema softwareSchema,
                    final SchemaManagerImpl manager,
                    final Map<String, SchemaHandle> imports,
                    final Map<String, SchemaHandle> includes,
                    final boolean transientSchema ) throws IOException
    {
        if ( softwareSchema == null || schemaDocument == null || schemaSourceResolverId ==null ||
             namespaceNormalizedSchemaDocument == null || imports == null || includes == null || manager == null ||
             systemId == null )
            throw new NullPointerException();

        this.transientSchema = transientSchema;
        this.targetNamespace = targetNamespace;
        this.systemId = systemId;
        this.softwareSchema = softwareSchema;
        this.schemaDocument = schemaDocument.intern();
        this.schemaSourceResolverId = schemaSourceResolverId;
        this.manager = manager;
        this.imports = imports;
        this.includes = includes;
        this.dependencies = new HashMap<String,SchemaHandle>();
        this.dependencies.putAll( imports ); // If an import and include have the same system identifier they must be the same schema
        this.dependencies.putAll( includes );
        this.tnsGen = "{" + nextSystemIdGeneration(systemId) + "} " + systemId;
        this.payloadElementNames = getPayloadElementNames( namespaceNormalizedSchemaDocument, includes.values() );
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

    @Override
    public SchemaHandle ref() {
        return super.ref();
    }

    public boolean isTransientSchema() {
        return transientSchema;
    }

    boolean isTransientlyReferencedSchema() {
        boolean transientSchema = this.transientSchema;
        
        if ( transientSchema ) { // check that all usages are transient
            for ( final CompiledSchema schema : getExports() ) {
                transientSchema = schema.isTransientlyReferencedSchema();
                if ( !transientSchema ) break;
            }
        }

        return transientSchema;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getSystemId() {
        return systemId;
    }

    String getSchemaDocument() {
        return schemaDocument;
    }

    public boolean isInclude() {
        boolean included = false;
        boolean imported = false;
        for ( final CompiledSchema schema : getExports() ) {
            if ( schema.includes.containsKey( systemId ) ) {
                included = true;
            } else {
                imported = true;
            }
        }
        return included && !imported;
    }

    boolean isConflictingTns() {
        return conflictingTns;
    }

    void setConflictingTns(boolean conflictingTns ) {
        this.conflictingTns = conflictingTns;
    }

    /**
     * Validate the specified message with this schema.
     *
     * @param msg           the message to validate.  Must not be null.
     * @param errorHandler  error handler for accumulating error information.  Must not be null.
     * @throws IOException          if the message cannot be read
     * @throws SAXException         if the message is found to be invalid
     */
    void validateMessage(Message msg, ValidationTarget validationTarget, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
        if (isClosed()) throw new IllegalStateException("CompiledSchema has already been closed");

        schemasToMarkAsUsed.put(schemaRef, Boolean.TRUE);
        needMarkUsedPass.set(true);

        final Lock readLock = manager.getReadLock();
        readLock.lock();
        try {
            doValidateElements(validationTarget.elementsToValidate(msg.getXmlKnob().getDocumentReadOnly()), errorHandler);
        } catch (InvalidDocumentFormatException e) {
            throw new SAXException("Error getting elements to validate for " + validationTarget, e);
        } finally {
            readLock.unlock();
        }
    }

    Map<String, SchemaHandle> getDependencies() {
        return dependencies;
    }

    void addExport(CompiledSchema schema) {
        exports.put(schema.getSystemId(), new WeakReference<CompiledSchema>(schema));
    }

    /**
     * Get the set of schemas that depend on this schema.
     *
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

    private String[] getPayloadElementNames( final Element schema, final Collection<SchemaHandle> includes  ) {
        final Set<String> elementNames = new TreeSet<String>();
        try {
            final String[] directGlobalElements = XmlUtil.getSchemaGlobalElements( schema );
            elementNames.addAll( Arrays.asList( directGlobalElements ) );                   
        } catch ( XmlUtil.BadSchemaException e ) {
            logger.log( Level.WARNING, 
                    "Error processing global elements for schema '"+systemId+"', '"+ ExceptionUtils.getMessage( e )+"'",
                    ExceptionUtils.getDebugException(e));
        }

        for ( final SchemaHandle include : includes ) {
            elementNames.addAll( Arrays.asList( include.getCompiledSchema().payloadElementNames ) );
        }

        return elementNames.toArray( new String[ elementNames.size() ] );
    }

    /**
     * Validate the specified elements.
     * Caller must hold the read lock.
     *
     * @param errorHandler  error handler for accumulating errors.  Must not be null.
     * @throws IOException   if an input element could not be read.
     * @throws SAXException  if at least one element was found to be invalid.  Contains the very first validation error;
     *                       the error handler will have been told about this and any subsequent errors.
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

    @Override
    protected void doClose() {
        // Close dependencies
        for (SchemaHandle impHandle : dependencies.values())
            impHandle.close();
        dependencies.clear();
        imports.clear();
        includes.clear();
        exports.clear();
        manager.closeSchema(this);
    }

    @Override
    protected SchemaHandle createHandle() {
        return new SchemaHandle(this);
    }

    private void setLastUsedTime(long time) {
        lastUsedTime.set(time);

        for ( final SchemaHandle handle : dependencies.values() ) {
            final CompiledSchema schema = handle.getTarget();
            if (schema == null || schema.isClosed()) continue;
            schema.setLastUsedTime(time);
        }
    }

    long getLastUsedTime() {
        return lastUsedTime.get();
    }

    long getCreatedTime() {
        return createdTime;
    }

    String getSchemaSourceResolverId() {
        return schemaSourceResolverId;
    }

    static {
        final String threadName = "Schema Usage Time Marker Thread";
        Thread thread = new Thread(threadName) {
            @Override
            public void run() {
                try {
                    runUsageTimeMarkerThread();
                } catch (InterruptedException e) {
                    logger.info(threadName + " interrupted, exiting");
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Uncaught exception in " + threadName + ": " + ExceptionUtils.getMessage(t), t);
                }
            }

            private void runUsageTimeMarkerThread() throws InterruptedException {
                //noinspection InfiniteLoopStatement
                for (;;) {
                    Thread.sleep(4057);
                    maybeMarkUsage();
                }
            }

            private void maybeMarkUsage() {
                while (needMarkUsedPass.getAndSet(false)) {
                    Iterator<WeakReference<CompiledSchema>> sit = schemasToMarkAsUsed.keySet().iterator();
                    while (sit.hasNext()) {
                        WeakReference<CompiledSchema> reference = sit.next();
                        sit.remove();

                        CompiledSchema target = reference.get();
                        if (target != null && !target.isClosed())
                            target.setLastUsedTime(System.currentTimeMillis());
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }
}
