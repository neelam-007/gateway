package com.l7tech.external.assertions.odata.server;

import org.odata4j.producer.resources.MetadataResource;

/**
 * @author rraquepo, 8/26/13
 */
public class ODataMetadataResource extends MetadataResource {
    private static ODataMetadataResource instance;

    public static ODataMetadataResource getInstance() {
        if (instance == null) {
            instance = new ODataMetadataResource();
        }
        return instance;
    }
}
