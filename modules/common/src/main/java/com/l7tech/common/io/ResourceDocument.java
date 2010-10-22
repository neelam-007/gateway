package com.l7tech.common.io;

import java.io.IOException;
import java.net.URI;

/**
 * Interface for document resources.
 */
public interface ResourceDocument {

    /**
     * Get the URI for this resource document.
     *
     * @return The URI (never null)
     */
    URI getUri();

    /**
     * Get the content for this resource document.
     *
     * @return The content (never null)
     * @throws IOException If an error occurs.
     */
    String getContent() throws IOException;

    /**
     * Get the resource document relative to this document.
     *
     * @param path The relative path to the document.
     * @param resolver The resolver to use (optional)
     * @return The resource document (never null)
     * @throws IOException If an error occurs or the resource is not found.
     */
    ResourceDocument relative( String path, ResourceDocumentResolver resolver ) throws IOException;

    /**
     * Is the content currently available for this resource.
     *
     * @return True if the content is available.
     */
    boolean available();

    /**
     * Does this resource exist (can the content be retrieved)
     *
     * @return True if this resource exists.
     * @throws IOException If an error occurs.
     */
    boolean exists() throws IOException;

    /**
     * Get the last modified time for the resource if available.
     *
     * @return The last modified time (0 if unknown)
     */
    long getLastModified();

}
