package com.l7tech.proxy.gui.util;

import com.l7tech.proxy.gui.Gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: May 29, 2003
 * Time: 2:25:29 PM
 * To change this template use Options | File Templates.
 */
public class IconManager {
    private static final ClassLoader cl = IconManager.class.getClassLoader();

    private static ImageIcon iconAdd;
    private static ImageIcon iconEdit;
    private static ImageIcon iconRemove;
    private static Image iconAppIcon;
    private static ImageIcon splashImage;

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

    public static Image getAppImage() {
        if (iconAppIcon == null)
            iconAppIcon = loadImage(Gui.RESOURCE_PATH + "/layer7_logo_small_32x32.png");
        return iconAppIcon;
    }

    public static ImageIcon getSplashImageIcon() {
        if (splashImage == null)
            splashImage = loadImageIcon(Gui.RESOURCE_PATH + "/agent_splash.png");
        return splashImage;
    }
}
