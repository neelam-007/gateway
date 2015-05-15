package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The ServerModuleFileMO managed objects represents a {@code ServerModuleFile} entity i.e. a modular or custom assertion module.
 * <p/>
 * The Accessor for {@code ServerModuleFile} entity supports read and write.<br/>
 * {@code ServerModuleFile} entity can be accessed by name or identifier (i.e. GOID).
 *
 * @see com.l7tech.gateway.api.ManagedObjectFactory#createServerModuleFileMO()
 */
@XmlRootElement(name = "ServerModuleFile")
@XmlType(name = "ServerModuleFileType", propOrder = {"name", "moduleType", "moduleSha256", "moduleDataValue", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "serverModuleFiles")
public class ServerModuleFileMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the {@code ServerModuleFile}. Required and cannot be {@code null}.
     *
     * @return the module name
     */
    @XmlElement(name = "Name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the {@code ServerModuleFile}.
     *
     * @param name The name to use
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the {@code ServerModuleFile} module type. Required and cannot be {@code null}.
     *
     * @return the module type
     */
    @XmlElement(name = "ModuleType", required = true)
    public ServerModuleFileModuleType getModuleType() {
        return moduleType;
    }

    /**
     * Set the {@code ServerModuleFile} module type.
     *
     * @param moduleType the module type
     */
    public void setModuleType(final ServerModuleFileModuleType moduleType) {
        this.moduleType = moduleType;
    }

    /**
     * Get the {@code ServerModuleFile} raw data digest (currently in SHA-256 format). Required and cannot be {@code null}.
     */
    @XmlElement(name = "ModuleSha256", required = true)
    public String getModuleSha256() {
        return moduleSha256;
    }

    /**
     * Set the {@code ServerModuleFile} raw data digest (currently in SHA-256 format).
     *
     * @param moduleSha256 The moduleSha256 to use
     */
    public void setModuleSha256(final String moduleSha256) {
        this.moduleSha256 = moduleSha256;
    }

    /**
     * Get the {@code ServerModuleFile} raw data.
     *
     * @return the value
     */
    public byte[] getModuleData() {
        return get(moduleData);
    }

    /**
     * Set the {@code ServerModuleFile} raw data.
     *
     * @param moduleData The data to use
     */
    public void setModuleData(final byte[] moduleData) {
        this.moduleData = set(this.moduleData, moduleData);
    }

    /**
     * Get the {@code ServerModuleFile} properties.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name = "Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this {@code ServerModuleFile}.
     */
    public void setProperties(final Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * ModuleType
     */
    @XmlEnum(String.class)
    @XmlType(name="ServerModuleFileModuleTypeType")
    public enum ServerModuleFileModuleType {
        /**
         * A dynamically-loadable .AAR file containing one or more custom assertions.
         * <p/>
         * These are only guaranteed to work on the version of the Gateway they are built for.
         */
        @XmlEnumValue("Modular Assertion") MODULAR_ASSERTION,

        /**
         * A dynamically-loadable .JAR file containing a custom assertion.
         * <p/>
         * These are intended to work on the version of the Gateway they are developed against
         * and all future Gateway versions.
         */
        @XmlEnumValue("Custom Assertion") CUSTOM_ASSERTION
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

    @XmlElement(name = "ModuleData")
    protected AttributeExtensibleByteArray getModuleDataValue() {
        return moduleData;
    }

    protected void setModuleDataValue(final AttributeExtensibleByteArray moduleData) {
        this.moduleData = moduleData;
    }


    //- PACKAGE

    ServerModuleFileMO() {
    }


    //- PRIVATE

    private String name;
    private ServerModuleFileModuleType moduleType;
    private String moduleSha256;
    private AttributeExtensibleByteArray moduleData;
    private Map<String, String> properties;
}
