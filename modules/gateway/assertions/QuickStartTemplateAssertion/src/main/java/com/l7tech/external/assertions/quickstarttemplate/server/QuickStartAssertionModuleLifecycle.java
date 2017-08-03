package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.annotations.VisibleForTesting;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.AssertionMapper;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.GatewayState;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public class QuickStartAssertionModuleLifecycle {
    private static final Logger logger = Logger.getLogger(QuickStartAssertionModuleLifecycle.class.getName());
    private static final String PROVIDED_FRAGMENT_FOLDER_GOID = "2a97ddf9a6e77162832b9c27bc8f57e0";
    @VisibleForTesting
    static final String QUICKSTART_SCALER_ENABLED_PROPERTY = "quickStart.scaler.enabled";
    @VisibleForTesting
    static final boolean QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_CLUSTER_PROPERTY_IS_MISSING = false;
    @VisibleForTesting
    static final boolean QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY = true;

    private final QuickStartAssertionLocator assertionLocator;
    private final QuickStartServiceBuilder serviceBuilder;
    private final QuickStartJsonServiceInstaller jsonServiceInstaller;

    /**
     * Our singleton instance
     */
    private static class InstanceHolder {
        private static QuickStartAssertionModuleLifecycle INSTANCE;
    }

    @VisibleForTesting
    QuickStartAssertionModuleLifecycle(
            @NotNull final ApplicationContext context,
            @NotNull final QuickStartAssertionLocator assertionLocator,
            @NotNull final QuickStartServiceBuilder serviceBuilder,
            @NotNull final QuickStartJsonServiceInstaller jsonServiceInstaller
    ) {
        this.assertionLocator = assertionLocator;
        this.serviceBuilder = serviceBuilder;
        this.jsonServiceInstaller = jsonServiceInstaller;

        initializeJsonInstaller(context);
    }

    @SuppressWarnings("unused")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (InstanceHolder.INSTANCE == null) {
            final FolderManager folderManager = context.getBean("folderManager", FolderManager.class);
            final EncapsulatedAssertionConfigManager encassManager = context.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);
            final ServiceManager serviceManager = context.getBean("serviceManager", ServiceManager.class);
            final ServiceCache ServiceCache = context.getBean("serviceCache", ServiceCache.class);
            final PolicyVersionManager policyVersionManager = context.getBean("policyVersionManager", PolicyVersionManager.class);
            final ClusterPropertyManager clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);

            final AssertionMapper assertionMapper = new AssertionMapper();
            final QuickStartAssertionLocator assertionLocator = new QuickStartAssertionLocator(encassManager, assertionMapper, folderManager, new Goid(PROVIDED_FRAGMENT_FOLDER_GOID));
            final QuickStartPublishedServiceLocator serviceLocator = new QuickStartPublishedServiceLocator(serviceManager);
            final QuickStartServiceBuilder serviceBuilder = new QuickStartServiceBuilder(ServiceCache, folderManager, serviceLocator, assertionLocator, clusterPropertyManager, assertionMapper);
            final QuickStartJsonServiceInstaller jsonServiceInstaller = new OneTimeJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager, new QuickStartParser());

            InstanceHolder.INSTANCE = new QuickStartAssertionModuleLifecycle(context, assertionLocator, serviceBuilder, jsonServiceInstaller);
        }
    }

    public static void onModuleLoaded(final ApplicationContext context, QuickStartServiceBuilder serviceBuilder) {
        if (InstanceHolder.INSTANCE == null) {
            final FolderManager folderManager = context.getBean("folderManager", FolderManager.class);
            final EncapsulatedAssertionConfigManager encassManager = context.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);
            final ServiceManager serviceManager = context.getBean("serviceManager", ServiceManager.class);
            final ServiceCache ServiceCache = context.getBean("serviceCache", ServiceCache.class);
            final PolicyVersionManager policyVersionManager = context.getBean("policyVersionManager", PolicyVersionManager.class);
            final ClusterPropertyManager clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);

            final AssertionMapper assertionMapper = new AssertionMapper();
            final QuickStartAssertionLocator assertionLocator = new QuickStartAssertionLocator(encassManager, assertionMapper, folderManager, new Goid(PROVIDED_FRAGMENT_FOLDER_GOID));
            final QuickStartPublishedServiceLocator serviceLocator = new QuickStartPublishedServiceLocator(serviceManager);
            //final QuickStartServiceBuilder serviceBuilder = new QuickStartServiceBuilder(ServiceCache, folderManager, serviceLocator, assertionLocator, clusterPropertyManager, assertionMapper);
            final QuickStartJsonServiceInstaller jsonServiceInstaller = new OneTimeJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager, new QuickStartParser());

            InstanceHolder.INSTANCE = new QuickStartAssertionModuleLifecycle(context, assertionLocator, serviceBuilder, jsonServiceInstaller);
        }
    }

    public static void reset() {
        InstanceHolder.INSTANCE = null;
    }

    /**
     * Utility method to initialize {@link #jsonServiceInstaller}.
     *
     * @param context    {@link ApplicationContext}.  Required and cannot be {@code null}.
     */
    private void initializeJsonInstaller(@NotNull final ApplicationContext context) {
        if (!isQuickStartScalerEnabled(context)) {
            // Check if the Gateway is ready for messages
            final GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
            assert gatewayState != null;
            if (gatewayState.isReadyForMessages()) {
                // our module is dynamically loaded after the gateway startup
                installJsonServices();
            } else {
                // wait for the gateway to become ready for messages (our module is loaded with gateway startup)
                final ApplicationEventProxy applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
                final ApplicationListener applicationListener = event -> {
                    if (event instanceof ReadyForMessages) {
                        installJsonServices();
                    }
                };
                applicationEventProxy.addApplicationListener(applicationListener);
            }
        } else {
            logger.fine("Skipping bootstrapped JSON services as QuickStart Scaler is enabled!");
        }
    }

    /**
     * Utility method that actually invokes {@link QuickStartJsonServiceInstaller#installJsonServices()} and
     * loges any exception thrown (though there shouldn't be any at this point).
     */
    private void installJsonServices() {
        try {
            jsonServiceInstaller.installJsonServices();
        } catch (final Throwable e) {
            logger.log(Level.SEVERE, ExceptionUtils.getDebugException(e), () -> "Unhandled exception while installing JSON services: " + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * Utility method that checks if QuickStart scalar is enabled or not.<br/>
     * Current implementation reads {@link #QUICKSTART_SCALER_ENABLED_PROPERTY scaler enabled} cluster property to determine
     * whether scalar is enabled or not, defaults to {@link #QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_CLUSTER_PROPERTY_IS_MISSING}
     * when the cluster property doesn't exist and defaults to {@link #QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY}
     * when an error occurs while retrieving the cluster property.
     *
     * @param context    the {@link ApplicationContext}.  Required and cannot be {@code null}.
     * @return the value of {@link #QUICKSTART_SCALER_ENABLED_PROPERTY scaler enabled} cluster property, or {@link #QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_CLUSTER_PROPERTY_IS_MISSING}
     * if the cluster property doesn't exist or {@link #QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY}
     * wif n error occurs while retrieving the cluster property.
     */
    private boolean isQuickStartScalerEnabled(@NotNull final ApplicationContext context) {
        try {
            final ClusterPropertyManager clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
            final String scalerEnabledClusterPropertyValue = clusterPropertyManager != null ? clusterPropertyManager.getProperty(QUICKSTART_SCALER_ENABLED_PROPERTY) : null;
            return scalerEnabledClusterPropertyValue != null ? Boolean.valueOf(scalerEnabledClusterPropertyValue.trim()) : QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_CLUSTER_PROPERTY_IS_MISSING;
        } catch (final FindException e) {
            logger.log(Level.WARNING, ExceptionUtils.getDebugException(e), () -> "Error while reading \"" + QUICKSTART_SCALER_ENABLED_PROPERTY + "\" cluster property: " + ExceptionUtils.getMessageWithCause(e));
        } catch (final Throwable e) {
            logger.log(Level.WARNING, ExceptionUtils.getDebugException(e), () -> "Unhandled Exception while reading \"" + QUICKSTART_SCALER_ENABLED_PROPERTY + "\" cluster property: " + ExceptionUtils.getMessageWithCause(e));
        }

        return QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY;
    }

    @SuppressWarnings("unused")
    public static synchronized void onModuleUnloaded() {
        // Do nothing; these shared resources are used by both the template and the documentation assertion.
    }

    @Nullable
    @Contract(pure = true)
    static QuickStartAssertionLocator getEncapsulatedAssertionLocator() {
        return InstanceHolder.INSTANCE != null ? InstanceHolder.INSTANCE.assertionLocator : null;
    }

    @Nullable
    @Contract(pure = true)
    static QuickStartServiceBuilder getServiceBuilder() {
        return InstanceHolder.INSTANCE != null ? InstanceHolder.INSTANCE.serviceBuilder : null;
    }
}
