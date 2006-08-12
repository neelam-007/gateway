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

/**
 * Abstract superclass of {@link Group}s that are stored in the database, as opposed to in external directories.
 *
 * @author alex
 */
public abstract class PersistentGroup extends NamedEntityImp implements Group {
    public static final String PROPERTIES_ENCODING = "UTF-8";

    public PersistentGroup() {
        this.bean = new GroupBean();
    }

    public PersistentGroup(GroupBean bean) {
        this.bean = bean;
    }

    public void setXmlProperties(String xml) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            try {
                XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
                bean.setProperties((Map)xd.readObject());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
    }

    public String getXmlProperties() {
        if ( xmlProperties == null ) {
            Map properties = bean.getProperties();
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

    public void setOid( long oid ) {
        super.setOid(oid);
        bean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        String uniqueId = bean.getId();
        if ( uniqueId == null || uniqueId.length() == 0 ) {
            return -1L;
        } else {
            return Long.parseLong(bean.getId());
        }
    }

    public String getDescription() {
        return bean.getDescription();
    }

    public String getName() {
        return bean.getName();
    }

    public void setDescription(String description) {
        bean.setDescription( description );
    }

    public void setName( String name ) {
        super.setName(name);
        bean.setName( name );
    }

    public String getId() {
        return Long.toString(_oid);
    }

    public long getProviderId() {
        return bean.getProviderId();
    }

    public void setProviderId( long providerId ) {
        bean.setProviderId( providerId );
    }

    public int getVersion() {
        return bean.getVersion();
    }

    public void setVersion(int version) {
        bean.setVersion(version);
    }

    public void copyFrom( Group objToCopy) {
        PersistentGroup imp = (PersistentGroup)objToCopy;
        setOid(imp.getOid());
        setName(imp.getName());
        setDescription(imp.getDescription());
        setProviderId(imp.getProviderId());
        setXmlProperties(imp.getXmlProperties());
    }

    public GroupBean getGroupBean() {
        return bean;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final PersistentGroup that = (PersistentGroup)o;

        if (getProviderId() != that.getProviderId()) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        long providerOid = getProviderId();
        result = 31 * result + (int)(providerOid ^ (providerOid >>> 32));
        return result;
    }

    protected GroupBean bean;
    protected String xmlProperties;
}
