package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Details for a PolicyMO.
 *
 * <p>The following properties can be used:
 * <ul>
 *   <li><code>revision</code> (read only): The policy revision number.</li>
 *   <li><code>soap</code>: True if the policy is for SOAP message processing.</li>
 *   <li><code>tag</code>: The tag to use for the policy (internal policies only)</li>
 * </ul>
 * </p>
 *
 * @see PolicyMO
 * @see PolicyMOAccessor#getPolicyDetail(String)
 * @see PolicyMOAccessor#putPolicyDetail(String, PolicyDetail)
 * @see ManagedObjectFactory#createPolicyDetail()
 */
@XmlType(name="PolicyDetailType", propOrder={"name","policyType","extensions","properties"})
public class PolicyDetail {

    //- PUBLIC

    /**
     * Get the identifier for the policy.
     *
     * @return The identifier or null.
     */
    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    /**
     * Set the identifier for the policy.
     *
     * @param id The identifier to use.
     */
    public void setId( final String id ) {
        this.id = id;
    }

    /**
     * Get the GUID for the policy.
     *
     * @return The GUID or null.
     */
    @XmlAttribute(name="guid")
    public String getGuid() {
        return guid;
    }

    /**
     * Set the GUID for the policy.
     *
     * @param guid The GUID to use.
     */
    public void setGuid( final String guid ) {
        this.guid = guid;
    }

    /**
     * Get the version for the policy.
     *
     * <p>The version is distinct from the policy revision.</p>
     *
     * @return The policy version or null.
     */
    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    /**
     * Set the version for the policy.
     *
     * @param version The version to use.
     */
    public void setVersion( final Integer version ) {
        this.version = version;
    }

    /**
     * Get the identifier of the Folder containing the policy.
     *
     * @return The folder identifier or null.
     * @see FolderMO
     */
    @XmlAttribute(name="folderId")
    public String getFolderId() {
        return folderId;
    }

    /**
     * Set the identifier of the Folder containing the policy.
     *
     * @param folderId The folder identifier to use.
     */
    public void setFolderId( final String folderId ) {
        this.folderId = folderId;
    }

    /**
     * Get the name for the policy (case insensitive, unique, required)
     *
     * @return The name for the policy or null
     */
    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the policy.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = name;
    }

    /**
     * Get the type for the policy (required)
     *
     * @return The policy type or null.
     */
    @XmlElement(name="PolicyType", required=true)
    public PolicyType getPolicyType() {
        return policyType;
    }

    /**
     * Set the type for the policy.
     *
     * @param policyType The policy type to use.
     */
    public void setPolicyType( final PolicyType policyType ) {
        this.policyType = policyType;
    }

    /**
     * Get the properties for the policy.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the policy.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    /**
     * Type for policies.
     */
    @XmlEnum(String.class)
    public enum PolicyType {
        /**
         * Policy Include Fragment
         */
        @XmlEnumValue("Include") INCLUDE,

        /**
         * Internal Use Policy
         */
        @XmlEnumValue("Internal") INTERNAL
    }

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PACKAGE

    PolicyDetail() {
    }

    //- PRIVATE

    private String id;
    private String guid;
    private Integer version;
    private String folderId;
    private String name;
    private PolicyType policyType;
    private Map<String,Object> properties;
    private Map<QName,Object> attributeExtensions;
    private List<Object> extensions;
}
