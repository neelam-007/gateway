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
import java.io.IOException;
import java.util.Map;

/**
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

    public void setXmlProperties(String xml) throws IOException {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
            bean.setProperties((Map)xd.readObject());
        }
    }

    public String getXmlProperties() throws IOException {
        if ( xmlProperties == null ) {
            Map properties = bean.getProperties();
            if ( properties == null ) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLEncoder xe = new XMLEncoder(baos);
            xe.writeObject(properties);
            xmlProperties = baos.toString(PROPERTIES_ENCODING);
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
            return new Long( bean.getUniqueIdentifier() ).longValue();
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
        return new Long( _oid ).toString();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return bean.getProviderId();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId ) {
        bean.setProviderId( providerId );
    }

    public int getVersion() {
        return bean.getVersion();
    }

    public void setVersion(int version) {
        bean.setVersion(version);
    }

    public abstract String toString();

    public abstract void copyFrom( Group objToCopy);

    public GroupBean getGroupBean() {
        return bean;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersistentGroup)) return false;
        final PersistentGroup groupImp = (PersistentGroup) o;
        if (_oid != DEFAULT_OID ? !(_oid == groupImp._oid) : groupImp._oid != DEFAULT_OID ) return false;
        return true;
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
