package com.l7tech.server.security.keystore;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//TODO change to use NamedEntityWithPropertiesImp
/**
 * Represents raw keystore data stored in a row of the "keystore" table.
 * <p/>
 * This is a low-level class used only for holding keystore bytes as they are copied to and from the database.
 * <p/>
 * The movement in and out of the database is managed by an implementation of {@link SsgKeyStore} such
 * as {@link com.l7tech.server.security.keystore.software.DatabasePkcs12SsgKeyStore} or
 * {@link com.l7tech.server.security.keystore.sca.ScaSsgKeyStore}.
 */
@Entity
@Proxy(lazy=false)
@Table(name="keystore_file")
public class KeystoreFile extends NamedEntityImp {
    private static final long serialVersionUID = 7293792837442132345L;
    private static final Charset PROPERTIES_ENCODING = Charsets.UTF8;

    @Column(name="format", nullable=false, length=128)
    private String format;
    private byte[] databytes;
    private transient String xmlProperties;
    private Map<String, String> properties;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Basic(fetch=FetchType.LAZY)
    @Column(name="databytes",length=Integer.MAX_VALUE)
    @Lob
    public byte[] getDatabytes() {
        return databytes;
    }

    public void setDatabytes(byte[] databytes) {
        this.databytes = databytes;
    }

    @Column(name="properties",length=Integer.MAX_VALUE)
    @Lob
    public String getXmlProperties() {
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

    public void setXmlProperties( final String xml ) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
            //noinspection unchecked
            this.properties = (Map<String, String>)xd.readObject();
        }
    }

    public String getProperty( final String propertyName ) {
        String propertyValue = null;

        Map<String,String> properties = this.properties;
        if (properties != null) {
            propertyValue = properties.get(propertyName);
        }

        return propertyValue;
    }

    public void setProperty( final String propertyName, final String propertyValue ) {
        Map<String,String> properties = this.properties;
        if (properties == null) {
            properties = new HashMap<String, String>();
            this.properties = properties;
        }

        properties.put(propertyName, propertyValue);

        // invalidate cached properties
        xmlProperties = null;
    }    

    /** @noinspection RedundantIfStatement*/
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        KeystoreFile that = (KeystoreFile)o;

        if (!Arrays.equals(databytes, that.databytes)) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (databytes != null ? Arrays.hashCode(databytes) : 0);
        return result;
    }
}
