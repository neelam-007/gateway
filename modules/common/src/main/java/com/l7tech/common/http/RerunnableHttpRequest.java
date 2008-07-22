package com.l7tech.common.http;

import java.io.InputStream;

/**
 * Request interface for use by clients that support request re-running.
 *
 * @author $Author$
 * @version $Revision$
 */
public interface RerunnableHttpRequest extends GenericHttpRequest {

    /**
     * Produces an InputStream.  Provide one of these to {@link RerunnableHttpRequest}
     * to avoid request buffering.
     */
    static interface InputStreamFactory {

        /**
         * @return an InputStream ready to play back the request body for a POST request.
         */
        InputStream getInputStream();
    }

    /**
     * Set the input stream factory.
     *
     * @param isf the factory to use.
     */
    void setInputStreamFactory(InputStreamFactory isf);

}
