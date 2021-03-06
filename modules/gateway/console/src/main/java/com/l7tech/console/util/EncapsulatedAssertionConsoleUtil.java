package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for GUI code that works with encapsulated assertions.
 */
public final class EncapsulatedAssertionConsoleUtil {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionConsoleUtil.class.getName());

    private EncapsulatedAssertionConsoleUtil() {}

    public static enum IconType {
        /** The default image used if no icon is configured for encass config. */
        DEFAULT_IMAGE,

        /** An icon loaded from the SSM's resource directory, specified by a filename in the encass config. */
        INTERNAL_RESOURCE,

        /** A custom image, specified by a base64-encoded image file within the encass config. */
        CUSTOM_IMAGE
    }

    /**
     * Find an icon to display for the specified encapsulated assertion config.
     *
     * @param config the config to examine.  Required.
     * @return an icon type + an icon.  Never null.
     */
    @NotNull
    public static Pair<IconType, ImageIcon> findIcon(@NotNull EncapsulatedAssertionConfig config) {
        String iconResourceFilename = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_RESOURCE_FILENAME);
        String imageBase64 = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64);
        return findIcon(iconResourceFilename, imageBase64);
    }

    /**
     * Find an icon to display for the specified encapsulated assertion config properties.
     *
     * @param iconResourceFilename filename of icon resource, or null.
     * @param imageBase64 Base-64 encoded icon image, or null.
     * @return an icon type + an icon.  Never null.
     */
    @NotNull
    public static Pair<IconType, ImageIcon> findIcon(@Nullable String iconResourceFilename, @Nullable String imageBase64) {
        ImageIcon ret = null;
        // If a resource path is set to a loadable resource, it takes precedence.
        if (iconResourceFilename != null) {
            ImageIcon icon = ImageCache.getInstance().getIconAsIcon(EncapsulatedAssertionConfig.ICON_RESOURCE_DIRECTORY + iconResourceFilename);
            if (icon != null) {
                icon.setDescription(iconResourceFilename);
                return new Pair<IconType, ImageIcon>(IconType.INTERNAL_RESOURCE, icon);
            }
        }

        // Otherwise, check for a base64-ed image
        if (imageBase64 != null && imageBase64.trim().length() > 0) {
            try {
                byte[] imageBytes = HexUtils.decodeBase64(imageBase64, true);
                ImageIcon icon = new ImageIcon(ImageCache.getInstance().createUncachedBufferedImage(imageBytes, Transparency.BITMASK));
                icon.setDescription("Custom Encapsulated Assertion Icon");
                return new Pair<IconType, ImageIcon>(IconType.CUSTOM_IMAGE, icon);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Invalid icon image: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        // Use the default icon.
        final ImageIcon icon = ImageCache.getInstance().getIconAsIcon(EncapsulatedAssertionConfig.ICON_RESOURCE_DIRECTORY + EncapsulatedAssertionConfig.DEFAULT_ICON_RESOURCE_FILENAME);
        icon.setDescription(EncapsulatedAssertionConfig.DEFAULT_ICON_RESOURCE_FILENAME);
        return new Pair<IconType, ImageIcon>(IconType.DEFAULT_IMAGE, icon);
    }

    /**
     * Tries to attach as many backing policies as possible to the given EncapsulatedAssertionConfigs if they are currently detached.
     *
     * If the EncapsulatedAssertionConfig already has a Policy then it will not be re-attached.
     *
     * Policies may not be attached if:
     * 1. caller does not have permission to access the backing policy
     * 2. no backing policy can be found for the EncapsulatedAssertionConfig
     *
     * @param configs the EncapsulatedAssertionConfigs for which to attach backing policies.
     * @throws FindException if unable to retrieve a backing Policy.
     */
    public static void attachPolicies(@NotNull Collection<EncapsulatedAssertionConfig> configs) throws FindException {
        final PolicyAdmin policyAdmin = Registry.getDefault().getPolicyAdmin();

        // reduce the number of calls over the network to improve Policy Manager post-login response time
        // get all policy guids to fetch once over the network (save on request and response overhead for each individual policy call)
        final List<String> policyGuids = new ArrayList<>(configs.size());
        for (final EncapsulatedAssertionConfig config : configs) {
            policyGuids.add(config.getProperty(EncapsulatedAssertionConfig.PROP_POLICY_GUID));
        }
        // rbac may filter results
        final List<Policy> policies = policyAdmin.findPoliciesByGuids(policyGuids);

        int policyIndex = 0;
        for (final EncapsulatedAssertionConfig config : configs) {
            if (config.getPolicy() == null) {
                final String policyGuid = config.getProperty(EncapsulatedAssertionConfig.PROP_POLICY_GUID);
                if (policyGuid != null) {
                    try {
                        Policy backingPolicy = null;

                        // rbac may filter, resulting in a shorter policy list
                        if (policies.size() > policyIndex) {
                            backingPolicy = policies.get(policyIndex);
                        }

                        if (backingPolicy != null && policyGuid.equals(backingPolicy.getGuid())) {
                            policyIndex++;   // match found (policy list is in same order as config)
                            config.setPolicy(backingPolicy);
                        } else {
                            logger.log(Level.WARNING, "Policy not found for configuration guid: " + policyGuid +
                                    ". Caller likely does not have permission to retrieve the backing policy.");
                        }
                    } catch (final PermissionDeniedException e) {
                        logger.log(Level.WARNING, "Caller does not have permission to retrieve the backing policy with guid: " + policyGuid,
                                ExceptionUtils.getDebugException(e));
                    }
                }
            }
        }
    }

    /**
     * Determine the name to display for the given Policy or an appropriate message to display if the Policy is null.
     *
     * @param policy the Policy for which to determine a display name (may not necessarily be the current backing policy set on the given EncapsulatedAssertionConfig).
     *               If null, the EncapsulatedAssertionConfig will be used to determine what to display.
     * @param config the EncapsulatedAssertionConfig which uses or will use the given Policy.
     * @return a display name for the given Policy or an appropriate message if the Policy is null.
     */
    public static String getPolicyDisplayName(@Nullable final Policy policy, @NotNull final EncapsulatedAssertionConfig config) {
        final String name;
        if (policy != null) {
            name = policy.getName();
        } else if (config.getGuid() == null) {
            // new encass
            name = NOT_CONFIGURED;
        } else {
            // existing encass but policy could not be attached
            name = POLICY_UNAVAILABLE;
        }
        return name;
    }

    /**
     * Determine the name or message to display for the backing Policy of the given EncapsulatedAssertionConfig.
     *
     * If the backing Policy is null, an appropriate message will be returned.
     *
     * @param config the EncapsulatedAssertionConfig which holds the backing Policy.
     * @return a name or message to display for the backing Policy of the given EncapsulatedAssertionConfig.
     */
    public static String getPolicyDisplayName(@NotNull final EncapsulatedAssertionConfig config) {
        return getPolicyDisplayName(config.getPolicy(), config);
    }

    static final String NOT_CONFIGURED = "<Not Configured>";
    static final String POLICY_UNAVAILABLE = "<Policy Unavailable>";
}
