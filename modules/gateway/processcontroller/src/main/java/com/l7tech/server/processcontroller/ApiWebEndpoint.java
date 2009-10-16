package com.l7tech.server.processcontroller;

/**
 * Web endpoints exposed by the process controller for various APIs.
 *
 * The property names defined here are used for defining the endpoint paths in processcontroller.properties.
 */
public enum ApiWebEndpoint {

    PROCESS_CONTROLLER("processcontroller.api.endpoint"),
    NODE_MANAGEMENT("node.management.api.endpoint"),
    OS("os.api.endpoint"),
    PATCH_SERVICE("patch.service.api.endpoint"),
    MONITORING("monitoring.api.endpoint");

    ApiWebEndpoint(String propName) {
        this.propName = propName;
    }

    public String getPropName() {
        return propName;
    }

    // - PRIVATE

    private String propName;
}
