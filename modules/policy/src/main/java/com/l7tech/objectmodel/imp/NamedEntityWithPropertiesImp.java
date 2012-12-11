package com.l7tech.objectmodel.imp;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.NamedEntityWithProperties;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the implementation of the namedEntityWithProperties. It is a named entity that provides a properties map. The properties are persisted to the database as an xml serialized hashmap
 *
 * @author Victor Kazakov
 */
@MappedSuperclass
public abstract class NamedEntityWithPropertiesImp extends NamedEntityImp implements NamedEntityWithProperties {
    private static final Charset PROPERTIES_ENCODING = Charsets.UTF8;
    private transient String xmlProperties;
    private Map<String, String> properties;

    /**
     * Returns the properties serialized as xml
     *
     * @return Properties as an xml string
     */
    @Column(name = "properties", length = Integer.MAX_VALUE)
    @Lob
    public String getXmlProperties() {
        if (xmlProperties == null) {
            Map<String, String> properties = this.properties;
            if (properties == null) return null;
            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
            try {
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
                xe.writeObject(properties);
                xe.close();
                xmlProperties = baos.toString(PROPERTIES_ENCODING);
            } finally {
                baos.close();
            }
        }
        return xmlProperties;
    }

    /**
     * Sets the properties from an xml string.
     *
     * @param xml The xml to set the properties from.
     */
    public void setXmlProperties(final String xml) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if (xml != null && xml.length() > 0) {
            XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
            //noinspection unchecked
            this.properties = (Map<String, String>) xd.readObject();
        }
    }

    /**
     * Gets a property for this entity
     *
     * @param propertyName The property whose value to retrieve
     * @return The property value. Null if no such property exists
     */
    @Override
    public String getProperty(final String propertyName) {
        String propertyValue = null;

        Map<String, String> properties = this.properties;
        if (properties != null) {
            propertyValue = properties.get(propertyName);
        }

        return propertyValue;
    }

    /**
     * Sets a property for this entity
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     */
    @Override
    public void setProperty(final String propertyName, final String propertyValue) {
        Map<String, String> properties = this.properties;
        if (properties == null) {
            properties = new HashMap<String, String>();
            this.properties = properties;
        }

        properties.put(propertyName, propertyValue);

        // invalidate cached properties
        xmlProperties = null;
    }
}
