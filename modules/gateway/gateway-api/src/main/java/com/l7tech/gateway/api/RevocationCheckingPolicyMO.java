package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

/**
 * The RevocationCheckingPolicyMO managed object represents a revocation
 * checking policy.
 *
 * <p>The Accessor for revocation checking policies only supports read access.
 * Revocation checking policies can be accessed by identifier or by name.</p>
 *
 * @see ManagedObjectFactory#createRevocationCheckingPolicy()
 */
@XmlRootElement(name="RevocationCheckingPolicy")
@XmlType(name="RevocationCheckingPolicyType", propOrder={"nameValue","defaultPolicy","continueOnServerUnavailable","defaultSuccess","revocationCheckItems","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="revocationCheckingPolicies")
public class RevocationCheckingPolicyMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the jdbc connection (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the jdbc connection.
     *
     * @param name The name to use
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the properties for this cluster property.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this cluster property.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }


    @XmlElement(name="RevocationCheckItems")
    public List<RevocationCheckingPolicyItemMO> getRevocationCheckItems() {
        return revocationCheckItems;
    }

    public void setRevocationCheckItems(List<RevocationCheckingPolicyItemMO> revocationCheckItems) {
        this.revocationCheckItems = revocationCheckItems;
    }

    @XmlElement(name="DefaultPolicy")
    public Boolean isDefaultPolicy() {
        return defaultPolicy == null? false: defaultPolicy;
    }

    public void setDefaultPolicy(Boolean defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    @XmlElement(name="DefaultSuccess")
    public Boolean isDefaultSuccess() {
        return defaultSuccess == null? false: defaultSuccess;
    }

    public void setDefaultSuccess(Boolean defaultSuccess) {
        this.defaultSuccess = defaultSuccess;
    }

    @XmlElement(name="ContinueOnServerUnavailable")
    public Boolean isContinueOnServerUnavailable() {
        return continueOnServerUnavailable == null? false: continueOnServerUnavailable;
    }

    public void setContinueOnServerUnavailable(Boolean continueOnServerUnavailable) {
        this.continueOnServerUnavailable = continueOnServerUnavailable;
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleType.AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleType.AttributeExtensibleString name ) {
        this.name = name;
    }

    //- PACKAGE

    RevocationCheckingPolicyMO() {
    }

    //- PRIVATE

    private AttributeExtensibleType.AttributeExtensibleString name;
    private List<RevocationCheckingPolicyItemMO> revocationCheckItems = new ArrayList<>();
    private Boolean defaultPolicy;
    private Boolean defaultSuccess;
    private Boolean continueOnServerUnavailable;
    private Map<String,Object> properties;
}
