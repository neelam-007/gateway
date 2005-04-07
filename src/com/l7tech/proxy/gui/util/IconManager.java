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
    private static Image iconAppIcon;
    private static ImageIcon trustedSsgDiagram;
    private static ImageIcon federatedSsgDiagram;
    private static ImageIcon federatedSsgWithTokenServiceDiagram;
    private static ImageIcon smtrustedSsgDiagram;
    private static ImageIcon smfederatedSsgDiagram;
    private static ImageIcon smfederatedSsgWithTokenServiceDiagram;

    private static ImageIcon loadImageIcon(String path) {
        URL url = cl.getResource(path);
        return url == null ? new ImageIcon() : new ImageIcon(url);
    }

    private static Image loadImage(String path) {
        URL url = cl.getResource(path);
        return url == null ? (new ImageIcon()).getImage()
                           : Toolkit.getDefaultToolkit().createImage(url);
    }

    public static ImageIcon getTrustedSsgDiagram() {
        if (trustedSsgDiagram == null)
            trustedSsgDiagram = loadImageIcon(Gui.RESOURCE_PATH + "/dia_trusted_ssg.png");
        return trustedSsgDiagram;
    }

    public static ImageIcon getSmallTrustedSsgDiagram() {
        if (smtrustedSsgDiagram == null)
            smtrustedSsgDiagram = loadImageIcon(Gui.RESOURCE_PATH + "/dia_small_trusted_ssg.png");
        return smtrustedSsgDiagram;
    }

    public static ImageIcon getFederatedSsgDiagram() {
        if (federatedSsgDiagram == null)
            federatedSsgDiagram = loadImageIcon(Gui.RESOURCE_PATH + "/dia_federated_ssg.png");
        return federatedSsgDiagram;
    }

    public static ImageIcon getSmallFederatedSsgDiagram() {
        if (smfederatedSsgDiagram == null)
            smfederatedSsgDiagram = loadImageIcon(Gui.RESOURCE_PATH + "/dia_small_federated_ssg.png");
        return smfederatedSsgDiagram;
    }

    public static ImageIcon getFederatedSsgWithTokenServiceDiagram() {
        if (federatedSsgWithTokenServiceDiagram == null) {
            String path = "/dia_federated_ssg_with_tokenservice.png";
            if (Boolean.getBoolean("noibm") || Boolean.getBoolean("interoperability") || Boolean.getBoolean("interop"))
                path = "/dia_federated_ssg_with_tokenservice_noibm.png";
            federatedSsgWithTokenServiceDiagram = loadImageIcon(Gui.RESOURCE_PATH + path);
        }
        return federatedSsgWithTokenServiceDiagram;
    }

    public static ImageIcon getSmallFederatedSsgWithTokenServiceDiagram() {
        if (smfederatedSsgWithTokenServiceDiagram == null) {
            String path = "/dia_small_federated_ssg_with_tokenservice.png";
            if (Boolean.getBoolean("noibm") || Boolean.getBoolean("interoperability") || Boolean.getBoolean("interop"))
                path = "/dia_small_federated_ssg_with_tokenservice_noibm.png";
            smfederatedSsgWithTokenServiceDiagram = loadImageIcon(Gui.RESOURCE_PATH + path);
        }
        return smfederatedSsgWithTokenServiceDiagram;
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
            splashImage = loadImageIcon(Gui.RESOURCE_PATH + "/bridge_splash.gif");
        return splashImage;
    }

    public static ImageIcon getSmallSplashImageIcon() {
        return getSplashImageIcon();
    }
}
