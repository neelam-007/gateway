/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

//TODO change to use NamedEntityWithPropertiesImp
/**
 * Abstract superclass of {@link Group}s that are stored in the database, as opposed to in external directories.
 *
 * @author alex
 */
@MappedSuperclass
public abstract class PersistentGroup extends NamedEntityImp implements Group {
    public static final Charset PROPERTIES_ENCODING = Charsets.UTF8;

    private long providerOid;
    private String description;
    private transient String xmlProperties;
    private Map<String, String> properties;

    /*Required by Jaxb*/
    public PersistentGroup(){

    }

    protected PersistentGroup(long providerOid, String name) {
        this.providerOid = providerOid;
        this._name = name;
    }

    protected PersistentGroup(long providerOid, String name, Map<String, String> properties) {
        this.providerOid = providerOid;
        this._name = name;
        this.properties = new HashMap<String, String>(properties);
    }

    public synchronized void setXmlProperties(String xml) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
            //noinspection unchecked
            this.properties = (Map<String, String>)xd.readObject();
        }
    }

    @Transient
    public synchronized String getXmlProperties() {
        if ( xmlProperties == null ) {
            Map<String, String> properties = this.properties;
            if ( properties == null ) return null;
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

    @Column(name="description", length=4096)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Transient
    @Migration(mapName = NONE, mapValue = NONE, export = false, resolver = PropertyResolver.Type.ID_PROVIDER_CONFIG)
    public long getProviderId() {
        return providerOid;
    }

    public void setProviderId( long providerId ) {
        this.providerOid = providerId;
    }

    public void copyFrom( PersistentGroup imp ) {
        setOid(imp.getOid());
        setName(imp.getName());
        setDescription(imp.getDescription());
        setProviderId(imp.getProviderId());
        setXmlProperties(imp.getXmlProperties());
    }

    public boolean isEquivalentId(Object thatId) {
        return getId().equals(thatId.toString());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PersistentGroup that = (PersistentGroup) o;

        return providerOid == that.providerOid && !(description != null ? !description.equals(that.description) : that.description != null);

    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (providerOid ^ (providerOid >>> 32));
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    protected synchronized String getProperty(String propertyName) {
        String propertyValue = null;

        Map<String,String> properties = this.properties;
        if (properties != null) {
            propertyValue = properties.get(propertyName);
        }

        return propertyValue;
    }

    protected synchronized void setProperty(String propertyName, String propertyValue) {
        Map<String,String> properties = this.properties;
        if (properties == null) {
            properties = new HashMap<String, String>();
            this.properties = properties;
        }

        properties.put(propertyName, propertyValue);

        // invalidate cached properties
        xmlProperties = null;
    }
}
