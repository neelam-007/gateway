package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * The PolicyMO managed object represents a policy.
 *
 * <p>The Accessor for policies supports read and write. Policies can be
 * accessed by name or identifier.</p>
 *
 * @see ManagedObjectFactory#createPolicy()
 */
@XmlRootElement(name="Policy")
@XmlType(name="PolicyType", propOrder={"policyDetail", "resourceSets", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="policies", accessorClassname="com.l7tech.gateway.api.impl.PolicyMOAccessorImpl")
public class PolicyMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    @Override
    public Integer getVersion() {
        Integer version = null;

        if ( policyDetail != null ) {
            version = policyDetail.getVersion();
        }

        return version;
    }

    @Override
    public void setVersion( final Integer version ) {
        if ( policyDetail != null ) {
            policyDetail.setVersion( version );
        }
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
     * Get the details for the policy (required)
     *
     * @return The policy details or null.
     */
    @XmlElement(name="PolicyDetail", required=true)
    public PolicyDetail getPolicyDetail() {
        return policyDetail;
    }

    /**
     * Set the details for the policy.
     *
     * @param policyDetail The details to use.
     */
    public void setPolicyDetail( final PolicyDetail policyDetail ) {
        this.policyDetail = policyDetail;
    }

    /**
     * Get the resource sets for the policy (required)
     *
     * <p>The policy document for a policy is a ResourceSet with tag 'policy'
     * containing a Resource of type 'policy'.</p>
     *
     * @return The resource sets or null.
     */
    @XmlElementWrapper(name="Resources", required=true)
    @XmlElement(name="ResourceSet", required=true)
    public List<ResourceSet> getResourceSets() {
        return resourceSets;
    }

    /**
     * Set the resources for the policy.
     *
     * @param resourceSets The resource sets to use.
     */
    public void setResourceSets( final List<ResourceSet> resourceSets ) {
        this.resourceSets = resourceSets;
    }

    //- PACKAGE

    PolicyMO() {
    }

    //- PRIVATE

    private String guid;
    private PolicyDetail policyDetail;
    private List<ResourceSet> resourceSets;
}
