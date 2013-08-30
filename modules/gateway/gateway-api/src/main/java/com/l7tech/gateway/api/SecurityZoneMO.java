package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The SecurityZoneMO object represents a Security Zone configuration.
 */
@XmlRootElement(name="SecurityZone")
@XmlType(name="SecurityZoneType", propOrder={"nameValue", "descriptionValue", "permittedEntityTypesValue", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "securityZones")
public class SecurityZoneMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the security zone (case insensitive, required)
     *
     * @return  The name of the security zone (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the security zone.
     *
     * @param name The name to use.
     */
    public void setName(String name) {
        this.name = set(this.name, name);
    }

    /**
     * Get the description for the security zone
     *
     * @return  The description of the security zone (may be null)
     */
    public String getDescription() {
        return get(description);
    }

    /**
     * Set the description for the security zone.
     *
     * @param description The description to use.
     */
    public void setDescription(String description) {
        this.description = set(this.description, description);
    }

    /**
     * Get the permitted entity types of this security zone
     *
     * @return  The list of permitted entity types (never null)
     */
    public List<String> getPermittedEntityTypes() {
        return unwrap(get( permittedEntityTypes, new ArrayList<AttributeExtensibleString>() ));
    }

    /**
     * Set the permitted entity tyes for this security zone
     *
     * <p>Recognised entity types are:
     * <ul>
     *   <li><code>ANY</code></li>
     *   <li><code>ID_PROVIDER_CONFIG</code></li>
     *   <li><code>SERVICE</code></li>
     *   <li><code>SERVICE_ALIAS</code></li>
     *   <li><code>JMS_CONNECTION</code></li>
     *   <li><code>JMS_ENDPOINT</code></li>
     *   <li><code>TRUSTED_CERT</code></li>
     *   <li><code>REVOCATION_CHECK_POLICY</code></li>
     *   <li><code>SSG_KEY_ENTRY</code></li>
     *   <li><code>SSG_KEY_METADATA</code></li>
     *   <li><code>SAMPLE_MESSAGE</code></li>
     *   <li><code>POLICY</code></li>
     *   <li><code>POLICY_ALIAS</code></li>
     *   <li><code>FOLDER</code></li>
     *   <li><code>ENCAPSULATED_ASSERTION</code></li>
     *   <li><code>AUDIT_MESSAGE</code></li>
     *   <li><code>SSG_CONNECTOR</code></li>
     *   <li><code>UDDI_REGISTRY</code></li>
     *   <li><code>UDDI_PROXIED_SERVICE_INFO</code></li>
     *   <li><code>UDDI_SERVICE_CONTROL</code></li>
     *   <li><code>JDBC_CONNECTION</code></li>
     *   <li><code>EMAIL_LISTENER</code></li>
     *   <li><code>SSG_ACTIVE_CONNECTOR</code></li>
     *   <li><code>LOG_SINK</code></li>
     *   <li><code>SECURE_PASSWORD</code></li>
     *   <li><code>HTTP_CONFIGURATION</code></li>
     *   <li><code>RESOURCE_ENTRY</code></li>
     *   <li><code>ASSERTION_ACCESS</code></li>
     *   <li><code>SITEMINDER_CONFIGURATION</code></li>
     * </ul>
     * </p>
     *
     * @param permittedEntityTypes The permitted entity types
     */
    public void setPermittedEntityTypes(List<String> permittedEntityTypes) {
        this.permittedEntityTypes = set(this.permittedEntityTypes, wrap(permittedEntityTypes, AttributeExtensibleStringBuilder));
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

    @XmlElement(name = "Name", required = true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name = "Description", required = true)
    protected AttributeExtensibleString getDescriptionValue() {
        return description;
    }

    protected void setDescriptionValue( final AttributeExtensibleString description ) {
        this.description = description;
    }

    @XmlElement(name = "PermittedEntityTypes", required = true)
    protected AttributeExtensibleStringList getPermittedEntityTypesValue() {
        return permittedEntityTypes;
    }

    protected void setPermittedEntityTypesValue(AttributeExtensibleStringList permittedEntities) {
        this.permittedEntityTypes = permittedEntities;
    }

    //- PACKAGE

    SecurityZoneMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString description;
    private AttributeExtensibleStringList permittedEntityTypes;

}
