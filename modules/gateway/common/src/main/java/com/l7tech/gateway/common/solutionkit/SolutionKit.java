package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.NamedEntityWithPropertiesImp;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.SafeXMLDecoder;
import com.l7tech.util.SafeXMLDecoderBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * A persistent entity that holds information about an installed solution kit.
 */
@Entity
@Proxy(lazy=false)
@Table(name="solution_kit")
public class SolutionKit extends NamedEntityWithPropertiesImp implements Comparable<SolutionKit> {
    /**
     * Maximum allowed length of the name field.
     */
    public static final int NAME_FIELD_MAX_LENGTH = 255;

    private static final long serialVersionUID = 6091924030211843475L;
    private static final Charset INSTALL_PROPERTIES_ENCODING = Charsets.UTF8;

    public static final String SK_PROP_DESC_KEY = "Description";
    public static final String SK_PROP_TIMESTAMP_KEY = "TimeStamp";
    public static final String SK_PROP_IS_COLLECTION_KEY = "IsCollection";
    public static final String SK_PROP_FEATURE_SET_KEY = "FeatureSet";
    public static final String SK_PROP_CUSTOM_CALLBACK_KEY = "CustomCallback";
    public static final String SK_PROP_CUSTOM_UI_KEY = "CustomUi";
    public static final String SK_PROP_INSTANCE_MODIFIER_KEY = "InstanceModifier";

    private String sk_guid;
    private String sk_version;
    private transient String installXmlProperties;
    private Map<String, String> installProperties;
    private String mappings;
    private String uninstallBundle;
    private long lastUpdateTime;
    private Set<EntityOwnershipDescriptor> entityOwnershipDescriptors;

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

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy="solutionKit", cascade=CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval=true)
    public Set<EntityOwnershipDescriptor> getEntityOwnershipDescriptors() {
        return entityOwnershipDescriptors;
    }

    public void setEntityOwnershipDescriptors(Set<EntityOwnershipDescriptor> entityOwnershipDescriptors) {
        checkLocked();
        this.entityOwnershipDescriptors = entityOwnershipDescriptors;
    }

    public void addEntityOwnershipDescriptors(Set<EntityOwnershipDescriptor> entityOwnershipDescriptors) {
        checkLocked();

        if (null == this.entityOwnershipDescriptors) {
            this.entityOwnershipDescriptors = new HashSet<>();
        }

        this.entityOwnershipDescriptors.addAll(entityOwnershipDescriptors);
    }

    public void removeEntityOwnershipDescriptors(Set<EntityOwnershipDescriptor> entityOwnershipDescriptors) {
        if (null != this.entityOwnershipDescriptors) {
            checkLocked();
            this.entityOwnershipDescriptors.removeAll(entityOwnershipDescriptors);
        }
    }

    @Transient
    @RbacAttribute
    @Size(min = 1, max = NAME_FIELD_MAX_LENGTH)
    @Override
    public String getName() {
        return super.getName();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SolutionKit that = (SolutionKit) o;

        if (lastUpdateTime != that.lastUpdateTime) return false;
        if (installProperties != null ? !installProperties.equals(that.installProperties) : that.installProperties != null)
            return false;
        if (installXmlProperties != null ? !installXmlProperties.equals(that.installXmlProperties) : that.installXmlProperties != null)
            return false;
        if (mappings != null ? !mappings.equals(that.mappings) : that.mappings != null) return false;
        if (sk_guid != null ? !sk_guid.equals(that.sk_guid) : that.sk_guid != null) return false;
        if (sk_version != null ? !sk_version.equals(that.sk_version) : that.sk_version != null) return false;
        if (uninstallBundle != null ? !uninstallBundle.equals(that.uninstallBundle) : that.uninstallBundle != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sk_guid != null ? sk_guid.hashCode() : 0);
        result = 31 * result + (sk_version != null ? sk_version.hashCode() : 0);
        result = 31 * result + (installXmlProperties != null ? installXmlProperties.hashCode() : 0);
        result = 31 * result + (installProperties != null ? installProperties.hashCode() : 0);
        result = 31 * result + (mappings != null ? mappings.hashCode() : 0);
        result = 31 * result + (uninstallBundle != null ? uninstallBundle.hashCode() : 0);
        result = 31 * result + (int) (lastUpdateTime ^ (lastUpdateTime >>> 32));
        return result;
    }

    @Override
    public int compareTo(@NotNull SolutionKit o) {
        // Compare name
        final int compareName = String.CASE_INSENSITIVE_ORDER.compare(getName(), o.getName());
        if (compareName != 0) {
            return compareName;
        }
        // Compare sk_version
        else {
            final int compareSKVersion = String.CASE_INSENSITIVE_ORDER.compare(getSolutionKitVersion(), o.getSolutionKitVersion());
            if (compareSKVersion != 0) {
                return compareSKVersion;
            }
            // Compare instance modifier
            else {
                // Instance modifier could be null, so reassign it as an empty string before passing it into the compare method.
                String instanceModifier = getProperty(SK_PROP_INSTANCE_MODIFIER_KEY);
                if (instanceModifier == null) instanceModifier = "";

                String otherIM = o.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY);
                if (otherIM == null) otherIM = "";

                final int compareInstanceModifier = String.CASE_INSENSITIVE_ORDER.compare(instanceModifier, otherIM);
                if (compareInstanceModifier != 0) {
                    return compareInstanceModifier;
                }
                // Compare description
                else {
                    final int compareDescription = String.CASE_INSENSITIVE_ORDER.compare(getProperty(SK_PROP_DESC_KEY), o.getProperty(SK_PROP_DESC_KEY));
                    if (compareDescription != 0) {
                        return compareDescription;
                    }
                    // Compare update time
                    else {
                        return (int) (getLastUpdateTime() - o.getLastUpdateTime());
                    }
                }
            }
        }
    }
}