package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

/**
 * The PolicyBackedServiceMO managed object represents a policy backed service.
 *
 * <p>Policy Backed Services are used to execute custom policy from internal gateway processes</p>
 *
 * @see com.l7tech.gateway.api.ManagedObjectFactory#createPolicyBackedServiceMO()
 */
@XmlRootElement(name="PolicyBackedService")
@XmlType(name="PolicyBackedServiceType", propOrder={"nameValue", "interfaceNameValue", "policyBackedServiceOperationPolicyIds", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="policyBackedServices")
public class PolicyBackedServiceMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the policy backed service (case insensitive, required)
     *
     * <p>Policy backed service names are unique</p>
     *
     * @return The name of the policy backed service
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the policy backed service.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the policy backed service interface name.
     *
     * @return The interface name for the policy backed service
     */
    public String getInterfaceName() {
        return get(interfaceName);
    }

    /**
     * Set the interface name for the policy backed service.
     *
     * @param interfaceName The interface name for the policy backed service
     */
    public void setInterfaceName( final String interfaceName ) {
        this.interfaceName = set(this.interfaceName,interfaceName);
    }

    @XmlElementWrapper(name = "PolicyBackedServiceOperationPolicyIds", required=true)
    @XmlElement(name = "PolicyId", required=true)
    public List<String> getPolicyBackedServiceOperationPolicyIds() {
        return policyBackedServiceOperationPolicyIds;
    }

    public void setPolicyBackedServiceOperationPolicyIds(List<String> policyBackedServiceOperationPolicyIds) {
        this.policyBackedServiceOperationPolicyIds = policyBackedServiceOperationPolicyIds;
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="InterfaceName", required=true)
    protected AttributeExtensibleString getInterfaceNameValue() {
        return interfaceName;
    }

    protected void setInterfaceNameValue( final AttributeExtensibleString interfaceName ) {
        this.interfaceName = interfaceName;
    }

    //- PACKAGE

    PolicyBackedServiceMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString interfaceName;
    private List<String> policyBackedServiceOperationPolicyIds;
}
