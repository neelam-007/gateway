package com.l7tech.console.tree;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * the <code>TreeNodeMenu</code> creates a menu for a TreeNode.
 * The <CODE>ActionListener</CODE> is passed to every
 * <code>JMenuItem</CODE>, that is every JMenuItem uses the same
 * listener.
 * The different events are identified by action commands in
 * <CODE>ActionEvent</CODE> class.
 * It should take the permissions in the process of determining
 * the available items.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 * @see java.awt.event.ActionEvent#getActionCommand()
 */
public class TreeNodeMenu extends JPopupMenu {
    private Object object;


    /**
     * creates a tree node menu for the particular Object and listener.
     */
    private TreeNodeMenu(Object o) {
        this.object = o;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for node passed.
     *
     * @param node     the node to create the menu for
     * @param listener the <CODE>ActionListener</CODE> where the events will be
     *                 sent.
     * @return JpopUpMenu for the node, or <CODE>null</code> if the menu
     *         cannot be created for the node.
     */
    public static JPopupMenu forNode(EntityTreeNode node, ActionListener listener) {

        Object object = node.getUserObject();

        JPopupMenu retMenu = null;

        if (object instanceof ProvidersFolderNode) {
            retMenu = forProvidersFolder((ProvidersFolderNode)object, listener);
        } else if (object instanceof ProviderNode) {
            retMenu = forProvider((ProviderNode)object, listener);
        } else if (object instanceof AdminFolderNode) {
            retMenu = forAdminFolder((AdminFolderNode)object, listener);
        } else if (object instanceof UserFolderNode) {
            retMenu = forUserFolder((UserFolderNode)object, listener);
        } else if (object instanceof UserNode) {
                 retMenu = forUser((UserNode)object, listener);
        } else if (object instanceof GroupFolderNode) {
            retMenu = forGroupFolder((GroupFolderNode)object, listener);
        } else if (object instanceof ServicesFolderNode) {
            retMenu = forServicesFolder((ServicesFolderNode)object, listener);
        }

        // if there is no menu yet make one..
        if (retMenu == null) {
            retMenu = new TreeNodeMenu(node);
        }

        // have properties?
        if (TreeNodeAction.hasProperties(node)) {
            JMenuItem item = new JMenuItem(PROPERTIES);
            item.addActionListener(listener);
            retMenu.insert(item, 0);
        }

        return retMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the Realm
     * Folder node passed.
     *
     * @param realm    The realm folder node
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forProvidersFolder(ProvidersFolderNode realm, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(realm);
        treeMenu.add(NEW_PROVIDER).addActionListener(listener);

        return treeMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the Realm
     * node passed.
     *
     * @param provider   ProviderNode node to create the menu for
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forProvider(ProviderNode provider, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(provider);
        treeMenu.add(DELETE).addActionListener(listener);

        return treeMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the Admin folder node passed.
     *
     * @param admin    The admin folder node
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forAdminFolder(AdminFolderNode admin, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(admin);
        treeMenu.add(NEW_ADMINISTRATOR).addActionListener(listener);

        return treeMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the Admin
     * node passed.
     *
     * @param entry    The entry to add the menu for
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forAdmin(UserNode entry, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(entry);
        treeMenu.add(DELETE).addActionListener(listener);

        return treeMenu;
    }


    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the user Folder
     * node passed.
     *
     * @param user    the user folder node
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forUserFolder(UserFolderNode user, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(user);
        treeMenu.add(NEW_USER).addActionListener(listener);

        return treeMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the Group Folder
     * node passed.
     *
     * @param group    the group folder node
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forGroupFolder(GroupFolderNode group, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(group);
        treeMenu.add(NEW_GROUP).addActionListener(listener);

        return treeMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the user
     * node passed.
     *
     * @param entry    The entry to add the menu for
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forUser(UserNode entry, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(entry);
        treeMenu.add(DELETE).addActionListener(listener);

        return treeMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the VPN
     * node passed.
     *
     * @param entry    The entry to add the menu for
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forGroup(GroupNode entry, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(entry);
        treeMenu.add(DELETE).addActionListener(listener);
        return treeMenu;
    }

    /**
     * create the popup menu <CODE>JpopUpMenu</CODE> for the Realm
     * Folder node passed.
     *
     * @param node    The services folder folder node
     * @param listener the <CODE>ActionListener</CODE> where the
     *                 events will be sent.
     * @return JpopUpMenu for the node
     */
    private static JPopupMenu forServicesFolder(ServicesFolderNode node, ActionListener listener) {
        TreeNodeMenu treeMenu = new TreeNodeMenu(node);
        treeMenu.add(NEW_SERVICE).addActionListener(listener);
        return treeMenu;
    }


    public static final String NEW_SERVICE = "New Service";
    public static final String NEW_PROVIDER = "New Provider";
    public static final String NEW_USER = "New User";
    public static final String NEW_GROUP = "New Group";
    public static final String NEW_ADMINISTRATOR = "New Administrator";
    public static final String NEW = "New";
    public static final String FIND = "Find";
    public static final String DELETE = "Delete";
    public static final String PROPERTIES = "Properties";
    public static final String BROWSE = "Browse";
}

