package com.l7tech.proxy.gui.util;

import com.l7tech.proxy.gui.Gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Provide easy access to the Client Proxy application-specific images.
 *
 * User: mike
 * Date: May 29, 2003
 * Time: 2:25:29 PM
 */
public class IconManager {
    private static final ClassLoader cl = IconManager.class.getClassLoader();

    private static ImageIcon iconAdd;
    private static ImageIcon iconEdit;
    private static ImageIcon iconRemove;
    private static ImageIcon iconDefault;
    private static ImageIcon iconCert;
    private static ImageIcon splashImage;
    private static ImageIcon smallSplashImage;
    private static Image iconAppIcon;

    private static ImageIcon loadImageIcon(String path) {
        URL url = cl.getResource(path);
        return url == null ? new ImageIcon() : new ImageIcon(url);
    }

    private static Image loadImage(String path) {
        URL url = cl.getResource(path);
        return url == null ? (new ImageIcon()).getImage()
                           : Toolkit.getDefaultToolkit().createImage(url);
    }

    public static ImageIcon getAdd() {
        if (iconAdd == null)
            iconAdd = loadImageIcon(Gui.RESOURCE_PATH + "/New16.gif");
        return iconAdd;
    }

    public static ImageIcon getEdit() {
        if (iconEdit == null)
            iconEdit = loadImageIcon(Gui.RESOURCE_PATH + "/Edit16.gif");
        return iconEdit;
    }

    public static ImageIcon getRemove() {
        if (iconRemove == null)
            iconRemove = loadImageIcon(Gui.RESOURCE_PATH + "/Delete16.gif");
        return iconRemove;
    }

    public static ImageIcon getDefault() {
        if (iconDefault == null) {
            iconDefault = loadImageIcon(Gui.RESOURCE_PATH + "/Default16.gif");
        }
        return iconDefault;
    }

    public static ImageIcon getCert() {
        if (iconCert == null)
            iconCert = loadImageIcon(Gui.RESOURCE_PATH + "/cert16.gif");
        return iconCert;
    }

    public static Image getAppImage() {
        if (iconAppIcon == null)
            iconAppIcon = loadImage(Gui.RESOURCE_PATH + "/layer7_logo_small_32x32.png");
        return iconAppIcon;
    }

    public static ImageIcon getSplashImageIcon() {
        if (splashImage == null)
            splashImage = loadImageIcon(Gui.RESOURCE_PATH + "/agent_splash.gif");
        return splashImage;
    }

    public static ImageIcon getSmallSplashImageIcon() {
        return getSplashImageIcon();
    }
}
