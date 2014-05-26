package com.l7tech.external.assertions.jsondocumentstructure.server;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class JsonDocumentStructureValidationException extends Exception {

    public enum ConstraintViolation {
        CONTAINER_DEPTH,
        OBJECT_ENTRY_COUNT,
        ARRAY_ENTRY_COUNT,
        ENTRY_NAME_LENGTH,
        STRING_VALUE_LENGTH
    }

    private final ConstraintViolation violation;
    private final int line;
    private final int column;

    public JsonDocumentStructureValidationException(final ConstraintViolation violation,
                                                    final int line, final int column) {
        this.violation = violation;
        this.line = line;
        this.column = column;
    }

    public ConstraintViolation getViolation() {
        return violation;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
