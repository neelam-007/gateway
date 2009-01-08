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
    private EntityHeader sourceHeader;
    private EntityHeader targetHeader;
    public static enum ImportOperation { CREATE, UPDATE, IGNORE }
    private ImportOperation operation;

    public MigratedItem() {}

    public MigratedItem(EntityHeader sourceHeader, EntityHeader targetHeader, ImportOperation operation) {
        this.sourceHeader = sourceHeader;
        this.targetHeader = targetHeader;
        this.operation = operation;
    }

    public EntityHeader getTargetHeader() {
        return targetHeader;
    }

    public void setTargetHeader(EntityHeader targetHeader) {
        this.targetHeader = targetHeader;
    }

    public EntityHeader getSourceHeader() {
        return sourceHeader;
    }

    public void setSourceHeader(EntityHeader sourceHeader) {
        this.sourceHeader = sourceHeader;
    }

    public ImportOperation getOperation() {
        return operation;
    }

    public void setOperation(ImportOperation operation) {
        this.operation = operation;
    }
}
