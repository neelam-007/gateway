/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.SoftwareFallbackException;
import org.xml.sax.SAXException;

/**
 * Tarari-specific functionality for schema validation
 */
public interface TarariSchemaHandler {
    /**
     * Loads the provided schema document to hardware.
     * @param systemId the systemId from which the was downloaded.
     * @param schemaDocument the schema document to load.
     * @throws SoftwareFallbackException if the schema document cannot be loaded to hardware.
     * @throws SAXException if the schema document is not well-formed.
     */
    void loadHardware(String systemId, String schemaDocument) throws SoftwareFallbackException, SAXException;

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
