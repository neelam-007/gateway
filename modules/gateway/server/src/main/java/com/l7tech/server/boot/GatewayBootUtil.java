package com.l7tech.server.boot;

import com.l7tech.util.SyspropUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class exists to help refactor the GatewayBoot class. The GatewayBoot class contains many static that make it
 * very difficult to properly unit test. Add new methods or move existing methods to this util class so that they can be
 * properly unit tested. For this reason everything in here is package protected.
 */
class GatewayBootUtil {

    // Set this system property to the mode that you want the gateway to start in.
    static final String PROP_SERVER_MODE = "com.l7tech.server.config.mode";

    /**
     * Returns the mode that the gateway should start in. By default this is the TRADITIONAL mode. It can be configured
     * using a system property. If the system property is set to an invalid value an exception is thrown.
     *
     * @return The mode that the gateway should start in.
     * @throws IllegalArgumentException This is thrown if the mode set is invalid.
     */
    static GatewayBoot.Mode getMode() {
        final String mode = SyspropUtil.getString(PROP_SERVER_MODE, GatewayBoot.Mode.TRADITIONAL.name());
        try {
            return GatewayBoot.Mode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Unknown gateway mode '%s' expected one of: %s", mode, Arrays.asList(GatewayBoot.Mode.values())), e);
        }
    }


    // These are the properties and defaults for defining the enabled gateway components.
    static final String PROP_COMPONENTS = "com.l7tech.server.components";
    static final String COMPONENT_RESOURCE_RUNTIME_NAME_PREFIX = "com/l7tech/server/resources/";
    static final String COMPONENT_RESOURCE_RUNTIME_NAME_SUFFIX = "RuntimeContext.xml";
    static final String COMPONENT_RESOURCE_ADMIN_NAME_PREFIX = "com/l7tech/server/resources/";
    static final String COMPONENT_RESOURCE_ADMIN_NAME_SUFFIX = "AdminContext.xml";

    static final Set<String> DEFAULT_COMPONENTS = new LinkedHashSet<>(Arrays.asList(
            "uddi", "databaseReplicationMonitor", "processController"
    ));

    /**
     * This will return a list of the application contexts needed to load the enabled components. Components
     * can be configured via system properties.
     *
     * @param mode The mode that the gateway is starting up in.
     * @return The list of application contexts that will need to be added to the gateway.
     */
    static List<String> getComponentsContexts(final GatewayBoot.Mode mode) {
        final Set<String> components = new LinkedHashSet<>();
        final String componentsFromEnvironment = SyspropUtil.getProperty(PROP_COMPONENTS);
        components.addAll(
                componentsFromEnvironment != null ?
                        !componentsFromEnvironment.trim().isEmpty() ?
                                Arrays.stream(componentsFromEnvironment.split(","))
                                        .map(String::trim)
                                        .collect(Collectors.toList()) :
                                Collections.EMPTY_LIST :
                        DEFAULT_COMPONENTS);
        final List<String> applicationContexts = new ArrayList<>();
        for (final String component : components) {
            final String runtimePath = COMPONENT_RESOURCE_RUNTIME_NAME_PREFIX + component + COMPONENT_RESOURCE_RUNTIME_NAME_SUFFIX;
            applicationContexts.add(runtimePath);
            if (GatewayBoot.Mode.TRADITIONAL.equals(mode)) {
                final String adminPath = COMPONENT_RESOURCE_ADMIN_NAME_PREFIX + component + COMPONENT_RESOURCE_ADMIN_NAME_SUFFIX;
                applicationContexts.add(adminPath);
            }
        }
        return applicationContexts;
    }

}
