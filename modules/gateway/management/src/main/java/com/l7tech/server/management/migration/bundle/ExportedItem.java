package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeaderRef;

import javax.xml.bind.annotation.*;

/**
 * An entity value exported for migration, wrapped together with its header in an ExportedItem.
 *
 * @author jbufu
 */
@XmlRootElement
//@XmlType(propOrder={"header", "value"})
public class ExportedItem {

    private EntityHeaderRef headerRef;
    private Entity value;
    private Entity mappedValue;

    protected ExportedItem() {}

    public ExportedItem(EntityHeaderRef headerRef, Entity value) {
        this.headerRef = EntityHeaderRef.fromOther(headerRef);
        this.value = value;
    }

    public EntityHeaderRef getHeaderRef() {
        return headerRef;
    }

    public void setHeaderRef(EntityHeaderRef headerRef) {
        this.headerRef = EntityHeaderRef.fromOther(headerRef);
    }

    @XmlAnyElement(lax=true)
    public Entity getValue() {
        return mappedValue != null ? mappedValue : value;
    }

    public void setValue(Entity value) {
        this.mappedValue = null;
        this.value = value;
    }

    public void setMappedValue(Entity mappedValue) {
        this.mappedValue = mappedValue;
    }

    @XmlTransient()
    public Entity getMappedValue() {
        return mappedValue;
    }

    @XmlTransient()
    public Entity getSourceValue() {
        return value;
    }

    @XmlAttribute
    public boolean isMappedValue() {
        return mappedValue != null;
    }
}
