package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client utility class for reading {@link com.l7tech.gateway.common.module.ServerModuleFile} known cluster wide props.
 */
public class ServerModuleFileClusterPropertiesReader {
    private static final Logger logger = Logger.getLogger(ServerModuleFileClusterPropertiesReader.class.getName());

    static final String CLUSTER_PROP_UPLOAD_MAX_SIZE = "serverModuleFile.upload.maxSize";
    static final long DEFAULT_SERVER_MODULE_FILE_UPLOAD_MAXSIZE = 20971520L;

    static final String CLUSTER_PROP_UPLOAD_ENABLE = "serverModuleFile.upload.enable";
    static final boolean DEFAULT_SERVER_MODULE_FILE_UPLOAD_ENABLE = false;

    /**
     * default instance
     */
    private static ServerModuleFileClusterPropertiesReader instance;


    /**
     * Static method to obtain the global locator.
     */
    @NotNull
    public static synchronized ServerModuleFileClusterPropertiesReader getInstance() {
        if (instance == null) {
            instance = new ServerModuleFileClusterPropertiesReader();
        }
        return instance;
    }


    /**
     * Actual extraction of the property.
     *
     * @param propertyName         the cluster property name to get.  Required and cannot be {@code null}.
     * @param defaultValue         property default value.  Required and cannot be {@code null}.
     * @param propertyConverter    property value converter.  Required and cannot be {@code null}.
     * @return the store cluster property value or the specified {@code defaultValue}, never {@code null}.
     */
    private <T> T getProperty(
            @NotNull final String propertyName,
            @NotNull T defaultValue,
            @NotNull final Functions.Unary<Either<Throwable,T>, String> propertyConverter
    ) {
        if (Registry.getDefault().isAdminContextPresent()) {
            final ClusterStatusAdmin clusterAdmin = Registry.getDefault().getClusterStatusAdmin();
            // first get the default value
            for (final ClusterPropertyDescriptor desc : clusterAdmin.getAllPropertyDescriptors()) {
                if (propertyName.equals(desc.getName())) {
                    try {
                        defaultValue = Eithers.extract(propertyConverter.call(desc.getDefaultValue()));
                    } catch (final Throwable e) {
                        logger.log(Level.SEVERE, "Error getting cluster property \"" + propertyName + "\" default value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                    break;
                }
            }

            try {
                final ClusterProperty prop = clusterAdmin.findPropertyByName(propertyName);
                if (prop != null) {
                    try {
                        return Eithers.extract(propertyConverter.call(prop.getValue()));
                    } catch (final Throwable e) {
                        logger.log(Level.SEVERE, "Error getting cluster property \"" + propertyName + "\" value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
            } catch (final FindException e) {
                logger.log(Level.SEVERE, "Error accessing cluster property \"" + propertyName + "\": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
        return defaultValue;
    }

    /**
     * Determine whether Modules upload is enabled or not.
     * This is determined by checking the cluster wide property "serverModuleFile.upload.enable".
     */
    public boolean isModulesUploadEnabled() {
        return getProperty(
                CLUSTER_PROP_UPLOAD_ENABLE,
                DEFAULT_SERVER_MODULE_FILE_UPLOAD_ENABLE,
                new Functions.Unary<Either<Throwable, Boolean>, String>() {
                    @Override
                    public Either<Throwable, Boolean> call(final String value) {
                        try {
                            return Either.right(Boolean.parseBoolean(TextUtils.trim(value)));
                        } catch (final Throwable e) {
                            return Either.left(e);
                        }
                    }
                }
        );
    }


    /**
     * Get the maximum Server Module File Size to be uploaded (in bytes), or 0 for unlimited (Integer).
     * This is determined by checking the cluster wide property "serverModuleFile.upload.maxSize".
     */
    public long getModulesUploadMaxSize() {
        return getProperty(
                CLUSTER_PROP_UPLOAD_MAX_SIZE,
                DEFAULT_SERVER_MODULE_FILE_UPLOAD_MAXSIZE,
                new Functions.Unary<Either<Throwable, Long>, String>() {
                    @Override
                    public Either<Throwable, Long> call(final String value) {
                        try {
                            return Either.right(Long.parseLong(TextUtils.trim(value)));
                        } catch (final Throwable e) {
                            return Either.left(e);
                        }
                    }
                }
        );
    }
}
