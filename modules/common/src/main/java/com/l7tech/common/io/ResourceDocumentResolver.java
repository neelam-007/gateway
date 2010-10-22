package com.l7tech.common.io;

import java.io.IOException;

/**
 * Interface for resolving resource documents by identifiers.
 */
public interface ResourceDocumentResolver {

    /**
     * Resolve a resource document by URI.
     *
     * @param uri The URI for the document (must not be null)
     * @return the resource or null.
     * @throws IOException If an error occurs.
     */
    ResourceDocument resolveByUri( String uri ) throws IOException;

    /**
     * Resolve an XML Schema resource by target namespace.
     *
     * @param targetNamespace The targetNamespace to resolved (may be null)
     * @return the schema resource or null.
     * @throws IOException If an error occurs.
     */
    ResourceDocument resolveByTargetNamespace( String targetNamespace ) throws IOException;

    /**
     * Resolve a resource by its public identifier.
     *
     * @param publicId the public identifier for the resource.
     * @eturn the resource or null.
     * @throws IOException If an error occurs.
     */
    ResourceDocument resolveByPublicId( String publicId ) throws IOException;
}
