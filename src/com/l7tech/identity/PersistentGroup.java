/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;

/**
 * Abstract superclass of {@link Group}s that are stored in the database, as opposed to in external directories.
 *
 * @author alex
 */
public abstract class PersistentGroup extends NamedEntityImp implements Group {
    public static final String PROPERTIES_ENCODING = "UTF-8";

    private long providerOid;
    private String description;
    protected String xmlProperties;
    private transient Map<String, String> properties;

    protected PersistentGroup(long providerOid, String name) {
        this.providerOid = providerOid;
        this._name = name;
    }

    public synchronized void setXmlProperties(String xml) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            try {
                XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
                //noinspection unchecked
                this.properties = (Map<String, String>)xd.readObject();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
    }

    public synchronized String getXmlProperties() {
        if ( xmlProperties == null ) {
            Map<String, String> properties = getProperties();
            if ( properties == null ) return null;
            BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
            try {
                XMLEncoder xe = new XMLEncoder(baos);
                xe.writeObject(properties);
                xmlProperties = baos.toString(PROPERTIES_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
            } finally {
                baos.close();
            }
        }
        return xmlProperties;
    }

    protected Map<String, String> getProperties() {
        if (properties == null) properties = new HashMap<String, String>();
        return properties;
    }

    public synchronized void setProperties(Map<String, String> properties) {
        this.properties = properties;
        this.xmlProperties = null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getProviderId() {
        return providerOid;
    }

    public void setProviderId( long providerId ) {
        this.providerOid = providerId;
    }

    public void copyFrom( Group objToCopy) {
        PersistentGroup imp = (PersistentGroup)objToCopy;
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

        if (providerOid != that.providerOid) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (providerOid ^ (providerOid >>> 32));
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
