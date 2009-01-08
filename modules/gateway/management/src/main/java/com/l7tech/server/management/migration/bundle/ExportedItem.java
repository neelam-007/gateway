package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;

import javax.xml.bind.annotation.*;

/**
 * An entity value exported for migration, wrapped together with its header, and optionally a mapped value,
 * in an ExportedItem.
 *
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"header", "value"})
public class ExportedItem {

    private EntityHeader header;
    private Entity value;
    private Entity mappedValue;

    protected ExportedItem() {}

    public ExportedItem(EntityHeader header, Entity value) {
        this.header = header;
        this.value = value;
    }

    public EntityHeader getHeader() {
        return header;
    }

    public void setHeader(EntityHeader header) {
        this.header = header;
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
