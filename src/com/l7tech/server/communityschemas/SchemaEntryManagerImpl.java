/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mike
 */
@Transactional(propagation=Propagation.SUPPORTS)
public class SchemaEntryManagerImpl
        extends HibernateEntityManager<SchemaEntry, EntityHeader>
        implements SchemaEntryManager
{
    private static final Logger logger = Logger.getLogger(SchemaEntryManagerImpl.class.getName());

    private final Map<Long, String> systemIdsByOid = new HashMap<Long, String>();
    private SchemaManager schemaManager;
    @SuppressWarnings( { "FieldCanBeLocal" } )
    private ApplicationListener invalidationListener; // hold reference to prevent listener getting GC'd

    public SchemaEntryManagerImpl(ApplicationEventProxy applicationEventProxy) {
        if (applicationEventProxy == null) throw new NullPointerException("missing applicationEventProxy");

        this.invalidationListener = new ApplicationListener() {
            public void onApplicationEvent(ApplicationEvent applicationEvent) {
                doOnApplicationEvent(applicationEvent);
            }
        };

        applicationEventProxy.addApplicationListener( invalidationListener );
    }

    public void setSchemaManager(SchemaManager schemaManager) {
        this.schemaManager = schemaManager;
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();
        Collection<SchemaEntry> schemaEntries = findAll();
        for (SchemaEntry entry : schemaEntries) {
            long oid = entry.getOid();
            try {
                compileAndCache(oid, entry, false);
            } catch (SAXException e) {
                logger.log(Level.WARNING, "Community schema #" + oid + " could not be compiled", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Community schema #" + oid + " could not be compiled", e);
            }
        }

        for (SchemaEntry entry : schemaEntries) {
            long oid = entry.getOid();
            try {
                validateCachedSchema( getSystemId( oid, entry ) );
            } catch (SAXException e) {
                logger.log(Level.WARNING, "Community schema #" + oid + " could not be compiled", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Community schema #" + oid + " could not be compiled", e);
            }
        }
    }

    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    @Override
    public Collection<SchemaEntry> findAll() throws FindException {
        Collection<SchemaEntry> output = super.findAll();

        // make sure the soapenv schema is always there
        output = addSoapEnv(output);

        return output;
    }

    private Collection<SchemaEntry> addSoapEnv(Collection<SchemaEntry> collection) {
        ArrayList<SchemaEntry> completeList = new ArrayList<SchemaEntry>(collection);
        completeList.add(SOAP_SCHEMA_ENTRY);
        return completeList;
    }

    /**
     * Find a schema from it's name (name column in community schema table)
     */
    @SuppressWarnings({"unchecked"})
    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    public Collection<SchemaEntry> findByName(final String schemaName) throws FindException {
        final String queryname = "from " + TABLE_NAME + " in class " + SchemaEntry.class.getName() +
                          " where " + TABLE_NAME + ".name = ?";
        Collection output = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            @Override
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Query q = session.createQuery(queryname);
                q.setString(0, schemaName);
                return q.list();
            }});

        if (SOAP_SCHEMA_NAME.equals(schemaName)) {
            output = addSoapEnv(output);
        }

        return output;
    }

    /**
     * Find a schema from it's target namespace (tns column in community schema table)
     */
    @SuppressWarnings({"unchecked"})
    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    public Collection<SchemaEntry> findByTNS(final String tns) throws FindException {
        final String querytns = "from " + TABLE_NAME + " in class " + SchemaEntry.class.getName() +
                          " where " + TABLE_NAME + ".tns = ?";
        Collection output = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            @Override
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Query q = session.createQuery(querytns);
                q.setString(0, tns);
                return q.list();
            }});

        if (SOAP_SCHEMA_TNS.equals(tns)) {
            output = addSoapEnv(output);
        }

        return output;
    }

    @Transactional(readOnly=true)
    @Override
    public SchemaEntry findByPrimaryKey(long oid) throws FindException {
        if (SOAP_SCHEMA_OID == oid)
            return SOAP_SCHEMA_ENTRY;
        return super.findByPrimaryKey(oid);
    }

    @Transactional(readOnly=true)
    @Override
    public Collection<EntityHeader> findAllHeaders() throws FindException {
        ArrayList<EntityHeader> completeList = new ArrayList<EntityHeader>(super.findAllHeaders());
        completeList.add(SOAP_SCHEMA_HEADER);
        return completeList;
    }

    @Transactional(readOnly=true)
    @Override
    public SchemaEntry findEntity(long oid) throws FindException {
        if (SOAP_SCHEMA_OID == oid)
            return SOAP_SCHEMA_ENTRY;
        return super.findEntity(oid);
    }

    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    @Override
    public long save(SchemaEntry newSchema) throws SaveException {
        if (newSchema.getOid() == SOAP_SCHEMA_OID)
            throw new SaveException("The SOAP schema cannot be saved");

        long res = super.save(newSchema);

        try {
            compileAndCache(res, newSchema);
        } catch (IOException e) {
            throw new SaveException("Schema document imports remote document that is missing or invalid: " + ExceptionUtils.getMessage(e), e);
        } catch (SAXException e) {
            throw new SaveException("Invalid schema document: " + ExceptionUtils.getMessage(e), e);
        }
        return res;
    }

    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    @Override
    public void update(SchemaEntry schemaEntry) throws UpdateException {
        if (schemaEntry.getOid() == SOAP_SCHEMA_OID)
            throw new UpdateException("The SOAP schema cannot be updated");

        super.update(schemaEntry);

        try {
            compileAndCache(schemaEntry.getOid(), schemaEntry);
        } catch (IOException e) {
            throw new UpdateException("Schema document imports missing or invalid remote document", e);
        } catch (SAXException e) {
            throw new UpdateException("Invalid schema document", e);
        }
    }

    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    @Override
    public void delete( long oid ) throws DeleteException, FindException {
        findAndDelete( oid );
    }

    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    @Override
    public void delete(SchemaEntry existingSchema) throws DeleteException {
        if (SOAP_SCHEMA_OID == existingSchema.getOid())
            throw new DeleteException("The SOAP schema cannot be deleted");
        try {
            super.delete(existingSchema);
        } finally {
            invalidateCompiledSchema(existingSchema.getOid());
        }
    }

    private void doOnApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent eieio = (EntityInvalidationEvent)event;
            if (SchemaEntry.class.isAssignableFrom(eieio.getEntityClass())) {
                new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        
                        for (long oid : eieio.getEntityIds()) {
                            try {
                                // See if it still exists (i.e. it's updated, not deleted)

                                SchemaEntry entry = findEntity(oid);
                                if (entry == null) {
                                    invalidateCompiledSchema(oid);
                                    continue;
                                }

                                compileAndCache(oid, entry);
                            } catch (FindException e) {
                                logger.log(Level.WARNING, "Couldn't find invalidated SchemaEntry", e);
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Couldn't compile updated SchemaEntry", e);
                            } catch (SAXException e) {
                                logger.log(Level.WARNING, "Couldn't compile updated SchemaEntry", e);
                            }
                        }
                    }
                });
            }
        }
    }

    private void compileAndCache(long oid, SchemaEntry entry) throws SAXException, IOException {
        compileAndCache(oid, entry, true);
    }

    private void compileAndCache(long oid, SchemaEntry entry, boolean validate) throws SAXException, IOException {
        String systemId = getSystemId( oid, entry );
        String schemaString = entry.getSchema();
        if (schemaString == null) schemaString = "";
        schemaManager.registerSchema(systemId, schemaString);

        if ( validate ) {
            validateCachedSchema( systemId );
        }

        synchronized(this) {
            systemIdsByOid.put(oid, systemId);
        }
    }

    private String getSystemId(long oid, SchemaEntry entry) {
        String systemId = entry.getName();
        if (systemId == null) systemId = "policy:SchemaEntry:" + oid;
        return systemId;
    }

    private void validateCachedSchema( String systemId ) throws SAXException, IOException {
        // Make sure it compiles
        SchemaHandle handle = schemaManager.getSchemaByUrl(systemId);
        handle.close();
    }

    /**
     * Ensure that no SchemaEntry with this oid has a corresponding cached CompiledSchema.
     * @param oid the OID of the schema entry to invalidate
     * @return true if there was a matching cached CompiledSchema and it was removed.
     */
    private boolean invalidateCompiledSchema(long oid) {
        // Cleanup old version
        synchronized(this) {
            String systemId = systemIdsByOid.get(oid);
            if (systemId != null) schemaManager.unregisterSchema(systemId);
        }
        return true;
    }


    @Override
    public Class<SchemaEntry> getImpClass() {
        return SchemaEntry.class;
    }

    @Override
    public Class<SchemaEntry> getInterfaceClass() {
        return SchemaEntry.class;
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEMA_ENTRY;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    private static final String TABLE_NAME = "community_schemas";
    private static final long SOAP_SCHEMA_OID = -3L;
    private static final String SOAP_SCHEMA_NAME = "soapenv";
    private static final String SOAP_SCHEMA_TNS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_SCHEMA = "<?xml version='1.0' encoding='UTF-8' ?>\n" +
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "           xmlns:tns=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           targetNamespace=\"http://schemas.xmlsoap.org/soap/envelope/\" >\n" +
            "  <!-- Envelope, header and body -->\n" +
            "  <xs:element name=\"Envelope\" type=\"tns:Envelope\" />\n" +
            "  <xs:complexType name=\"Envelope\" >\n" +
            "    <xs:sequence>\n" +
            "      <xs:element ref=\"tns:Header\" minOccurs=\"0\" />\n" +
            "      <xs:element ref=\"tns:Body\" minOccurs=\"1\" />\n" +
            "      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n" +
            "  </xs:complexType>\n" +
            "  <xs:element name=\"Header\" type=\"tns:Header\" />\n" +
            "  <xs:complexType name=\"Header\" >\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n" +
            "  </xs:complexType>\n" +
            "  <xs:element name=\"Body\" type=\"tns:Body\" />\n" +
            "  <xs:complexType name=\"Body\" >\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" >\n" +
            "          <xs:annotation>\n" +
            "            <xs:documentation>\n" +
            "                  Prose in the spec does not specify that attributes are allowed on the Body element\n" +
            "                </xs:documentation>\n" +
            "          </xs:annotation>\n" +
            "        </xs:anyAttribute>\n" +
            "  </xs:complexType>\n" +
            "  <!-- Global Attributes.  The following attributes are intended to be usable via qualified attribute names on any complex type referencing them.  -->\n" +
            "  <xs:attribute name=\"mustUnderstand\" >\n" +
            "     <xs:simpleType>\n" +
            "     <xs:restriction base='xs:boolean'>\n" +
            "           <xs:pattern value='0|1' />\n" +
            "         </xs:restriction>\n" +
            "   </xs:simpleType>\n" +
            "  </xs:attribute>\n" +
            "  <xs:attribute name=\"actor\" type=\"xs:anyURI\" />\n" +
            "  <xs:simpleType name=\"encodingStyle\" >\n" +
            "    <xs:annotation>\n" +
            "          <xs:documentation>\n" +
            "            'encodingStyle' indicates any canonicalization conventions followed in the contents of the containing element.  For example, the value 'http://schemas.xmlsoap.org/soap/encoding/' indicates the pattern described in SOAP specification\n" +
            "          </xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "    <xs:list itemType=\"xs:anyURI\" />\n" +
            "  </xs:simpleType>\n" +
            "  <xs:attribute name=\"encodingStyle\" type=\"tns:encodingStyle\" />\n" +
            "  <xs:attributeGroup name=\"encodingStyle\" >\n" +
            "    <xs:attribute ref=\"tns:encodingStyle\" />\n" +
            "  </xs:attributeGroup>" +
            "  <xs:element name=\"Fault\" type=\"tns:Fault\" />\n" +
            "  <xs:complexType name=\"Fault\" final=\"extension\" >\n" +
            "    <xs:annotation>\n" +
            "          <xs:documentation>\n" +
            "            Fault reporting structure\n" +
            "          </xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "    <xs:sequence>\n" +
            "      <xs:element name=\"faultcode\" type=\"xs:QName\" />\n" +
            "      <xs:element name=\"faultstring\" type=\"xs:string\" />\n" +
            "      <xs:element name=\"faultactor\" type=\"xs:anyURI\" minOccurs=\"0\" />\n" +
            "      <xs:element name=\"detail\" type=\"tns:detail\" minOccurs=\"0\" />\n" +
            "    </xs:sequence>\n" +
            "  </xs:complexType>\n" +
            "  <xs:complexType name=\"detail\">\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" />\n" +
            "  </xs:complexType>\n" +
            "</xs:schema>";


    public static final SchemaEntry SOAP_SCHEMA_ENTRY = new SchemaEntry();
    static {
        SOAP_SCHEMA_ENTRY.setSchema(SOAP_SCHEMA);
        SOAP_SCHEMA_ENTRY.setName(SOAP_SCHEMA_NAME);
        SOAP_SCHEMA_ENTRY.setTns(SOAP_SCHEMA_TNS);
        SOAP_SCHEMA_ENTRY.setOid(SOAP_SCHEMA_OID);
        SOAP_SCHEMA_ENTRY.setSystem(true);
    }
    public static final EntityHeader SOAP_SCHEMA_HEADER = new EntityHeader(Long.toString(SOAP_SCHEMA_OID),
                                                                           EntityType.SCHEMA_ENTRY,
                                                                           SOAP_SCHEMA_ENTRY.getName(),
                                                                           "");
}
