package com.l7tech.gateway.common.module;

/**
 * Represents a {@link ServerModuleFile module} state.
 */
public enum ModuleState {
    /**
     * Indicates that the module upload process has completed.
     * Typically this state is the initial state once a module is created or its data (hash and bytes) is updated.
     */
    UPLOADED("Uploaded"),

    /**
     * Indicates that module signature is verified, however the module could not be loaded due to errors.
     */
    ACCEPTED("Accepted"),

    /**
     * Indicates that the module signature was not verified and the module has been rejected.
     */
    REJECTED("Rejected"),

    /**
     * Finally the module has been successfully loaded into the specified Gateway node.
     */
    LOADED("Loaded"),

    /**
     * Indicates that an error happen while staging or deploying the module.
     */
    ERROR("Error")
    ;

    private final String displayName;

    private ModuleState(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public final String toString() {
        return displayName;
    }
}
