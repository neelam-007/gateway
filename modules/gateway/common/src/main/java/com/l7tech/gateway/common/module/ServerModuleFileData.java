package com.l7tech.gateway.common.module;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.search.Dependency;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.SafeXMLDecoder;
import com.l7tech.util.SafeXMLDecoderBuilder;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds data bytes for a {@link ServerModuleFile Server Module File} that an SSM
 * administrator adds to the Gateway database in the hope that every cluster node will
 * install or upgrade it.
 * <p/>
 * This entity have to be fetched lazy, therefore annotation {@code @Proxy(lazy=true)} is set.
 */
@Entity
@Proxy(lazy = true)
@Table(name = "server_module_file_data")
public class ServerModuleFileData extends PersistentEntityImp implements Serializable {
    private static final long serialVersionUID = -7525412738669913604L;
    private static final Charset PROPERTIES_ENCODING = Charsets.UTF8;

    /**
     * Three signature property names: digest, cert, and signature.
     */
    public static final String SIGNATURE_PROP_DIGEST = "digest";
    public static final String SIGNATURE_PROP_CERT = "cert";
    public static final String SIGNATURE_PROP_SIGNATURE = "signature";

    /**
     * Module row bytes.
     */
    private byte[] dataBytes;
    // When adding fields, update copyFrom() method

    // Store signature properties
    private transient String xmlProperties;
    private Map<String, String> properties;

    /**
     * The owning {@link ServerModuleFile Server Module File}.
     */
    private ServerModuleFile serverModuleFile;

    /**
     * Required by Hibernate.
     */
    @Deprecated
    protected ServerModuleFileData() {
    }


    /**
     * Default constructor.
     *
     * @param serverModuleFile    Owning {@link ServerModuleFile module file}.  Required.
     */
    public ServerModuleFileData(@NotNull final ServerModuleFile serverModuleFile) {
        this.serverModuleFile = serverModuleFile;
    }


    /**
     * Represents the owning {@link ServerModuleFile Server Module File}.
     */
    @NotNull
    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true, mappedBy = "data")
    @Dependency(isDependency = false)
    public ServerModuleFile getServerModuleFile() {
        return serverModuleFile;
    }
    public void setServerModuleFile(final ServerModuleFile serverModuleFile) {
        this.serverModuleFile = serverModuleFile;
    }


    /**
     * Represents entity version field or property.
     */
    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }


    /**
     * Get the module row bytes.<br/>
     * Note that executing this property getter method might cause the object to be fetched from the Database.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data_bytes")
    public byte[] getDataBytes() {
        return dataBytes;
    }
    public void setDataBytes(final byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }

    /**
     * Returns the properties serialized as xml
     *
     * @return Properties as an xml string
     */
    @Column(name = "signature_properties", length = Integer.MAX_VALUE)
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
            SafeXMLDecoder xd = new SafeXMLDecoderBuilder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING))).build();
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

    @Transient
    public String getSignatureProperties() {
        if (properties == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(SIGNATURE_PROP_SIGNATURE).append('=').append(properties.get(SIGNATURE_PROP_SIGNATURE)).append('\n');
        sb.append(SIGNATURE_PROP_CERT).append('=').append(properties.get(SIGNATURE_PROP_CERT)).append('\n');
        sb.append(SIGNATURE_PROP_DIGEST).append('=').append(properties.get(SIGNATURE_PROP_DIGEST));

        return sb.toString();
    }

    @Transient
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Copy data from the specified {@code otherData}
     *
     * @param otherData    the {@link ServerModuleFileData module data} from which to copy properties.  Required.
     */
    public void copyFrom(@NotNull final ServerModuleFileData otherData) {
        setDataBytes(otherData.getDataBytes());
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ServerModuleFileData that = (ServerModuleFileData) o;

        // extract the module sha256
        //
        // todo (tveninov): Perhaps remove the check (serverModuleFile != null), serverModuleFile shouldn't be null
        // unless hibernate somehow mess-up the entity, in which NullPointerException exception might be a better option.
        final String moduleSha256 = serverModuleFile != null ? serverModuleFile.getModuleSha256() : null;
        final String thatModuleSha256 = that.getServerModuleFile() != null ? that.getServerModuleFile().getModuleSha256() : null;

        // no need to compare the row bytes, sha256 is enough
        return (moduleSha256 != null ? moduleSha256.equals(thatModuleSha256) : thatModuleSha256 == null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // extract the module sha256
        // no need to generate row bytes hash-code, having sha256 hash-code is enough
        //
        // todo (tveninov): Perhaps remove the check (serverModuleFile != null), serverModuleFile shouldn't be null
        // unless hibernate somehow mess-up the entity, in which NullPointerException exception might be a better option.
        final String moduleSha256 = serverModuleFile != null ? serverModuleFile.getModuleSha256() : null;
        result = 31 * result + (moduleSha256 != null ? moduleSha256.hashCode() : 0);
        return result;
    }
}