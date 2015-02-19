package com.l7tech.gateway.common.module;

/**
 * A type of module representable by ServerModuleFile.
 */
public enum ModuleType {

    /**
     * A dynamically-loadable .AAR file containing one or more custom assertions.
     * <p/>
     * These are only guaranteed to work on the version of the Gateway they are built for.
     */
    MODULAR_ASSERTION("Modular Assertion"),

    /**
     * A dynamically-loadable .JAR file containing a custom assertion.
     * <p/>
     * These are intended to work on the version of the Gateway they are developed against
     * and all future Gateway versions.
     */
    CUSTOM_ASSERTION("Custom Assertion")
    ;


    private final String displayName;

    private ModuleType(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
