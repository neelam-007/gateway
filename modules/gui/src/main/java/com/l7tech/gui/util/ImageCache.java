package com.l7tech.gui.util;

import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>ImageCache</code> registers all loaded images into the
 * internal <code>Map</code> as <code>SoftReference</code> instancese
 * so Images are not loaded twice.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public final class ImageCache {
    protected static final Logger logger = Logger.getLogger(ImageCache.class.getName());

    /** singleton instance */
    protected static final ImageCache iconManager = new ImageCache();

    /** map of resource name to loaded icon (String, SoftRefrence (Image)) or (String, NO_ICON) */
    private final Map<String, Reference<Image>> imageMap = new ConcurrentHashMap<String, Reference<Image>>();

    private final Map<String, Reference<ImageIcon>> iconMap = new ConcurrentHashMap<String, Reference<ImageIcon>>();

    /** this instance classloader */
    private final ClassLoader loader = ImageCache.class.getClassLoader();

    /**
     * @return the singleton instance
     */
    public static ImageCache getInstance() {
        return iconManager;
    }

    /**
     * Finds the Image as a resource with the given name.
     *
     * @param name   the image resource name
     *
     * @return the <code>Image</code> or <b>null</b> if the resource
     *         cannot be found
     */
    public Image getIcon(String name) {
        if (name == null) return null;
        return getIcon(name, loader);
    }

    /**
     * Same as getIcon but returns the cached image as a cached ImageIcon instance.
     *
     * @param name   the image resource name
     *
     * @return the <code>Icon</code> or <b>null</b> if the resource
     *         cannot be found
     */
    public ImageIcon getIconAsIcon(String name) {
        if (name == null) return null;
        return getIconAsIcon(name, loader);
    }

    /**
     * Find the Image as an Icon looking for a resource with the given name
     * in the specified classloader.
     *
     * @param name    name of the resource
     * @param loader  classloader to load from.  required
     * @return the Icon or null if not found
     */
    public ImageIcon getIconAsIcon(String name, ClassLoader loader) {
        if (name == null) return null;

        Reference<ImageIcon> iconref = iconMap.get(name);
        if (iconref != null) {
            ImageIcon icon = iconref.get();
            if (icon != null)
                return icon;
        }

        // Have to create it
        Image image = getIcon(name, loader);
        if (image == null) return null;

        ImageIcon icon = new ImageIcon(image);
        iconMap.put(name, new SoftReference<ImageIcon>(icon));
        return icon;
    }

    /**
     * Finds the Image as a resource with the given name using
     * specified classloader.
     *
     * @param name   the image resource name
     * @param loader  a specific classloader to use for the image.  Required.
     * @return the <code>Image</code> or <b>null</b> if the resource
     *         cannot be found
     */
    public Image getIcon(String name, ClassLoader loader) {
        return getIcon( name, loader, java.awt.Transparency.BITMASK );
    }

    /**
     * Finds the Image as a resource with the given name using
     * specified classloader.
     *
     * @param name   the image resource name
     * @param loader  a specific classloader to use for the image.  Required.
     * @param transparency the transparency to use with the image.
     * @return the <code>Image</code> or <b>null</b> if the resource
     *         cannot be found
     */
    public Image getIcon(String name, ClassLoader loader, int transparency) {
        if (name == null) return null;

        Reference<Image> imgref = imageMap.get(name);

        if (imgref != null) {
            Image image = imgref.get();
            if (image != null)
                return image;
        }

        InputStream stream = loader.getResourceAsStream(name);
        if (stream != null) {
            try {
                final Image image = ImageIO.read(stream);
                final Reference<Image> imgRef = new SoftReference<>(image);
                imageMap.put(name, imgRef);
                return image;
            } catch (final Exception e) {
                logger.log( Level.WARNING, "Unable to load image resource " + name + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException( e ) );
            }
        }
        return null;
    }

    /**
     * Attempt to decode the specified bytes as an image in a supported format, and convert the result to a buffered image.
     * <p/>
     * This method does not enroll anything into the cache.
     *
     * @param imageBytes bytes of the image, in some supported format (eg, GIF, PNG, JPG).  Required.
     * @param transparency transparency mode for the image, eg java.awt.Transparency.BITMASK or Transparency.TRANSLUCENT.
     * @return a BufferedImage ready to render to a graphics context.  Never null.
     */
    public BufferedImage createUncachedBufferedImage(@NotNull byte[] imageBytes, int transparency) {
        return toBufferedImage(Toolkit.getDefaultToolkit().createImage(imageBytes), transparency);
    }

    /**
     * The method creates a BufferedImage which represents
     * the same Image as the parameter but consumes less memory.
     */
    static BufferedImage toBufferedImage(Image img, int transparency) {
        // load the image
        new ImageIcon(img);
        BufferedImage rep = createBufferedImage(img.getWidth(null), img.getHeight(null), transparency);
        Graphics g = rep.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        img.flush();
        return rep;
    }

    /** Creates BufferedImage with Transparency.BITMASK */
    private static BufferedImage createBufferedImage(int width, int height, int transparency) {
        ColorModel model =
          GraphicsEnvironment.
          getLocalGraphicsEnvironment().
          getDefaultScreenDevice().
          getDefaultConfiguration().
          getColorModel(transparency);

        BufferedImage buffImage =
          new BufferedImage(model,
            model.createCompatibleWritableRaster(width, height),
            model.isAlphaPremultiplied(), null);
        return buffImage;
    }
}
