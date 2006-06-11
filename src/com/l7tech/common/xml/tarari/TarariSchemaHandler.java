/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import org.xml.sax.SAXException;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Tarari-specific functionality for schema validation
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
     * @param hardwareSchemas a map (tns -> handle) of the new set of schemas that the hardware should use.  Must not be null.
     *                        Must already be topologically sorted such that, if schema B depends on schema A, schema A
     *                        appears first in the (ordered) LinkedHashMap.
     * @return a map(tns -> error) of the schema TNSs that could not be loaded due to errors, and the errors that caused the problem.
     *         May be empty, but never null.
     */
    Map<? extends TarariSchemaSource, Exception> setHardwareSchemas(LinkedHashMap<String, ? extends TarariSchemaSource> hardwareSchemas);

    /**
     * Validate the given document in hardware using the current schemas, if possible to do so.
     *
     * @param tarariMsg the document to validate.  Must not be null.
     * @return <code>true</code> if the document was validated; <code>false</code> if the document was invalid.
     */
    boolean validate(TarariMessageContext tarariMsg) throws SAXException;
}
