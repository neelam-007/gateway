package com.l7tech.console.tree;

import org.apache.log4j.Category;

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

  /**
   * creates a tree node menu for the particular Object and listener.
   */
  private TreeNodeMenu(Object dirObject, ActionListener listener) {
    this.dirObject = dirObject;
    this.listener = listener;
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
  public static JPopupMenu forNode(DirectoryTreeNode node, ActionListener listener) {

    Object object = node.getUserObject();
    
    JPopupMenu retMenu = null;

    if (object instanceof ProvidersFolderTreeNode) {
      retMenu = forRealmFolder((ProvidersFolderTreeNode)object, listener);
    } else if (object instanceof ProviderTreeNode) {
      retMenu = forProvider((ProviderTreeNode)object, listener);
    } else if (object instanceof AdminFolderTreeNode) {
      retMenu = forAdminFolder((AdminFolderTreeNode)object, listener);
    } else if (object instanceof UserFolderTreeNode) {
      retMenu = forUserFolder((UserFolderTreeNode)object, listener);
    } else if (object instanceof GroupFolderTreeNode) {
         retMenu = forGroupFolder((GroupFolderTreeNode)object, listener);
    }
    
    // if there is no menu yet make one..
    if (retMenu == null) {
      retMenu = new TreeNodeMenu(node, listener);
    }
    
    // have properties?
    if (TreeNodeAction.hasProperties(node)) {
      JMenuItem item = new JMenuItem(PROPERTIES);
      item.addActionListener(listener);
      retMenu.insert(item,0);
    }
    
    // browseable?
    if (TreeNodeAction.isBrowseable(node)) {
      JMenuItem item = new JMenuItem(BROWSE);
      item.addActionListener(listener);
      retMenu.insert(item,0);
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
  private static JPopupMenu forRealmFolder(ProvidersFolderTreeNode realm, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(realm, listener);
    
    JMenu menu = new JMenu(NEW);
    menu.add(NEW_PROVIDER).addActionListener(listener);
    treeMenu.add(menu);
    
    return treeMenu;
  }
  
  /**
   * create the popup menu <CODE>JpopUpMenu</CODE> for the Realm
   * node passed.
   *
   * @param provider   ProviderTreeNode node to create the menu for
   * @param listener the <CODE>ActionListener</CODE> where the
   *                 events will be sent.
   * @return JpopUpMenu for the node
   */
  private static JPopupMenu forProvider(ProviderTreeNode provider, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(provider, listener);
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
  private static JPopupMenu forAdminFolder(AdminFolderTreeNode admin, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(admin, listener);
    
    JMenu menu = new JMenu(NEW);
    menu.add(NEW_ADMINISTRATOR).addActionListener(listener);
    treeMenu.add(menu);
    
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
  private static JPopupMenu forAdmin(UserTreeNode entry, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(entry, listener);
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
  private static JPopupMenu forUserFolder(UserFolderTreeNode user, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(user, listener);
    
    JMenu menu = new JMenu(NEW);
    menu.add(NEW_USER).addActionListener(listener);
    treeMenu.add(menu);
    
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
  private static JPopupMenu forGroupFolder(GroupFolderTreeNode group, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(group, listener);

    JMenu menu = new JMenu(NEW);
    menu.add(NEW_GROUP).addActionListener(listener);
    treeMenu.add(menu);

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
  private static JPopupMenu forUser(UserTreeNode entry, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(entry, listener);
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
  private static JPopupMenu forGroup(GroupTreeNode entry, ActionListener listener) {
    TreeNodeMenu treeMenu = new TreeNodeMenu(entry, listener);
    treeMenu.add(DELETE).addActionListener(listener);
    return treeMenu;
  }
  
  private Object dirObject;
  private ActionListener listener;

  public static final String NEW_PROVIDER = "Provider";
  public static final String NEW_USER = "User";
  public static final String NEW_GROUP = "Group";
  public static final String NEW_ADMINISTRATOR = "Admin";
  public static final String NEW = "New";
  public static final String FIND = "Find";
  public static final String DELETE = "Delete";
  public static final String PROPERTIES = "Properties";
  public static final String BROWSE = "Browse";
}

