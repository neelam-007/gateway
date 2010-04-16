/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.xml.tarari;

import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;

/**
 * Tarari-specific functionality for schema validation.
 */
public interface TarariSchemaHandler {
    /**
     * Replace all schemas currently in hardware with a new set.  The first thing this method will do is unload
     * all existing schemas from the hardware: caller is responsible for ensuring that
     * all existing schemas, including those passed in as the hardwareSchemas argument,
     * have already had their "loaded on hardware" flag cleared before this method is called.
     * <p/>
     * Even though it doesn't clear them, this method will take care of setting the "loaded on hardware" flag
     * for schemas that were loaded successfully.
     *
     * @param hardwareSchemas a map (systemId -> TarariSchemaSource) of the new set of schemas that the hardware should use.  Must not be null.
     * @return a map(TarariSchemSource -> error) of the schemas that could not be loaded due to errors, and the errors that caused the problem.
     *         May be empty, but never null.
     */
    Map<TarariSchemaSource,Exception> setHardwareSchemas( HashMap<String,? extends TarariSchemaSource> hardwareSchemas );

    /**
     * Validate the given document in hardware using the current schemas, if possible to do so.
     *
     * @param tarariMsg the document to validate.  Must not be null.
     * @return <code>true</code> if the document was validated; <code>false</code> if the document was invalid.
     */
    boolean validate(TarariMessageContext tarariMsg) throws SAXException;
}
