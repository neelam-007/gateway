/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.server.HibernateEntityManager;
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
        output = addSoapEnv(output, null, null);

        return output;
    }

    private Collection<SchemaEntry> addSoapEnv( final Collection<SchemaEntry> collection, final String name, final String tns ) {
        ArrayList<SchemaEntry> completeList = new ArrayList<SchemaEntry>(collection);

        if ( schemaMatch( XMLNS_SCHEMA_NAME, XMLNS_SCHEMA_TNS, name, tns ) ) {
            completeList.add(XMLNS_SCHEMA_ENTRY);
        }

        if ( schemaMatch( SOAP11_SCHEMA_NAME, SOAP11_SCHEMA_TNS, name, tns ) ) {
            completeList.add(SOAP11_SCHEMA_ENTRY);
        }

        if ( schemaMatch( SOAP12_SCHEMA_NAME, SOAP12_SCHEMA_TNS, name, tns ) ) {
            completeList.add(SOAP12_SCHEMA_ENTRY);
        }

        return completeList;
    }

    private boolean schemaMatch( final String targetName, final String targetTns, final String name, final String tns ) {
        boolean match = false;

        if ( (name==null || name.equals(targetName)) &&
            ((tns==null || tns.equals(targetTns))) ) {
            match = true;
        }

        return match;
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

        output = addSoapEnv(output, schemaName, null);

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

        output = addSoapEnv(output, null, tns);

        return output;
    }

    @Transactional(readOnly=true)
    @Override
    public SchemaEntry findByPrimaryKey(long oid) throws FindException {
        SchemaEntry result = getInternalSchema(oid);
        return result != null ? result : super.findByPrimaryKey(oid);
    }

    private SchemaEntry getInternalSchema(long oid) {
        if (SOAP11_SCHEMA_OID == oid) return SOAP11_SCHEMA_ENTRY;
        else if (SOAP12_SCHEMA_OID == oid) return SOAP12_SCHEMA_ENTRY;
        else if (XMLNS_SCHEMA_OID == oid) return XMLNS_SCHEMA_ENTRY;
        else return null;
    }

    @Transactional(readOnly=true)
    @Override
    public EntityHeaderSet<EntityHeader> findAllHeaders() throws FindException {
        EntityHeaderSet<EntityHeader> completeList = new EntityHeaderSet<EntityHeader>();
        completeList.addAll(super.findAllHeaders());
        completeList.add(SOAP11_SCHEMA_HEADER);
        return completeList;
    }

    @Transactional(readOnly=true)
    @Override
    public SchemaEntry findEntity(long oid) throws FindException {
        SchemaEntry result = getInternalSchema(oid);
        return result != null ? result : super.findEntity(oid);
    }

    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    @Override
    public long save(SchemaEntry newSchema) throws SaveException {
        long oid = newSchema.getOid();
        if (SOAP11_SCHEMA_OID == oid || SOAP12_SCHEMA_OID == oid || XMLNS_SCHEMA_OID == oid)
            throw new SaveException("Internal schema cannot be saved");

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
        long oid = schemaEntry.getOid();
        if (SOAP11_SCHEMA_OID == oid || SOAP12_SCHEMA_OID == oid || XMLNS_SCHEMA_OID == oid)
            throw new UpdateException("Internal schema cannot be updated");

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
        long oid = existingSchema.getOid();
        if (SOAP11_SCHEMA_OID == oid || SOAP12_SCHEMA_OID == oid || XMLNS_SCHEMA_OID == oid)
            throw new DeleteException("Internal schema cannot be deleted");
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
        return UniqueType.NAME;
    }

    private static final String TABLE_NAME = "community_schemas";

    private static final long SOAP11_SCHEMA_OID = -3L;
    private static final String SOAP11_SCHEMA_NAME = "soapenv";
    private static final String SOAP11_SCHEMA_TNS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP11_SCHEMA = "<?xml version='1.0' encoding='UTF-8' ?>\n" +
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

    private static final long SOAP12_SCHEMA_OID = -4L;
    private static final String SOAP12_SCHEMA_NAME = "soapenv12";
    private static final String SOAP12_SCHEMA_TNS = "http://www.w3.org/2003/05/soap-envelope";
    private static final String SOAP12_SCHEMA = "<!-- Schema defined in the SOAP Version 1.2 Part 1 specification\n" +
        "     Recommendation:\n" +
        "     http://www.w3.org/TR/2003/REC-soap12-part1-20030624/\n" +
        "     $Id$\n" +
        "\n" +
        "     Copyright (C)2003 W3C(R) (MIT, ERCIM, Keio), All Rights Reserved.\n" +
        "     W3C viability, trademark, document use and software licensing rules\n" +
        "     apply.\n" +
        "     http://www.w3.org/Consortium/Legal/\n" +
        "\n" +
        "     This document is governed by the W3C Software License [1] as\n" +
        "     described in the FAQ [2].\n" +
        "\n" +
        "     [1] http://www.w3.org/Consortium/Legal/copyright-software-19980720\n" +
        "     [2] http://www.w3.org/Consortium/Legal/IPR-FAQ-20000620.html#DTD\n" +
        "-->\n" +
        "\n" +
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
        "           xmlns:tns=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
        "           targetNamespace=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
        "\t\t   elementFormDefault=\"qualified\" >\n" +
        "\n" +
        "  <xs:import namespace=\"http://www.w3.org/XML/1998/namespace\" \n" +
        "             schemaLocation=\"xmlnamespace\"/>\n" +
        "\n" +
        "  <!-- Envelope, header and body -->\n" +
        "  <xs:element name=\"Envelope\" type=\"tns:Envelope\" />\n" +
        "  <xs:complexType name=\"Envelope\" >\n" +
        "    <xs:sequence>\n" +
        "      <xs:element ref=\"tns:Header\" minOccurs=\"0\" />\n" +
        "      <xs:element ref=\"tns:Body\" minOccurs=\"1\" />\n" +
        "    </xs:sequence>\n" +
        "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <xs:element name=\"Header\" type=\"tns:Header\" />\n" +
        "  <xs:complexType name=\"Header\" >\n" +
        "    <xs:annotation>\n" +
        "\t  <xs:documentation>\n" +
        "\t  Elements replacing the wildcard MUST be namespace qualified, but can be in the targetNamespace\n" +
        "\t  </xs:documentation>\n" +
        "\t</xs:annotation>\n" +
        "    <xs:sequence>\n" +
        "      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\"  />\n" +
        "    </xs:sequence>\n" +
        "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n" +
        "  </xs:complexType>\n" +
        "  \n" +
        "  <xs:element name=\"Body\" type=\"tns:Body\" />\n" +
        "  <xs:complexType name=\"Body\" >\n" +
        "    <xs:sequence>\n" +
        "      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\n" +
        "    </xs:sequence>\n" +
        "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <!-- Global Attributes.  The following attributes are intended to be\n" +
        "  usable via qualified attribute names on any complex type referencing\n" +
        "  them.  -->\n" +
        "  <xs:attribute name=\"mustUnderstand\" type=\"xs:boolean\" default=\"0\" />\n" +
        "  <xs:attribute name=\"relay\" type=\"xs:boolean\" default=\"0\" />\n" +
        "  <xs:attribute name=\"role\" type=\"xs:anyURI\" />\n" +
        "\n" +
        "  <!-- 'encodingStyle' indicates any canonicalization conventions\n" +
        "  followed in the contents of the containing element.  For example, the\n" +
        "  value 'http://www.w3.org/2003/05/soap-encoding' indicates the pattern\n" +
        "  described in the SOAP Version 1.2 Part 2: Adjuncts Recommendation -->\n" +
        "\n" +
        "  <xs:attribute name=\"encodingStyle\" type=\"xs:anyURI\" />\n" +
        "\n" +
        "  <xs:element name=\"Fault\" type=\"tns:Fault\" />\n" +
        "  <xs:complexType name=\"Fault\" final=\"extension\" >\n" +
        "    <xs:annotation>\n" +
        "\t  <xs:documentation>\n" +
        "\t    Fault reporting structure\n" +
        "\t  </xs:documentation>\n" +
        "\t</xs:annotation>\n" +
        "    <xs:sequence>\n" +
        "      <xs:element name=\"Code\" type=\"tns:faultcode\" />\n" +
        "      <xs:element name=\"Reason\" type=\"tns:faultreason\" />\n" +
        "      <xs:element name=\"Node\" type=\"xs:anyURI\" minOccurs=\"0\" />\n" +
        "\t  <xs:element name=\"Role\" type=\"xs:anyURI\" minOccurs=\"0\" />\n" +
        "      <xs:element name=\"Detail\" type=\"tns:detail\" minOccurs=\"0\" />\n" +
        "    </xs:sequence>\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <xs:complexType name=\"faultreason\" >\n" +
        "    <xs:sequence>\n" +
        "\t  <xs:element name=\"Text\" type=\"tns:reasontext\" \n" +
        "                  minOccurs=\"1\"  maxOccurs=\"unbounded\" />\n" +
        "\t</xs:sequence>\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <xs:complexType name=\"reasontext\" >\n" +
        "    <xs:simpleContent>\n" +
        "\t  <xs:extension base=\"xs:string\" >\n" +
        "\t    <xs:attribute ref=\"xml:lang\" use=\"required\" />\n" +
        "\t  </xs:extension>\n" +
        "\t</xs:simpleContent>\n" +
        "  </xs:complexType>\n" +
        "  \n" +
        "  <xs:complexType name=\"faultcode\">\n" +
        "    <xs:sequence>\n" +
        "      <xs:element name=\"Value\"\n" +
        "                  type=\"tns:faultcodeEnum\"/>\n" +
        "      <xs:element name=\"Subcode\"\n" +
        "                  type=\"tns:subcode\"\n" +
        "                  minOccurs=\"0\"/>\n" +
        "    </xs:sequence>\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <xs:simpleType name=\"faultcodeEnum\">\n" +
        "    <xs:restriction base=\"xs:QName\">\n" +
        "      <xs:enumeration value=\"tns:DataEncodingUnknown\"/>\n" +
        "      <xs:enumeration value=\"tns:MustUnderstand\"/>\n" +
        "      <xs:enumeration value=\"tns:Receiver\"/>\n" +
        "      <xs:enumeration value=\"tns:Sender\"/>\n" +
        "      <xs:enumeration value=\"tns:VersionMismatch\"/>\n" +
        "    </xs:restriction>\n" +
        "  </xs:simpleType>\n" +
        "\n" +
        "  <xs:complexType name=\"subcode\">\n" +
        "    <xs:sequence>\n" +
        "      <xs:element name=\"Value\"\n" +
        "                  type=\"xs:QName\"/>\n" +
        "      <xs:element name=\"Subcode\"\n" +
        "                  type=\"tns:subcode\"\n" +
        "                  minOccurs=\"0\"/>\n" +
        "    </xs:sequence>\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <xs:complexType name=\"detail\">\n" +
        "    <xs:sequence>\n" +
        "      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\"  />\n" +
        "    </xs:sequence>\n" +
        "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" /> \n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <!-- Global element declaration and complex type definition for header entry returned due to a mustUnderstand fault -->\n" +
        "  <xs:element name=\"NotUnderstood\" type=\"tns:NotUnderstoodType\" />\n" +
        "  <xs:complexType name=\"NotUnderstoodType\" >\n" +
        "    <xs:attribute name=\"qname\" type=\"xs:QName\" use=\"required\" />\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "\n" +
        "  <!-- Global element and associated types for managing version transition as described in Appendix A of the SOAP Version 1.2 Part 1 Recommendation  -->  <xs:complexType name=\"SupportedEnvType\" >\n" +
        "    <xs:attribute name=\"qname\" type=\"xs:QName\" use=\"required\" />\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "  <xs:element name=\"Upgrade\" type=\"tns:UpgradeType\" />\n" +
        "  <xs:complexType name=\"UpgradeType\" >\n" +
        "    <xs:sequence>\n" +
        "\t  <xs:element name=\"SupportedEnvelope\" type=\"tns:SupportedEnvType\" minOccurs=\"1\" maxOccurs=\"unbounded\" />\n" +
        "\t</xs:sequence>\n" +
        "  </xs:complexType>\n" +
        "\n" +
        "\n" +
        "</xs:schema>";

    private static final long XMLNS_SCHEMA_OID = -5L;
    private static final String XMLNS_SCHEMA_NAME = "xmlnamespace";
    private static final String XMLNS_SCHEMA_TNS = "http://www.w3.org/XML/1998/namespace";
    private static final String XMLNS_SCHEMA = "<?xml version='1.0'?>\n" +
        "<xs:schema targetNamespace=\"http://www.w3.org/XML/1998/namespace\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xml:lang=\"en\">\n" +
        "\n" +
        " <xs:annotation>\n" +
        "  <xs:documentation>\n" +
        "   See http://www.w3.org/XML/1998/namespace.html and\n" +
        "   http://www.w3.org/TR/REC-xml for information about this namespace.\n" +
        "\n" +
        "    This schema document describes the XML namespace, in a form\n" +
        "    suitable for import by other schema documents.  \n" +
        "\n" +
        "    Note that local names in this namespace are intended to be defined\n" +
        "    only by the World Wide Web Consortium or its subgroups.  The\n" +
        "    following names are currently defined in this namespace and should\n" +
        "    not be used with conflicting semantics by any Working Group,\n" +
        "    specification, or document instance:\n" +
        "\n" +
        "    base (as an attribute name): denotes an attribute whose value\n" +
        "         provides a URI to be used as the base for interpreting any\n" +
        "         relative URIs in the scope of the element on which it\n" +
        "         appears; its value is inherited.  This name is reserved\n" +
        "         by virtue of its definition in the XML Base specification.\n" +
        "\n" +
        "    id   (as an attribute name): denotes an attribute whose value\n" +
        "         should be interpreted as if declared to be of type ID.\n" +
        "         This name is reserved by virtue of its definition in the\n" +
        "         xml:id specification.\n" +
        "\n" +
        "    lang (as an attribute name): denotes an attribute whose value\n" +
        "         is a language code for the natural language of the content of\n" +
        "         any element; its value is inherited.  This name is reserved\n" +
        "         by virtue of its definition in the XML specification.\n" +
        "  \n" +
        "    space (as an attribute name): denotes an attribute whose\n" +
        "         value is a keyword indicating what whitespace processing\n" +
        "         discipline is intended for the content of the element; its\n" +
        "         value is inherited.  This name is reserved by virtue of its\n" +
        "         definition in the XML specification.\n" +
        "\n" +
        "    Father (in any context at all): denotes Jon Bosak, the chair of \n" +
        "         the original XML Working Group.  This name is reserved by \n" +
        "         the following decision of the W3C XML Plenary and \n" +
        "         XML Coordination groups:\n" +
        "\n" +
        "             In appreciation for his vision, leadership and dedication\n" +
        "             the W3C XML Plenary on this 10th day of February, 2000\n" +
        "             reserves for Jon Bosak in perpetuity the XML name\n" +
        "             xml:Father\n" +
        "  </xs:documentation>\n" +
        " </xs:annotation>\n" +
        "\n" +
        " <xs:annotation>\n" +
        "  <xs:documentation>This schema defines attributes and an attribute group\n" +
        "        suitable for use by\n" +
        "        schemas wishing to allow xml:base, xml:lang, xml:space or xml:id\n" +
        "        attributes on elements they define.\n" +
        "\n" +
        "        To enable this, such a schema must import this schema\n" +
        "        for the XML namespace, e.g. as follows:\n" +
        "        &lt;schema . . .>\n" +
        "         . . .\n" +
        "         &lt;import namespace=\"http://www.w3.org/XML/1998/namespace\"\n" +
        "                    schemaLocation=\"http://www.w3.org/2001/xml.xsd\"/>\n" +
        "\n" +
        "        Subsequently, qualified reference to any of the attributes\n" +
        "        or the group defined below will have the desired effect, e.g.\n" +
        "\n" +
        "        &lt;type . . .>\n" +
        "         . . .\n" +
        "         &lt;attributeGroup ref=\"xml:specialAttrs\"/>\n" +
        " \n" +
        "         will define a type which will schema-validate an instance\n" +
        "         element with any of those attributes</xs:documentation>\n" +
        " </xs:annotation>\n" +
        "\n" +
        " <xs:annotation>\n" +
        "  <xs:documentation>In keeping with the XML Schema WG's standard versioning\n" +
        "   policy, this schema document will persist at\n" +
        "   http://www.w3.org/2007/08/xml.xsd.\n" +
        "   At the date of issue it can also be found at\n" +
        "   http://www.w3.org/2001/xml.xsd.\n" +
        "   The schema document at that URI may however change in the future,\n" +
        "   in order to remain compatible with the latest version of XML Schema\n" +
        "   itself, or with the XML namespace itself.  In other words, if the XML\n" +
        "   Schema or XML namespaces change, the version of this document at\n" +
        "   http://www.w3.org/2001/xml.xsd will change\n" +
        "   accordingly; the version at\n" +
        "   http://www.w3.org/2007/08/xml.xsd will not change.\n" +
        "  </xs:documentation>\n" +
        " </xs:annotation>\n" +
        "\n" +
        " <xs:attribute name=\"lang\">\n" +
        "  <xs:annotation>\n" +
        "   <xs:documentation>Attempting to install the relevant ISO 2- and 3-letter\n" +
        "         codes as the enumerated possible values is probably never\n" +
        "         going to be a realistic possibility.  See\n" +
        "         RFC 3066 at http://www.ietf.org/rfc/rfc3066.txt and the IANA registry\n" +
        "         at http://www.iana.org/assignments/lang-tag-apps.htm for\n" +
        "         further information.\n" +
        "\n" +
        "         The union allows for the 'un-declaration' of xml:lang with\n" +
        "         the empty string.</xs:documentation>\n" +
        "  </xs:annotation>\n" +
        "  <xs:simpleType>\n" +
        "   <xs:union memberTypes=\"xs:language\">\n" +
        "    <xs:simpleType>    \n" +
        "     <xs:restriction base=\"xs:string\">\n" +
        "      <xs:enumeration value=\"\"/>\n" +
        "     </xs:restriction>\n" +
        "    </xs:simpleType>\n" +
        "   </xs:union>\n" +
        "  </xs:simpleType>\n" +
        " </xs:attribute>\n" +
        "\n" +
        " <xs:attribute name=\"space\">\n" +
        "  <xs:simpleType>\n" +
        "   <xs:restriction base=\"xs:NCName\">\n" +
        "    <xs:enumeration value=\"default\"/>\n" +
        "    <xs:enumeration value=\"preserve\"/>\n" +
        "   </xs:restriction>\n" +
        "  </xs:simpleType>\n" +
        " </xs:attribute>\n" +
        "\n" +
        " <xs:attribute name=\"base\" type=\"xs:anyURI\">\n" +
        "  <xs:annotation>\n" +
        "   <xs:documentation>See http://www.w3.org/TR/xmlbase/ for\n" +
        "                     information about this attribute.</xs:documentation>\n" +
        "  </xs:annotation>\n" +
        " </xs:attribute>\n" +
        " \n" +
        " <xs:attribute name=\"id\" type=\"xs:ID\">\n" +
        "  <xs:annotation>\n" +
        "   <xs:documentation>See http://www.w3.org/TR/xml-id/ for\n" +
        "                     information about this attribute.</xs:documentation>\n" +
        "  </xs:annotation>\n" +
        " </xs:attribute>\n" +
        "\n" +
        " <xs:attributeGroup name=\"specialAttrs\">\n" +
        "  <xs:attribute ref=\"xml:base\"/>\n" +
        "  <xs:attribute ref=\"xml:lang\"/>\n" +
        "  <xs:attribute ref=\"xml:space\"/>\n" +
        "  <xs:attribute ref=\"xml:id\"/>\n" +
        " </xs:attributeGroup>\n" +
        "\n" +
        "</xs:schema>";

    public static final SchemaEntry SOAP11_SCHEMA_ENTRY = new SchemaEntry();
    static {
        SOAP11_SCHEMA_ENTRY.setSchema(SOAP11_SCHEMA);
        SOAP11_SCHEMA_ENTRY.setName(SOAP11_SCHEMA_NAME);
        SOAP11_SCHEMA_ENTRY.setTns(SOAP11_SCHEMA_TNS);
        SOAP11_SCHEMA_ENTRY.setOid(SOAP11_SCHEMA_OID);
        SOAP11_SCHEMA_ENTRY.setSystem(true);
    }
    public static final EntityHeader SOAP11_SCHEMA_HEADER = new EntityHeader(Long.toString(SOAP11_SCHEMA_OID),
                                                                           EntityType.SCHEMA_ENTRY,
                                                                           SOAP11_SCHEMA_ENTRY.getName(),
                                                                           "");

    public static final SchemaEntry SOAP12_SCHEMA_ENTRY = new SchemaEntry();
    static {
        SOAP12_SCHEMA_ENTRY.setSchema(SOAP12_SCHEMA);
        SOAP12_SCHEMA_ENTRY.setName(SOAP12_SCHEMA_NAME);
        SOAP12_SCHEMA_ENTRY.setTns(SOAP12_SCHEMA_TNS);
        SOAP12_SCHEMA_ENTRY.setOid(SOAP12_SCHEMA_OID);
        SOAP12_SCHEMA_ENTRY.setSystem(true);
    }
    public static final EntityHeader SOAP12_SCHEMA_HEADER = new EntityHeader(Long.toString(SOAP12_SCHEMA_OID),
                                                                           EntityType.SCHEMA_ENTRY,
                                                                           SOAP12_SCHEMA_ENTRY.getName(),
                                                                           "");

    public static final SchemaEntry XMLNS_SCHEMA_ENTRY = new SchemaEntry();
    static {
        XMLNS_SCHEMA_ENTRY.setSchema(XMLNS_SCHEMA);
        XMLNS_SCHEMA_ENTRY.setName(XMLNS_SCHEMA_NAME);
        XMLNS_SCHEMA_ENTRY.setTns(XMLNS_SCHEMA_TNS);
        XMLNS_SCHEMA_ENTRY.setOid(XMLNS_SCHEMA_OID);
        XMLNS_SCHEMA_ENTRY.setSystem(true);
    }
    public static final EntityHeader XMLNS_SCHEMA_HEADER = new EntityHeader(Long.toString(XMLNS_SCHEMA_OID),
                                                                           EntityType.SCHEMA_ENTRY,
                                                                           XMLNS_SCHEMA_ENTRY.getName(),
                                                                           "");
}
