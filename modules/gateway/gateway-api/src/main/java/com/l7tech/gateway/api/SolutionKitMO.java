package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
@XmlRootElement(name = "SolutionKit")
@XmlType(name = "SolutionKitType", propOrder = {"name", "skGuid", "skVersion", "parentReference", "properties", "installProperties", "uninstallBundle", "mappings", "lastUpdateTime", "entityOwnershipDescriptors", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "solutionKits")
public class SolutionKitMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the {@code SolutionKit}. Required and cannot be {@code null}.
     *
     * @return the solution kit name
     */
    @XmlElement(name = "Name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the {@code SolutionKit}.
     *
     * @param name The name to use
     */
    public void setName(final String name) {
        this.name = name;
    }


    /**
     * Get the {@code SolutionKit} {@code GUID}. Required and cannot be {@code null}.
     *
     * @return the solution kit {@code GUID}
     */
    @XmlElement(name = "SolutionKitGuid", required = true)
    public String getSkGuid() {
        return skGuid;
    }

    /**
     * Set the name for the {@code SolutionKit}.
     *
     * @param skGuid The solution kit goid to use
     */
    public void setSkGuid(final String skGuid) {
        this.skGuid = skGuid;
    }

    /**
     * Get the {@code SolutionKit} version. Required and cannot be {@code null}.
     *
     * @return the solution kit version
     */
    @XmlElement(name = "SolutionKitVersion", required = true)
    public String getSkVersion() {
        return skVersion;
    }

    /**
     * Set the {@code SolutionKit} version.
     *
     * @param sk_version The version to use
     */
    public void setSkVersion(final String sk_version) {
        this.skVersion = sk_version;
    }

    /**
     * Get the {@code SolutionKit} parent reference.
     *
     * @return The {@code SolutionKit} parent reference.
     */
    @XmlElement(name="ParentReference")
    public final ManagedObjectReference getParentReference() {
        return parentReference;
    }

    /**
     * Set the {@code SolutionKit} parent reference.
     *
     * @param parentReference The {@code SolutionKit} parent reference.
     */
    public final void setParentReference(final ManagedObjectReference parentReference) {
        this.parentReference = parentReference;
    }

    /**
     * Get the {@code SolutionKit} properties.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name = "Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this {@code SolutionKit}.
     */
    public void setProperties(final Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Get the {@code SolutionKit} install properties.
     *
     * @return The signature properties (may be null)
     */
    @XmlElement(name = "InstallProperties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getInstallProperties() {
        return installProperties;
    }

    /**
     * Set the install properties for this {@code SolutionKit}.
     */
    public void setInstallProperties(final Map<String, String> installProperties) {
        this.installProperties = installProperties;
    }

    /**
     * Get the {@code SolutionKit} uninstallBundle. Required and cannot be {@code null}.
     *
     * @return the solution kit uninstallBundle
     */
    @XmlElement(name = "UninstallBundle")
    public String getUninstallBundle() {
        return uninstallBundle;
    }

    /**
     * Set the {@code SolutionKit} uninstallBundle.
     *
     * @param uninstallBundle The uninstallBundle to use
     */
    public void setUninstallBundle(final String uninstallBundle) {
        this.uninstallBundle = uninstallBundle;
    }

    /**
     * Get the {@code SolutionKit} mappings. Required and cannot be {@code null}.
     *
     * @return the solution kit mappings
     */
    @XmlElement(name = "Mappings", required = true)
    public String getMappings() {
        return mappings;
    }

    /**
     * Set the {@code SolutionKit} mappings.
     *
     * @param mappings The mappings to use
     */
    public void setMappings(final String mappings) {
        this.mappings = mappings;
    }

    /**
     * Get the {@code SolutionKit} last update time.
     *
     * @return the solution kit lastUpdateTime
     */
    @XmlElement(name = "LastUpdateTime", required = true)
    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Set the {@code SolutionKit} last update time.
     *
     * @param lastUpdateTime The last update time to use
     */
    public void setLastUpdateTime(final Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * This is the list of entity ownership descriptors this {@code SolutionKit}.
     *
     * @return The list of entity ownership descriptors for this solution kit.
     */
    @XmlElementWrapper(name = "EntityOwnershipDescriptors", required = false)
    @XmlElement(name = "EntityOwnershipDescriptor", required = false)
    public List<EntityOwnershipDescriptorMO> getEntityOwnershipDescriptors() {
        return entityOwnershipDescriptors;
    }

    /**
     * Sets the list of entity ownership descriptors for this {@code SolutionKit}.
     *
     * @param entityOwnershipDescriptors The list of entity ownership descriptors for this solution kit.
     */
    public void setEntityOwnershipDescriptors(final List<EntityOwnershipDescriptorMO> entityOwnershipDescriptors) {
        this.entityOwnershipDescriptors = entityOwnershipDescriptors;
    }

    //- PROTECTED

    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }


    //- PACKAGE

    SolutionKitMO() {
    }


    //- PRIVATE

    private String name;
    private String skGuid;
    private String skVersion;
    private Map<String, String> properties;
    private Map<String, String> installProperties;
    private String uninstallBundle;
    private String mappings;
    private Long lastUpdateTime;
    private ManagedObjectReference parentReference;
    private List<EntityOwnershipDescriptorMO> entityOwnershipDescriptors = new ArrayList<>();
}
