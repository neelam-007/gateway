package com.l7tech.console.table;

import com.l7tech.console.tree.DirectoryTreeNode;
import com.l7tech.console.tree.ProviderTreeNode;
import com.l7tech.console.tree.ProvidersFolderTreeNode;
import org.apache.log4j.Category;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * the <code>TableRowMenu</code> creates a menu for a TableRow.
 * The <CODE>ActionListener</CODE> is passed to every
 * <code>JMenuItem</CODE>, that is every JMenuItem uses the same
 * listener.
 * The different events are identified by action commands in
 * <CODE>ActionEvent</CODE> class.
 * It should take the permissions in the process of determining
 * the available items.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class TableRowMenu extends JPopupMenu {
  private static final Category log = Category.getInstance(TableRowMenu.class.getName());

  /**
   * creates a tree node that points to the particular Object.
   */
  private TableRowMenu(Object dirObject, ActionListener listener) {
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
      retMenu = forRealm((ProviderTreeNode)object, listener);
    }
    
    // if there is no menu yet make one..
    if (retMenu == null) {
      retMenu = new TableRowMenu(node, listener);
    }
    
    // have properties?
    if (TableRowAction.hasProperties(node)) {
      JMenuItem item = new JMenuItem(PROPERTIES);
      item.addActionListener(listener);
      retMenu.insert(item,0);
    }
    
    // browseable?
    if (TableRowAction.isBrowseable(node)) {
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
    TableRowMenu tableMenu = new TableRowMenu(realm, listener);
    
    JMenu menu = new JMenu(NEW);
    menu.add(NEW_PROVIDER).addActionListener(listener);
    tableMenu.add(menu);
    
    return tableMenu;
  }
  
  /**
   * create the popup menu <CODE>JpopUpMenu</CODE> for the Realm
   * node passed.
   *
   * @param realm   ProviderTreeNode node to create the menu for
   * @param listener the <CODE>ActionListener</CODE> where the
   *                 events will be sent.
   * @return JpopUpMenu for the node
   */
  private static JPopupMenu forRealm(ProviderTreeNode realm, ActionListener listener) {
    TableRowMenu tableMenu = new TableRowMenu(realm, listener);
    tableMenu.add(DELETE).addActionListener(listener);
    
    return tableMenu;
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


