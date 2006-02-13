/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.util.LSInputImpl;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfig;
import com.tarari.xml.schema.SchemaResolver;
import org.apache.xmlbeans.XmlException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This manager gives access to the community schemas included in the
 * schema table. This is meant to be used by the server schema validation
 * implementation using tarari as well as for schema import resolution support
 * in the case of software schema validation.
 *
 * @author flascelles@layer7-tech.com
 */
public class CommunitySchemaManager extends HibernateDaoSupport {
    private ServerConfig serverConfig;

    public CommunitySchemaManager() {
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public Collection findAll() throws FindException {
        String queryall = "from " + TABLE_NAME + " in class " + SchemaEntry.class.getName();
        Collection output = getHibernateTemplate().find(queryall);
        // make sure the soapenv schema is always there
        if (output.isEmpty()) {
            SchemaEntry defaultEntry = newDefaultEntry();
            try {
                save(defaultEntry);
            } catch (SaveException e) {
                logger.log(Level.WARNING, "cannot save default soap xsd", e);
            }
            output = new ArrayList();
            output.add(defaultEntry);
        }

        return output;
    }

    /**
     * Find a schema from it's name (name column in community schema table)
     */
    public Collection findByName(String schemaName) throws FindException {
        String queryname = "from " + TABLE_NAME + " in class " + SchemaEntry.class.getName() +
                          " where " + TABLE_NAME + ".name = ?";
        Collection output = getHibernateTemplate().find(queryname, schemaName);
        return output;
    }

    /**
     * Find a schema from it's target namespace (tns column in community schema table)
     */
    public Collection findByTNS(String tns) throws FindException {
        String querytns = "from " + TABLE_NAME + " in class " + SchemaEntry.class.getName() +
                          " where " + TABLE_NAME + ".tns = ?";
        Collection output = getHibernateTemplate().find(querytns, tns);
        return output;
    }

    public long save(SchemaEntry newSchema) throws SaveException {
        Long res = (Long)getHibernateTemplate().save(newSchema);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    logger.fine("updating tarari schemas post-save from " + CommunitySchemaManager.class.getName());
                    try {
                        TarariLoader.updateSchemasToCard(serverConfig.getSpringContext());
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    } /*catch (SchemaLoadingException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    }*/ catch (XmlException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    }
                }
            }
        });
        if (res == null) {
            throw new SaveException("unexpected value returned from HibernateTemplate.save (null)");
        }
        return res.longValue();
    }

    public void update(SchemaEntry existingSchema) throws UpdateException {
        getHibernateTemplate().update(existingSchema);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    logger.fine("updating tarari schemas post-update from " + CommunitySchemaManager.class.getName());
                    try {
                        TarariLoader.updateSchemasToCard(serverConfig.getSpringContext());
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    }/* catch (SchemaLoadingException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    }*/ catch (XmlException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    }
                }
            }
        });
    }

    public void delete(SchemaEntry existingSchema) throws DeleteException {
        getHibernateTemplate().delete(existingSchema);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    logger.fine("updating tarari schemas post-delete from " + CommunitySchemaManager.class.getName());
                    try {
                        TarariLoader.updateSchemasToCard(serverConfig.getSpringContext());
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    } /*catch (SchemaLoadingException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    }*/ catch (XmlException e) {
                        logger.log(Level.WARNING, "Error updating schemas to tarari card", e);
                    }
                }
            }
        });
    }

    /**
     * To get an EntityResolver based on the community schema table. This is meant to be used in conjunction with
     * javax.xml.parsers.DocumentBuilder.setEntityResolver.
     *
     * @return an EntityResolver that can be used to resolve schema import statements as part of the
     * software implementation of schema validation. this resolver assumes that the external schemas
     * are already populated in the community schema table and that they are identified the same way
     * as the import statements' schemaLocation attribute value.
     */
    public EntityResolver communityEntityResolver() {
        final CommunitySchemaManager manager = this;
        return new EntityResolver () {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                SchemaEntry resolved = manager.getSchemaEntryFromSystemId(systemId);

                if(resolved==null) {
                    // this is supposed to let sax parser resolve his own way if possible
                    return null;
                }
                else {
                    return new InputSource(new StringReader(resolved.getSchema()));
                }
            }
        };
    }

    /**
     * To get an LSResourceResolver based on the community schema table. This is meant to be used in
     * conjunction with javax.xml.validation.SchemaFactory.setResourceResolver.
     *
     * @return an LSResourceResolver that can be used to resolve schema import statements as part of
     * the software implementation of schema validation. this resolver assumes that the external
     * schemas are already populated in the community schema table and that they are identified the
     * same way as the import statements' schemaLocation attribute value.
     */
    public LSResourceResolver communityLSResourceResolver() {
        final CommunitySchemaManager manager = this;
        return new LSResourceResolver () {
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, final String baseURI) {
                LSInput lsInput = new LSInputImpl();
                if(XmlUtil.W3C_XML_SCHEMA.equals(type)) { // check we are resolving schema
                    SchemaEntry resolved = manager.getSchemaEntryFromSystemId(systemId);
                    if(resolved!=null) {
                        lsInput.setCharacterStream(new StringReader(resolved.getSchema()));
                    }
                }
                return lsInput; // if we return null the schema would be resolved over the network.
            }
        };

    }

    /**
     * Equivalent to communityEntityResolver but for tarari based hardware schema validation.
     * This is expected to be used by the GlobalTarariContextImpl
     */
    public SchemaResolver communitySchemaResolver() {
        final CommunitySchemaManager manager = this;
        return new SchemaResolver() {
            public byte[] resolveSchema(String tns, String location, String baseURI) {
                // todo, get schema based on information provided (from table).
                logger.info("tarari asking for resource. tns: " + tns +
                            ", location: " + location +
                            ", baseURI: " + baseURI);
                Collection matchingSchemas = null;
                // first, try to resolve by name
                if (location != null && location.length() > 0) {
                    logger.info("asking for schema with systemId " + location);
                    try {
                        matchingSchemas = manager.findByName(location);
                    } catch (FindException e) {
                        logger.log(Level.INFO, "error getting community schema with systemid " + location, e);
                    }
                }
                // then, try to resolve by tns
                if (matchingSchemas == null || matchingSchemas.isEmpty()) {
                   if (tns != null && tns.length() > 0) {
                        logger.info("asking for schema with tns " + tns);
                        try {
                            matchingSchemas = manager.findByTNS(tns);
                        } catch (FindException e) {
                            logger.log(Level.INFO, "error getting community schema with systemid " + tns, e);
                        }
                   }
                }
                if (matchingSchemas == null || matchingSchemas.isEmpty()) {
                    logger.warning("could not resolve external schema either by name or tns");
                    return new byte[0];
                } else {
                    SchemaEntry resolved = (SchemaEntry)matchingSchemas.iterator().next();
                    return resolved.getSchema().getBytes();
                }
            }
        };

    }

    private SchemaEntry newDefaultEntry() {
        SchemaEntry defaultEntry = new SchemaEntry();
        defaultEntry.setSchema(SOAP_SCHEMA);
        defaultEntry.setName("soapenv");
        defaultEntry.setTns("http://schemas.xmlsoap.org/soap/envelope/");
        return defaultEntry;
    }

    /**
     * Get the SchemaEntry or null if not found.
     */
    private SchemaEntry getSchemaEntryFromSystemId(final String systemId) {
        SchemaEntry resolved = null;
        String HOMEDIR = System.getProperty("user.dir");

        // by default, the parser constructs a systemId in the form of a url "file:///user.dir/filename"
        String schemaId = systemId;
        if (systemId != null && HOMEDIR != null) {
            int pos = systemId.indexOf(HOMEDIR);
            if (pos > -1) {
                schemaId = systemId.substring(pos+HOMEDIR.length()+1);
            }
        }

        logger.info("asking for schema with systemId " + schemaId);
        Collection matchingSchemas = null;
        try {
            matchingSchemas = findByName(schemaId);

            if (matchingSchemas.isEmpty()) {
                logger.warning("there were no community schema that would resolve with the systemid " + schemaId);
            }
            else {
                resolved = (SchemaEntry) matchingSchemas.iterator().next();
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting community schema with systemid " + schemaId, e);
        }

        return resolved;
    }

    private static final String TABLE_NAME = "community_schema";
    private final Logger logger = Logger.getLogger(CommunitySchemaManager.class.getName());
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
