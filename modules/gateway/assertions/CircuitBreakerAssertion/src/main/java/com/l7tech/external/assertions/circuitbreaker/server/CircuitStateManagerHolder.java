package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
class CircuitStateManagerHolder {
    private static CircuitStateManager circuitStateManager;

    static CircuitStateManager getCircuitStateManager() {
        return circuitStateManager;
    }

    static void setCircuitStateManager(CircuitStateManager circuitStateManager) {
        CircuitStateManagerHolder.circuitStateManager = circuitStateManager;
    }
}
