package com.l7tech.gateway.common.module;

/**
 * Represents a {@link ServerModuleFile module} state.
 */
public enum ModuleState {
    /**
     * Indicates that the module upload process has completed.
     * Typically this state is the initial state once a module is created or its data (hash and bytes) is updated.
     */
    @SuppressWarnings("JavadocReference") UPLOADED("Uploaded"),

    /**
     * Indicates that the module is successfully copied into the staging folder.
     */
    STAGED("Staged"),

    /**
     * Indicates that the module is successfully copied into the corresponding modules folder (depending on the type).
     */
    DEPLOYED("Deployed"),

    /**
     * Indicates that the OS Level Service, service on the Gateway side responsible for modules installation,
     * rejected the module based on its native criteria.
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
    public String toString() {
        return displayName;
    }
}
