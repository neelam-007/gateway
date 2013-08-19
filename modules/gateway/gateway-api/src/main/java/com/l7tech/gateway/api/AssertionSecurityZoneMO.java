package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The AssertionSecurityZoneMO object represents an assignment of a security zone to a gateway assertion.
 */
@XmlRootElement(name="AssertionSecurityZone")
@XmlType(name="AssertionAccessType", propOrder={"nameValue", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "assertionSecurityZones")
public class AssertionSecurityZoneMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    public String getName() {
        return get(name);
    }

    public void setName(String name) {
        this.name = set(this.name, name);
    }

    //- PROTECTED

    @XmlElement(name="Name",required=true)
    protected AttributeExtensibleString getNameValue() {
        return this.name;
    }

    protected void setNameValue(AttributeExtensibleString name) {
        this.name = name;
    }

    //- PACKAGE

    AssertionSecurityZoneMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;

}
