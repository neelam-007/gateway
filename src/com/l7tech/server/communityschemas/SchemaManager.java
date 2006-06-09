/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.http.cache.HttpObjectCache;

import java.util.regex.Pattern;

public interface SchemaManager {

    /**
     * Get the {@link com.l7tech.server.communityschemas.CompiledSchema} for the specified schema document, reusing an existing instance
     * if possible.  If the schema was loaded from a URL, that URL should be supplied as a
     * System ID, so that imports using relative URIs can be resolved.
     * @param   schemadoc the XML Schema Document to get a CompiledSchema for. Must not be null.
     * @param   systemId  the System ID from which the document was loaded. May be null or empty.
     * @return  a SchemaHandle for the given document.  Never null.
     * @throws  com.l7tech.common.xml.InvalidDocumentFormatException if the schema or a dependent could not be compiled
     */
    SchemaHandle compile(String schemadoc,
                         String systemId,
                         Pattern[] urlWhitelist)
            throws InvalidDocumentFormatException;

    /** @return the HttpObjectCache that is being used to fetch schemas */
    HttpObjectCache getHttpObjectCache();
}
