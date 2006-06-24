/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class SchemaEntryManagerImpl
        extends HibernateEntityManager<SchemaEntry>
        implements SchemaEntryManager, ApplicationListener
{
    private static final Logger logger = Logger.getLogger(SchemaEntryManagerImpl.class.getName());

    private final Map<Long, String> systemIdsByOid = new HashMap<Long, String>();
    private SchemaManager schemaManager;

    public void setSchemaManager(SchemaManager schemaManager) {
        this.schemaManager = schemaManager;
    }

    protected void initDao() throws Exception {
        super.initDao();
        for (SchemaEntry entry : findAll()) {
            long oid = entry.getOid();
            try {
                compileAndCache(oid, entry);
            } catch (SAXException e) {
                logger.log(Level.WARNING, "Community schema #" + oid + " could not be compiled", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Community schema #" + oid + " could not be compiled", e);
            }
        }
    }

    public Collection<SchemaEntry> findAll() throws FindException {
        Collection<SchemaEntry> output = super.findAll();

        // make sure the soapenv schema is always there
        if (!containsSoapEnv(output)) {
            output = addSoapEnv(output);
        }

        return output;
    }

    private boolean containsSoapEnv(Collection<SchemaEntry> schemaEntries) {
        return containsShemaWithName(schemaEntries, SOAP_SCHEMA_NAME);
    }

    private boolean containsShemaWithName(Collection<SchemaEntry> schemaEntries, String schemaName) {
        boolean contains = false;

        for (SchemaEntry schemaEntry : schemaEntries) {
            if (schemaName.equals(schemaEntry.getName())) {
                contains = true;
                break;
            }
        }

        return contains;
    }


    private Collection<SchemaEntry> addSoapEnv(Collection<SchemaEntry> collection) {
        SchemaEntry defaultEntry = newDefaultEntry();
        try {
            save(defaultEntry);
        } catch (SaveException e) {
            logger.log(Level.WARNING, "cannot save default soap xsd", e);
        }
        ArrayList<SchemaEntry> completeList = new ArrayList<SchemaEntry>(collection);
        completeList.add(defaultEntry);
        return completeList;
    }


    /**
     * Find a schema from it's name (name column in community schema table)
     */
    @SuppressWarnings({"unchecked"})
    public Collection<SchemaEntry> findByName(String schemaName) throws FindException {
        String queryname = "from " + TABLE_NAME + " in class " + SchemaEntry.class.getName() +
                          " where " + TABLE_NAME + ".name = ?";
        Collection output = getHibernateTemplate().find(queryname, schemaName);

        if (SOAP_SCHEMA_NAME.equals(schemaName) && !containsSoapEnv(output)) {
            output = addSoapEnv(output);
        }

        return output;
    }

    /**
     * Find a schema from it's target namespace (tns column in community schema table)
     */
    @SuppressWarnings({"unchecked"})
    public Collection<SchemaEntry> findByTNS(String tns) throws FindException {
        String querytns = "from " + TABLE_NAME + " in class " + SchemaEntry.class.getName() +
                          " where " + TABLE_NAME + ".tns = ?";
        Collection output = getHibernateTemplate().find(querytns, tns);

        if (SOAP_SCHEMA_TNS.equals(tns) && !containsSoapEnv(output)) {
            output = addSoapEnv(output);
        }

        return output;
    }

    public long save(SchemaEntry newSchema) throws SaveException {
        if (newSchema.getOid() != SchemaEntry.DEFAULT_OID) {
            invalidateCompiledSchema(newSchema.getOid());
        }

        Long res = (Long) getHibernateTemplate().save(newSchema);
        if (res == null) {
            throw new SaveException("unexpected value returned from HibernateTemplate.save (null)");
        }
        try {
            compileAndCache(res, newSchema);
        } catch (IOException e) {
            throw new SaveException("Schema document imports remote document that is missing or invalid", e);
        } catch (SAXException e) {
            throw new SaveException("Invalid schema document", e);
        }
        return res;
    }

    public void update(SchemaEntry existingSchema) throws UpdateException {
        if (existingSchema.getOid() != SchemaEntry.DEFAULT_OID) {
            invalidateCompiledSchema(existingSchema.getOid());
        }

        // Get current object (Load from DB to ensure previous values are available in the session)
        try {
            SchemaEntry fromDb = (SchemaEntry) findByPrimaryKey(SchemaEntry.class, existingSchema.getOid());
            if(fromDb==null) throw new UpdateException("Schema does not exist '"+existingSchema.getName()+"'");

            // Update any potentially modified fields
            fromDb.setName(existingSchema.getName());
            fromDb.setTns(existingSchema.getTns());
            fromDb.setSchema(existingSchema.getSchema());

            // Commit
            getSession().update(fromDb);

            try {
                compileAndCache(fromDb.getOid(), fromDb);
            } catch (IOException e) {
                throw new UpdateException("Schema document imports missing or invalid remote document", e);
            } catch (SAXException e) {
                throw new UpdateException("Invalid schema document", e);
            }
        } catch (FindException fe) {
            throw new UpdateException(fe.getMessage(), fe.getCause());
        }
    }

    public void delete(SchemaEntry existingSchema) throws DeleteException {
        try {
            getHibernateTemplate().delete(existingSchema);
        } finally {
            invalidateCompiledSchema(existingSchema.getOid());
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eieio = (EntityInvalidationEvent) event;
            if (SchemaEntry.class.isAssignableFrom(eieio.getEntityClass())) {
                for (long oid : eieio.getEntityIds()) {
                    try {
                        if (!invalidateCompiledSchema(oid)) continue; // We have no record of it, don't care

                        // See if it still exists (i.e. it's updated, not deleted)
                        SchemaEntry entry = findEntity(oid);
                        if (entry == null) continue; // It's gone, no need to compile

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
        }
    }

    private SchemaEntry newDefaultEntry() {
        SchemaEntry defaultEntry = new SchemaEntry();
        defaultEntry.setSchema(SOAP_SCHEMA);
        defaultEntry.setName(SOAP_SCHEMA_NAME);
        defaultEntry.setTns(SOAP_SCHEMA_TNS);
        return defaultEntry;
    }

    private void compileAndCache(long oid, SchemaEntry entry) throws SAXException, IOException {
        String systemId = entry.getName();
        if (systemId == null) systemId = "policy:communityschema:" + oid;
        schemaManager.registerSchema(systemId, entry.getSchema());

        // Make sure it compiles
        SchemaHandle handle = schemaManager.getSchemaByUrl(systemId);
        handle.close();

        synchronized(this) {
            systemIdsByOid.put(oid, systemId);
        }
    }

    /**
     * Ensure that no SchemaEntry with this oid has a corresponding cached CompiledSchema.
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


    public Class getImpClass() {
        return SchemaEntry.class;
    }

    public Class getInterfaceClass() {
        return SchemaEntry.class;
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    private static final String TABLE_NAME = "community_schema";
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

}
