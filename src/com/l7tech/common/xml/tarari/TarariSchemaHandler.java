/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.InvalidDocumentFormatException;

/**
 * Tarari-specific functionality for schema validation
 */
public interface TarariSchemaHandler {
    /**
     * Loads the provided schema document to hardware.
     * @param targetNamespace the targetNamespace from the schema to be loaded.  Must be unique system-wide.
     * @param schemaDocument the schema document to load.
     * @throws com.l7tech.common.xml.InvalidDocumentFormatException if the schema document cannot be loaded to hardware.
     */
    void loadHardware(String targetNamespace, String schemaDocument) throws InvalidDocumentFormatException;

    /**
     * Unloads the schema document with the provided targetNamespace from hardware.
     * @param targetNamespace the targetNamespace of the schema to unload.
     */
    void unloadHardware(String targetNamespace);

    /**
     * Validate the given document in hardware using the current schemas, if possible to do so.
     *
     * @param tarariMsg the document to validate.  Must not be null.
     * @return <code>true</code> if the document was validated; <code>false</code> if the document was invalid.
     */
    boolean validate(TarariMessageContext tarariMsg);
}
