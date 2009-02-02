package com.l7tech.server.management.migration.bundle;

import com.l7tech.objectmodel.ExternalEntityHeader;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Captures the result of a migration operation for an item, in the form of a header and a status string.
 *
 * @author jbufu
 */
@XmlRootElement
public class MigratedItem {
    private ExternalEntityHeader sourceHeader;
    private ExternalEntityHeader targetHeader;
    public static enum ImportOperation { CREATE, UPDATE, IGNORE }
    private ImportOperation operation;

    public MigratedItem() {}

    public MigratedItem(ExternalEntityHeader sourceHeader, ExternalEntityHeader targetHeader, ImportOperation operation) {
        this.sourceHeader = sourceHeader;
        this.targetHeader = targetHeader;
        this.operation = operation;
    }

    public ExternalEntityHeader getTargetHeader() {
        return targetHeader;
    }

    public void setTargetHeader(ExternalEntityHeader targetHeader) {
        this.targetHeader = targetHeader;
    }

    public ExternalEntityHeader getSourceHeader() {
        return sourceHeader;
    }

    public void setSourceHeader(ExternalEntityHeader sourceHeader) {
        this.sourceHeader = sourceHeader;
    }

    public ImportOperation getOperation() {
        return operation;
    }

    public void setOperation(ImportOperation operation) {
        this.operation = operation;
    }
}
