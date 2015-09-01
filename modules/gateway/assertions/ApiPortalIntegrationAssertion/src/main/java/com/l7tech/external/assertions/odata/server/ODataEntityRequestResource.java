package com.l7tech.external.assertions.odata.server;

import org.odata4j.producer.resources.EntityRequestResource;

/**
 * @author rraquepo, 8/22/13
 */
public class ODataEntityRequestResource extends EntityRequestResource {
    private static ODataEntityRequestResource instance;

    public static ODataEntityRequestResource getInstance() {
        if (instance == null) {
            instance = new ODataEntityRequestResource();
        }
        return instance;
    }

}
