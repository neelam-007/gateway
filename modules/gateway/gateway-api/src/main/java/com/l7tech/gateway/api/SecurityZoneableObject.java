package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * Abstract base class for all accessible objects that support SecurityZones.
 */
@XmlType(name="SecurityZoneableObjectType")
public abstract class SecurityZoneableObject extends AccessibleObject {

    //- PUBLIC

    @XmlAttribute(name="securityZoneId", required=false)
    public String getSecurityZoneId() {
        return securityZoneId;
    }

    public void setSecurityZoneId(String securityZoneId) {
        this.securityZoneId = securityZoneId;
    }

    public String getSecurityZone() {
        return get(securityZone);
    }

    public void setSecurityZone(String securityZone) {
        set(this.securityZone, securityZone);
    }

//    @XmlElement(name="SecurityZone", required=false)
    protected AttributeExtensibleString getSecurityZoneValue() {
        return securityZone;
    }

    protected void setSecurityZoneValue(AttributeExtensibleString securityZone) {
        this.securityZone = securityZone;
    }

    //- PRIVATE

    private String securityZoneId;
    private AttributeExtensibleString securityZone;
}
