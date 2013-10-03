package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.ManagedObjectReference;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The ServiceAliasMO managed object represents a service alias.
 *
 * <p>Service alias are are displayed in the SecureSpan Manager and used to organize service in folders.</p>
 *
 * <p>Service alias can be accessed by identifier only.</p>
 *
 * @see ManagedObjectFactory#createServiceAlias()
 */
@XmlRootElement(name="ServiceAlias")
@XmlType(name="ServiceAliasType", propOrder={"serviceReference","extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="serviceAliases")
public class ServiceAliasMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the identifier for the parent folder.
     *
     * <p>A null parent folder means the folder is a root folder.</p>
     *
     * @return The containing folder identifier (may be null)
     */
    @XmlAttribute
    public String getFolderId() {
        return folderId;
    }

    /**
     * Set the folder identifier.
     *
     * @param folderId The containing folder identifier (null for the root folder)
     */
    public void setFolderId( final String folderId ) {
        this.folderId = folderId;
    }

    /**
     * Reference to the service
     *
     * @return The service reference
     */
    @XmlElement(name = "ServiceReference", required=true)
    public ManagedObjectReference getServiceReference() {
        return serviceReference;
    }

    /**
     * Sets the reference to the policy. Required.
     *
     * @param serviceReference The policy reference
     */
    public void setServiceReference(ManagedObjectReference serviceReference) {
        this.serviceReference = serviceReference;
    }

    //- PROTECTED

    //- PACKAGE

    ServiceAliasMO() {
    }

    //- PRIVATE

    private ManagedObjectReference serviceReference;
    private String folderId;
}
