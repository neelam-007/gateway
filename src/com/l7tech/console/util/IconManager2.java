package com.l7tech.console.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.lang.ref.*;
import java.util.*;
import java.net.URL;

/**
 * The <code>IconManager</code> registers all loaded images into
 *
 * It registers all loaded images into the internal <code>Map</code>
 * as <code>SoftReference</code> instancese so Images are not loaded
 * twice.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public final class IconManager2 {
    /** singleton instance */
    protected static final IconManager2 iconManager = new IconManager2();

    /** a value that indicates that the icon does not exists */
    private static final Object NO_ICON = new Object();

    /** map of resource name to loaded icon (String, SoftRefrence (Image)) or (String, NO_ICON) */
    private final HashMap map = new HashMap();

    /** this instance classloader */
    private final ClassLoader loader = getClass().getClassLoader();

    /**
     * @return the singleton instance
     */
    public static IconManager2 getInstance() {
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
        Object img = map.get(name);

        // no icon for this name (already tested)
        if (img == NO_ICON) return null;

        if (img != null) {
            img = ((Reference)img).get();
        }
        if (img != null) return (Image)img;

        return getIcon(name, loader);
    }

    /**
     * Finds the Image as a resource with the given name usinf
     * specified classloader.
     *
     * @param name   the image resource name
     *
     * @return the <code>Image</code> or <b>null</b> if the resource
     *         cannot be found
     */
    private Image getIcon(String name, ClassLoader loader) {
        Object img = map.get(name);

        // no icon for this name (already tested)
        if (img == NO_ICON) return null;

        if (img != null) {
            // then it is SoftRefrence
            img = ((Reference)img).get();
        }

        // icon found
        if (img != null) return (Image)img;

        synchronized(map) {
            // again under the lock
            img = map.get(name);

            // no icon for this name (already tested)
            if (img == NO_ICON) return null;

            if (img != null) {
                img = ((Reference)img).get();
            }

            if (img != null)
            // cannot be NO_ICON, since it never disappears from the map.
                return (Image) img;

            // we have to load it
            URL url = loader.getResource(name);

            img = url == null ? null : Toolkit.getDefaultToolkit().createImage(url);
            if (img != null) {
                Image img2 = toBufferedImage((Image)img);

                Reference r = new SoftReference(img2);
                map.put(name, r);
                return img2;
            } else {
                // no icon found
                map.put(name, NO_ICON);
                return null;
            }
        }
    }

    /**
     * The method creates a BufferedImage which represents
     * the same Image as the parameter but consumes less memory.
     */
    static final Image toBufferedImage(Image img) {
        // load the image
        new ImageIcon(img);
        BufferedImage rep = createBufferedImage(img.getWidth(null), img.getHeight(null));
        Graphics g = rep.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        img.flush();
        return rep;
    }

    /** Creates BufferedImage with Transparency.BITMASK */
    private static final BufferedImage createBufferedImage(int width, int height) {
        ColorModel model =
          GraphicsEnvironment.
          getLocalGraphicsEnvironment().
          getDefaultScreenDevice().
          getDefaultConfiguration().
          getColorModel(java.awt.Transparency.BITMASK);

        BufferedImage buffImage =
          new BufferedImage(model,
            model.createCompatibleWritableRaster(width, height),
            model.isAlphaPremultiplied(), null);
        return buffImage;
    }
}
