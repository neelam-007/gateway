package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The PolicyAliasMO managed object represents a policy alias.
 *
 * <p>Policy alias are are displayed in the SecureSpan Manager and used to organize policies in folders.</p>
 *
 * <p>Policy alias can be accessed by identifier only.</p>
 *
 * @see ManagedObjectFactory#createPolicyAlias()
 */
@XmlRootElement(name="PolicyAlias")
@XmlType(name="PolicyAliasType", propOrder={"policyReference","extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="policyAliases")
public class PolicyAliasMO extends ElementExtendableAccessibleObject {

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
     * Reference to the policy
     *
     * @return The policy reference
     */
    @XmlElement(name = "PolicyReference", required=true)
    public ManagedObjectReference getPolicyReference() {
        return policyReference;
    }

    /**
     * Sets the reference to the policy. Required.
     *
     * @param policyReference The policy reference
     */
    public void setPolicyReference(ManagedObjectReference policyReference) {
        this.policyReference = policyReference;
    }

    //- PROTECTED

    //- PACKAGE

    PolicyAliasMO() {
    }

    //- PRIVATE

    private ManagedObjectReference policyReference;
    private String folderId;
}
