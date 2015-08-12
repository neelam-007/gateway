package com.l7tech.server.policy.module;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base holder for assertion jar-files, both modular and custom.
 * <br/>
 * All shared entities between modular and custom assertion modules goes here.
 */
public class BaseAssertionModule<T extends ClassLoader> {

    @NotNull private final String name;
    private final long modifiedTime;
    @NotNull private final String digest;
    @NotNull private final T classLoader;

    /**
     * In case the module has been uploaded using the Policy manager (i.e. if the module is a {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile}),
     * this represents the {@code ServerModuleFile} entity name.
     */
    @Nullable private String entityName;

    /**
     * Create assertion module.
     *
     * @param moduleName      the module filename.  Required and cannot be {@code null}.
     * @param modifiedTime    the module last modified timestamp.
     * @param moduleDigest    the module content checksum (currently SHA256). Required and cannot be {@code null}.
     * @param classLoader     the module class loader. Required and cannot be {@code null}.
     */
    public BaseAssertionModule(
            @SuppressWarnings("NullableProblems") final String moduleName,
            final long modifiedTime,
            @SuppressWarnings("NullableProblems") final String moduleDigest,
            @SuppressWarnings("NullableProblems") final T classLoader
    ) {
        if (StringUtils.isBlank(moduleName)) {
            throw new IllegalArgumentException("non-empty moduleName required");
        }
        if (StringUtils.isBlank(moduleDigest)) {
            throw new IllegalArgumentException("non-empty moduleDigest required");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader required");
        }

        this.name = moduleName;
        this.modifiedTime = modifiedTime;
        this.digest = moduleDigest;
        this.classLoader = classLoader;
    }

    /**
     * @return the name of this assertion module, ie "RateLimitAssertion-3.7.0.jar".
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * @return the modification time of the file when this module was read.
     */
    public long getModifiedTime() {
        return modifiedTime;
    }

    /**
     * @return the hex-encoded digest of this assertion module file (currently SHA256).
     */
    @NotNull
    public String getDigest() {
        return digest;
    }

    /**
     * @return the ClassLoader providing classes for this jar file.
     */
    @NotNull
    public T getModuleClassLoader() {
        return classLoader;
    }

    /**
     * Getter for {@link #entityName}
     */
    @Nullable
    public String getEntityName() {
        return entityName;
    }

    /**
     * Setter for {@link #entityName}.
     */
    public void setEntityName(@Nullable final String entityName) {
        this.entityName = entityName;
    }

    /**
     * Indicates whether the module has been uploaded using the Policy manager
     * (i.e. if the module is a {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile}).
     */
    public boolean isFromDb() {
        return (entityName != null && entityName.length() > 0);
    }
}
