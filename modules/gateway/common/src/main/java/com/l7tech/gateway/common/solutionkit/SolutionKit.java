package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.NamedEntityWithPropertiesImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.SafeXMLDecoder;
import com.l7tech.util.SafeXMLDecoderBuilder;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * A persistent entity that holds information about an installed solution kit.
 */
@Entity
@Proxy(lazy=false)
@Table(name="solution_kit")
public class SolutionKit extends NamedEntityWithPropertiesImp {
    private static final Charset INSTALL_PROPERTIES_ENCODING = Charsets.UTF8;

    public static final String SK_PROP_DESC_KEY = "Description";
    public static final String SK_PROP_TIMESTAMP_KEY = "TimeStamp";
    public static final String SK_PROP_FEATURE_SET_KEY = "FeatureSet";

    private String sk_guid;
    private String sk_version;
    private transient String installXmlProperties;
    private Map<String, String> installProperties;
    private String mappings;
    private String uninstallBundle;
    private long lastUpdateTime;

    public SolutionKit() {
    }

    @Column(name = "sk_guid", nullable = false)
    public String getSolutionKitGuid() {
        return sk_guid;
    }

    public void setSolutionKitGuid(String sk_guid) {
        this.sk_guid = sk_guid;
    }

    @Column(name = "sk_version", nullable = false)
    @Size(min = 1, max = 16)
    public String getSolutionKitVersion() {
        return sk_version;
    }

    public void setSolutionKitVersion (String sk_version) {
        this.sk_version = sk_version;
    }

    /**
     * Returns the solution kit installation properties serialized as xml
     *
     * @return Properties as an xml string
     */
    @Column(name = "install_properties", length = Integer.MAX_VALUE)
    @Lob
    public String getInstallationXmlProperties() {
        if (installXmlProperties == null) {
            Map<String, String> properties = installProperties;
            if (properties == null) return null;
            try (PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream()) {
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
                xe.writeObject(properties);
                xe.close();
                installXmlProperties = baos.toString(INSTALL_PROPERTIES_ENCODING);
            }

        }
        return installXmlProperties;
    }

    /**
     * Sets the solution kit installation properties from an xml string.
     *
     * @param xml The xml to set the properties from.
     */
    public void setInstallationXmlProperties(final String xml) {
        if (xml != null && xml.equals(installXmlProperties)) {
            return;
        }

        installXmlProperties = xml;
        if (xml != null && xml.length() > 0) {
            SafeXMLDecoder xd = new SafeXMLDecoderBuilder(new ByteArrayInputStream(xml.getBytes(INSTALL_PROPERTIES_ENCODING))).build();
            //noinspection unchecked
            installProperties = (Map<String, String>) xd.readObject();
        }
    }

    /**
     * Gets a solution kit installation property
     *
     * @param propertyName The property whose value to retrieve
     * @return The property value. Null if no such property exists
     */
    public String getInstallationProperty(final String propertyName) {
        String propertyValue = null;

        Map<String, String> properties = installProperties;
        if (properties != null) {
            propertyValue = properties.get(propertyName);
        }

        return propertyValue;
    }

    /**
     * Sets a solution kit installation property.
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     */
    public void setInstallationProperty(final String propertyName, final String propertyValue) {
        Map<String, String> properties = installProperties;
        if (properties == null) {
            properties = new HashMap<>();
            installProperties = properties;
        }

        properties.put(propertyName, propertyValue);

        // invalidate cached properties
        installXmlProperties = null;
    }

    @Column(name = "mappings", nullable = false, length = Integer.MAX_VALUE)
    @Lob
    public String getMappings() {
        return mappings;
    }

    public void setMappings(String mappings) {
        this.mappings = mappings;
    }

    @Column(name = "uninstall_bundle", length = Integer.MAX_VALUE)
    @Lob
    public String getUninstallBundle() {
        return uninstallBundle;
    }

    public void setUninstallBundle(String uninstallBundle) {
        this.uninstallBundle = uninstallBundle;
    }

    @Column(name = "last_update_time", nullable = false)
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}