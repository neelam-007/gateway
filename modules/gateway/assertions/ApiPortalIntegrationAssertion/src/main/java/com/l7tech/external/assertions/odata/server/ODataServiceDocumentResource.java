package com.l7tech.external.assertions.odata.server;

import org.odata4j.producer.resources.ServiceDocumentResource;

/**
 * @author rraquepo, 8/26/13
 */
public class ODataServiceDocumentResource extends ServiceDocumentResource {
    private static ODataServiceDocumentResource instance;

    public static ODataServiceDocumentResource getInstance() {
        if (instance == null) {
            instance = new ODataServiceDocumentResource();
        }
        return instance;
    }
}
