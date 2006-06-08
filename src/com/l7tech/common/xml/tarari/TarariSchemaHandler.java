/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.SoftwareFallbackException;
import org.xml.sax.SAXException;

import java.util.ConcurrentModificationException;

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
    void loadHardware(String systemId, String schemaDocument)
            throws SoftwareFallbackException, SAXException;

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

    /**
     * Set the resolver that will be used for recursively loading referenced schemas by the next loadHardware() call.
     * Caller is responsible for ensuring that no (possibly-recursive) loadHardware() call is currently under way
     * when this method is called.
     * <p/>
     * All calls to setTarariSchemaResolver are paired with exactly
     * one root-level call to loadHardware for a particular schema and its dependencies
     * (that is, that no recursively-invoked loadHardware calls try to change the
     * schema resolver in effect)
     *
     * @param resolver  the resolver to use for the next call to loadHardware.  Must not be null.
     * @throws ConcurrentModificationException if a call to loadHardware is presently under way.
     */
    void setTarariSchemaResolver(TarariSchemaResolver resolver) throws ConcurrentModificationException;
}
