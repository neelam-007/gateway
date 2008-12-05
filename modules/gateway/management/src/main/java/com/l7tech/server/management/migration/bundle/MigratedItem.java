package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.EntityHeader;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Captures the result of a migration operation for an item, in the form of a header and a status string.
 *
 * @author jbufu
 */
@XmlRootElement
public class MigratedItem {
    private EntityHeader header;
    private String status;

    public MigratedItem() {}

    public MigratedItem(EntityHeader header, String status) {
        this.header = header;
        this.status = status;
    }

    public EntityHeader getHeader() {
        return header;
    }

    public void setHeader(EntityHeader header) {
        this.header = header;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
