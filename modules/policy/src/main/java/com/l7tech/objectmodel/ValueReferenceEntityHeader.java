package com.l7tech.objectmodel;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author jbufu
 */
@XmlRootElement
public class ValueReferenceEntityHeader extends EntityHeader {

    private EntityType ownertype;
    private String propertyName;

    public ValueReferenceEntityHeader() {
        this.ownertype = EntityType.ANY;
    }

    public ValueReferenceEntityHeader(EntityHeader owner, String propertyName) {
        super(owner.getStrId(), EntityType.VALUE_REFERENCE, owner.getName() + " : " + propertyName, "");
        this.ownertype = owner.getType();
        this.propertyName = propertyName;
    }

    @XmlAttribute
    public EntityType getOwnertype() {
        return ownertype;
    }

    public void setOwnertype(EntityType ownertype) {
        this.ownertype = ownertype;
    }

    @XmlAttribute
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
}
