package com.l7tech.gateway.common.module;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * Holds a Custom or Modular Assertion Module information.
 */
public final class AssertionModuleInfo implements Serializable {
    private static final long serialVersionUID = -6306494289836086182L;

    /**
     * The filename of the module, ie "RateLimitAssertion-3.7.0.jar".
     * */
    private final String moduleFileName;

    /**
     * In case the module has been uploaded using the Policy Manager (i.e. if the module is a {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile}),
     * this represents the {@code ServerModuleFile} entity name.
     */
    @Nullable private final String moduleEntityName;

    /**
     * The hex encoded digest of the module file, currently SHA256
     * */
    private final String moduleDigest;

    /**
     * The assertion ClassName(s) provided by this module, ie { "com.yoyodyne.integration.layer7.SqlSelectAssertion" }.
     * */
    @NotNull private final Collection<String> assertionClasses;

    /**
     * Construct {@code AssertionModuleInfo} with the specified attributes.
     *
     * @param moduleFileName      the module file name.
     * @param moduleEntityName    the module entity name, in case the module has been uploaded using the Policy manager
     *                            (i.e. if the module is a {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile}).
     *                            Optional and can be {@code null} if the module has not been uploaded using the Policy manager.
     * @param moduleDigest        the checksum of thi module file (currently SHA256).
     * @param assertionClasses    the collection of assertions (i.e. assertion class names) registered by the module.
     *                            Required and cannot be {@code null}.
     */
    public AssertionModuleInfo(
            final String moduleFileName,
            @Nullable final String moduleEntityName,
            final String moduleDigest,
            @NotNull final Collection<String> assertionClasses
    ) {
        this.moduleFileName = moduleFileName;
        this.moduleDigest = moduleDigest;
        this.assertionClasses = assertionClasses;
        this.moduleEntityName = moduleEntityName;
    }

    /**
     * Getter for {@link #moduleFileName}
     */
    public String getModuleFileName() {
        return moduleFileName;
    }

    /**
     * Getter for {@link #moduleEntityName}
     */
    @Nullable
    public String getModuleEntityName() {
        return moduleEntityName;
    }

    /**
     * Getter for {@link #moduleDigest}
     */
    public String getModuleDigest() {
        return moduleDigest;
    }

    /**
     * Read-only view of assertion ClassName(s) provided by this module.
     *
     * @return Unmodifiable collection of this module assertion ClassName(s).
     */
    @NotNull
    public Collection<String> getAssertionClasses() {
        return Collections.unmodifiableCollection(assertionClasses);
    }

    /**
     * Indicates whether the module has been uploaded using the Policy manager
     * (i.e. if the module is a {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile}).
     */
    public boolean isFromDb() {
        return (moduleEntityName != null && moduleEntityName.length() > 0);
    }
}
