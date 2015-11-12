package com.l7tech.external.assertions.swagger;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import org.springframework.transaction.annotation.Transactional;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

/**
 * Admin interface to providing Swagger document parsing and downloading functionality to Manager.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@Secured
@Administrative
public interface SwaggerAdmin extends AsyncAdminMethods {

    // Exception thrown if a document could not be parsed
    public static final class InvalidSwaggerDocumentException extends Exception {
        public InvalidSwaggerDocumentException(String s) {
            super(s);
        }
    }

    /**
     * Downloads the Swagger document from the specified URL, parses it, and extracts the basic API metadata.
     *
     * @param url the location of the Swagger document
     * @return the parsed Swagger API metadata
     */
    @Transactional(readOnly = true, propagation = SUPPORTS)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    SwaggerApiMetadata retrieveApiMetadata(String url) throws InvalidSwaggerDocumentException;

    /**
     * This is the asynchronous version of the {@link #retrieveApiMetadata(String)} method.
     *
     * @param url the location of the Swagger document
     * @return the parsed Swagger API metadata
     */
    @Transactional(readOnly = true, propagation = SUPPORTS)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    JobId<SwaggerApiMetadata> retrieveApiMetadataAsync(String url);
}
