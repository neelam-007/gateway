package com.l7tech.console.util;

import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
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
}
