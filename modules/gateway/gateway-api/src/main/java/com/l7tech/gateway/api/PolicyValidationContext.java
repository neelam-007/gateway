package com.l7tech.gateway.api;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import com.l7tech.gateway.api.impl.AttributeExtensiblePolicyType;
import com.l7tech.gateway.api.impl.ElementExtendableManagedObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * Policy validation parameters.
 *
 * <p>The context encapsulates the information required to perform validation
 * for a persistent or ephemeral policy.</p>
 *
 * <p>When validating a persistent policy no context information is required.</p>
 */
@XmlRootElement(name="PolicyValidationContext")
@XmlType(name="PolicyValidationContextType", propOrder={"policyTypeValue", "properties", "resourceSets", "extension", "extensions"})
public class PolicyValidationContext extends ElementExtendableManagedObject {

    //- PUBLIC

    /**
     * Get the type of the policy.
     *
     * <p>This will be null for service policies.</p>
     *
     * @return The type or null.
     */
    public PolicyDetail.PolicyType getPolicyType() {
        return get(policyType);
    }

    /**
     * Set the type of the policy.
     *
     * @param policyType The type to use.
     */
    public void setPolicyType( final PolicyDetail.PolicyType policyType ) {
        this.policyType = setNonNull( this.policyType==null ? new AttributeExtensiblePolicyType() : this.policyType, policyType);
    }

    /**
     * Get the properties for the context.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the context.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    /**
     * The resource sets for the context.
     *
     * <p>The resource sets can be for "policy" and "wsdl" documents.</p>
     *
     * <p>The "policy" resource set can include both the main policy and any
     * referenced policy include fragments.</p>
     *
     * @return The resource sets or null.
     */
    @XmlElementWrapper(name="Resources")
    @XmlElement(name="ResourceSet", required=true)
    public List<ResourceSet> getResourceSets() {
        return resourceSets;
    }

    /**
     * Set the resource sets for the context.
     *
     * @param resourceSets The resource sets to use.
     */
    public void setResourceSets( final List<ResourceSet> resourceSets ) {
        this.resourceSets = resourceSets;
    }

    //- PROTECTED

    @XmlElement(name="PolicyType")
    protected AttributeExtensiblePolicyType getPolicyTypeValue() {
        return policyType;
    }

    protected void setPolicyTypeValue( final AttributeExtensiblePolicyType policyType ) {
        this.policyType = policyType;
    }

    //- PACKAGE

    PolicyValidationContext() {
    }

    //- PRIVATE

    private AttributeExtensiblePolicyType policyType;
    private Map<String,Object> properties;
    private List<ResourceSet> resourceSets;
}
