package com.l7tech.console.util;

import com.l7tech.console.tree.*;
import com.l7tech.console.MainWindow;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.util.WeakSet;

import javax.swing.*;

/**
 * A singleton class that contains icon resources.
 * todo: rework this with weak cache and icons lazy loading
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class IconManager {
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();
    private static IconManager instance = new IconManager();

    public static IconManager getInstance() {
        return instance;
    }

    /**
     * default constructor
     */
    protected IconManager() {
        loadimages();
    }

    /**
     * Get the Icon for the BasicTreeNode passed.
     *
     * @param node   the BasicTreeNode instance
     * @return ImageIcon for the given node
     */
    public ImageIcon getIcon(BasicTreeNode node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        ClassLoader cl = node.getClass().getClassLoader();
        Class clazz = node.getClass();
        if (clazz.equals(ProvidersFolderNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/providers16.gif"));
        } else if (clazz.equals(PoliciesFolderNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/policy16.gif"));
        } else if (clazz.equals(ServicesFolderNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/server16.gif"));
        } else if (clazz.equals(ServiceNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/services16.png"));
        } else if (clazz.equals(AdminFolderNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/user16.png"));
        } else if (clazz.equals(GroupFolderNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/group16.png"));
        } else if (clazz.equals(UserFolderNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/user16.png"));
        } else if (clazz.equals(RootNode.class)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/root.gif"));
        }

        return null;
    }


    /**
     * Get the Icon for the EntityHeader passed.
     *
     * @param node   the EntityHeader instance
     * @return ImageIcon for the given node
     */
    public ImageIcon getIcon(EntityHeader node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        ClassLoader cl = node.getClass().getClassLoader();
        return getIcon(node.getType());

    }

    /**
     * Get the Icon for the Class passed.
     *
     * @param type   the entity type enum
     * @return ImageIcon for the given node
     */
    public ImageIcon getIcon(EntityType type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        ClassLoader cl = type.getClass().getClassLoader();
        if (type.equals(EntityType.GROUP)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/group16.png"));
        } else if (type.equals(EntityType.USER)) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/user16.png"));
        }
        return null;
    }

    /**
     * Get the Icon for the EntityHeader passed.
     *
     * @param node   the EntityHeader instance
     * @return ImageIcon for the given node
     */
    public ImageIcon getIcon(WsdlTreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }
        ClassLoader cl = node.getClass().getClassLoader();
        if (node.isMessage()) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/SendMail16.gif"));
        } else if (node.isService()) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Bean16.gif"));
        } else if (node.isPortType()) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/interface.gif"));
        } else if (node.isOperation()) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Forward16.gif"));
        } else if (node.isBindingOperation()) {
            return new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Forward16.gif"));
        }
        return null;

    }

    /**
     * Returns an up button
     * enabled.
     */
    public ImageIcon getUpButton() {
        return upButton;
    }

    /**
     * Returns a down button
     * enabled.
     */
    public ImageIcon getDownButton() {
        return downButton;
    }

    /**
     * load icon images using this instance ClassLoader.
     *
     * @see java.lang.ClassLoader
     */
    private void loadimages() {

        // icons for adding (all) and removing (all)
        iconAdd
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Add16.gif"));
        iconAddAll
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/AddAll16.gif"));
        iconRemove
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Remove16.gif"));
        iconRemoveAll
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/RemoveAll16.gif"));

        defaultEdit
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Edit16.gif"));
        defaultDelete
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Delete16.gif"));
        defaultNew
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/New16.gif"));

        upOneLevel
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/upOneLevel.gif"));

        openFolder
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/FolderOpen16.gif"));

        upButton
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/up-button.gif"));

        downButton
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/down-button.gif"));
    }


    public ImageIcon getIconAdd() {
        return iconAdd;
    }

    public ImageIcon getIconAddAll() {
        return iconAddAll;
    }

    public ImageIcon getIconRemove() {
        return iconRemove;
    }

    public ImageIcon getIconRemoveAll() {
        return iconRemoveAll;
    }

    /** @return the default Edit icon */
    public ImageIcon getDefaultEditIcon() {
        return defaultEdit;
    }

    /** @return the default Delete icon */
    public ImageIcon getDefaultDeleteIcon() {
        return defaultDelete;
    }

    /** @return the default New icon */
    public ImageIcon getDefaultNewIcon() {
        return defaultNew;
    }

    /** @return the 'up one level' icon */
    public ImageIcon getUpOneLevelIcon() {
        return upOneLevel;
    }

    /** @return the 'open folder' icon */
    public ImageIcon getOpenFolderIcon() {
        return openFolder;
    }

    private ImageIcon iconAdd;
    private ImageIcon iconAddAll;
    private ImageIcon iconRemove;
    private ImageIcon iconRemoveAll;
    private ImageIcon upButton;
    private ImageIcon downButton;

    /** the default Edit icon */
    private ImageIcon defaultEdit;
    /** the default Delete icon */
    private ImageIcon defaultDelete;
    /** the default New icon */
    private ImageIcon defaultNew;
    /** the 'up one level' icon */
    private ImageIcon upOneLevel;
    /** the 'action open folder' icon */
    private ImageIcon openFolder;
}
