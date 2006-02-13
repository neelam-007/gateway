/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Abstract superclass of {@link Group}s that are stored in the database, as opposed to in external directories.
 *
 * @author alex
 * @version $Revision$
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
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLEncoder xe = new XMLEncoder(baos);
                xe.writeObject(properties);
                xmlProperties = baos.toString(PROPERTIES_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
        return xmlProperties;
    }

    public void setOid( long oid ) {
        super.setOid(oid);
        bean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        String uniqueId = bean.getUniqueIdentifier();
        if ( uniqueId == null || uniqueId.length() == 0 ) {
            return -1L;
        } else {
            return Long.parseLong(bean.getUniqueIdentifier());
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
        bean.setName( name );
    }

    public String getUniqueIdentifier() {
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
        if (!(o instanceof PersistentGroup)) return false;
        final PersistentGroup groupImp = (PersistentGroup) o;
        return !(_oid != DEFAULT_OID ? !(_oid == groupImp._oid) : groupImp._oid != DEFAULT_OID);
    }

    public int hashCode() {
        if ( _oid != DEFAULT_OID ) return (int)_oid;
        if ( _name == null ) return System.identityHashCode(this);

        int hash = _name.hashCode();
        hash += 29 * (int)bean.getProviderId();
        return hash;
    }

    protected GroupBean bean;
    protected String xmlProperties;
}