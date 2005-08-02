/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.tarari.xml.schema.SchemaResolver;

/**
 * This manager gives access to the community schemas included in the
 * schema table. This is meant to be used by the server schema validation
 * implementation using tarari as well as for schema import resolution support
 * in the case of software schema validation.
 *
 * @author flascelles@layer7-tech.com
 */
public class CommunitySchemaManager extends HibernateDaoSupport {

    public CommunitySchemaManager() {
    }

    public Collection findAll() throws FindException {
        String queryall = "from " + TABLE_NAME + " in class " + CommunitySchemaEntry.class.getName();
        Collection output = getHibernateTemplate().find(queryall);
        if (output.isEmpty()) {
            CommunitySchemaEntry defaultEntry = new CommunitySchemaEntry();
            defaultEntry.setSchema(SOAP_SCHEMA);
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

    public long save(CommunitySchemaEntry newSchema) throws SaveException {
        return ((Long)getHibernateTemplate().save(newSchema)).longValue();
    }

    public void update(CommunitySchemaEntry existingSchema) throws UpdateException {
        getHibernateTemplate().update(existingSchema);
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
            private final String HOMEDIR = System.getProperty("user.dir");
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                // by default, the parser constructs a systemId in the form of a url "file:///user.dir/filename"
                String schemaId = systemId;
                if (systemId != null && HOMEDIR != null) {
                    int pos = systemId.indexOf(HOMEDIR);
                    if (pos > -1) {
                        schemaId = systemId.substring(pos+HOMEDIR.length()+1);
                    }
                }
                logger.info("asking for resource " + schemaId);
                // todo, get schema based on the schemaId from the table instead of throwing
                //return new InputSource(get schema based on schemId);
                throw new SAXException("schema imports based on community table are not yet supported.");
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
                return new byte[0];
            }
        };

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
