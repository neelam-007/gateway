package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.PolicyReferenceInstruction;
import com.l7tech.gateway.api.Resource;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlRootElement(name="PolicyImportContext")
@XmlType(name="PolicyImportContextType", propOrder={"properties", "resource", "policyReferenceInstructions", "extensions"})
public class PolicyImportContext extends ManagedObject {

    //- PUBLIC

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    @XmlElement(name="Resource", required=true)
    public Resource getResource() {
        return resource;
    }

    public void setResource( final Resource resource ) {
        this.resource = resource;
    }

    @XmlElementWrapper(name="PolicyReferenceInstructions")
    @XmlElement(name="PolicyReferenceInstruction")
    public List<PolicyReferenceInstruction> getPolicyReferenceInstructions() {
        return policyReferenceInstructions;
    }

    public void setPolicyReferenceInstructions( final List<PolicyReferenceInstruction> policyReferenceInstructions ) {
        this.policyReferenceInstructions = policyReferenceInstructions;
    }

    //- PROTECTED

    @Override
    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PRIVATE

    private Map<String,Object> properties;
    private Resource resource;
    private List<PolicyReferenceInstruction> policyReferenceInstructions;

}
