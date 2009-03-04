package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.ExternalEntityHeader;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * An entity value exported for migration, wrapped together with its header, and optionally a mapped value,
 * in an ExportedItem.
 *
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder={"header", "value"})
public class ExportedItem implements Serializable {

    private ExternalEntityHeader header;
    private Entity value;

    protected ExportedItem() {}

    public ExportedItem(ExternalEntityHeader header, Entity value) {
        this.header = header;
        this.value = value;
    }

    public ExternalEntityHeader getHeader() {
        return header;
    }

    public void setHeader(ExternalEntityHeader header) {
        this.header = header;
    }

    @XmlAnyElement(lax=true)
    public Entity getValue() {
        return value;
    }

    public void setValue(Entity value) {
        this.value = value;
    }
}
