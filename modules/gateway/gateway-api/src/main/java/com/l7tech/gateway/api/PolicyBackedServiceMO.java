package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.*;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

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
@XmlType(name="PolicyBackedServiceType", propOrder={"nameValue", "interfaceNameValue", "policyBackedServiceOperationsValues", "extension", "extensions"})
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

    public List<PolicyBackedServiceOperation> getPolicyBackedServiceOperations() {
        return get(policyBackedServiceOperations, new ArrayList<PolicyBackedServiceOperation>() );
    }

    public void setPolicyBackedServiceOperations(List<PolicyBackedServiceOperation> policyBackedServiceOperations) {
        this.policyBackedServiceOperations = set(this.policyBackedServiceOperations, policyBackedServiceOperations, AttributeExtensiblePolicyBackedServiceOperationsList.Builder);
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

    @XmlElement(name = "PolicyBackedServiceOperations")
    protected AttributeExtensiblePolicyBackedServiceOperationsList getPolicyBackedServiceOperationsValues() {
        return policyBackedServiceOperations;
    }

    protected void setPolicyBackedServiceOperationsValues(AttributeExtensiblePolicyBackedServiceOperationsList policyBackedServiceOperations) {
        this.policyBackedServiceOperations = policyBackedServiceOperations;
    }

    //- PACKAGE

    PolicyBackedServiceMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString interfaceName;
    private AttributeExtensiblePolicyBackedServiceOperationsList policyBackedServiceOperations;

    @XmlRootElement(name="PolicyBackedServiceOperation")
    @XmlType(name="PolicyBackedServiceOperationArgumentType",propOrder={"policyIdValue","operationNameValue","extension","extensions"})
    public static class PolicyBackedServiceOperation extends ElementExtensionSupport {

        //- PUBLIC

        public String getPolicyId() {
            return get(this.policyId);
        }

        public void setPolicyId(String policyId) {
            this.policyId = set(this.policyId, policyId);
        }

        public String getOperationName() {
            return get(operationName);
        }

        public void setOperationName(String operationName) {
            this.operationName = set(this.operationName, operationName);
        }

        //- PROTECTED

        @XmlElement(name = "PolicyId")
        protected AttributeExtensibleString getPolicyIdValue() {
            return policyId;
        }

        protected void setPolicyIdValue(AttributeExtensibleString policyId) {
            this.policyId = policyId;
        }

        @XmlElement(name = "OperationName")
        protected AttributeExtensibleString getOperationNameValue() {
            return operationName;
        }

        protected void setOperationNameValue(AttributeExtensibleString operationName) {
            this.operationName = operationName;
        }

        //- PRIVATE

        private AttributeExtensibleString policyId;
        private AttributeExtensibleString operationName;
    }

    @XmlType(name="PolicyBackedServiceOperationListPropertyType", propOrder={"value"})
    protected static class AttributeExtensiblePolicyBackedServiceOperationsList extends AttributeExtensibleType.AttributeExtensible<List<PolicyBackedServiceOperation>> {
        private List<PolicyBackedServiceOperation> value;

        @Override
        @XmlElement(name="PolicyBackedServiceOperation")
        public List<PolicyBackedServiceOperation> getValue() {
            return value;
        }

        @Override
        public void setValue( final List<PolicyBackedServiceOperation> value ) {
            this.value = value;
        }

        protected AttributeExtensiblePolicyBackedServiceOperationsList() {
        }

        private static final Functions.Nullary<AttributeExtensiblePolicyBackedServiceOperationsList> Builder =
                new Functions.Nullary<AttributeExtensiblePolicyBackedServiceOperationsList>(){
                    @Override
                    public AttributeExtensiblePolicyBackedServiceOperationsList call() {
                        return new AttributeExtensiblePolicyBackedServiceOperationsList();
                    }
                };
    }
}
