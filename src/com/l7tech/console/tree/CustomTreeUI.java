package com.l7tech.console.tree;


import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.incors.plaf.kunststoff.KunststoffTreeUI;
import com.sun.java.swing.plaf.motif.MotifLookAndFeel;
import com.sun.java.swing.plaf.motif.MotifTreeUI;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import com.sun.java.swing.plaf.windows.WindowsTreeUI;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;


/**
 * The class contian utilities to create custom tree UI for a
 * giventhe look and feel.
 *
 * The tree UI is overriden to deal with folders that contain
 * children but are not expandabple.
 *
 * Note that there are dependencies to the com.sun.java.*
 * packages.
 */
public final class CustomTreeUI {
  /**
   * private constructor, this class cannot be instantiated
   */
  private CustomTreeUI() {
  }

  public static TreeUI getTreeUI(String lookAndFeel) {
    if (WindowsLookAndFeel.class.getName().equals(lookAndFeel)) {
      return new CustomWindowsTreeUI();
    } else if (MotifLookAndFeel.class.getName().equals(lookAndFeel)) {
      return new CustomMotifTreeUI();
    } else if (MetalLookAndFeel.class.getName().equals(lookAndFeel)) {
      return new CustomMetalTreeUI(); // default 
    } else if (KunststoffLookAndFeel.class.getName().equals(lookAndFeel)) {
      return new CustomKunstStoffTreeUI(null);
    }
    throw new 
      IllegalArgumentException("Don't know how to handle look and feel "+lookAndFeel);
  }

  public static TreeUI getTreeUI() {
    return getTreeUI(UIManager.getLookAndFeel().getClass().getName());
  }

  /**
   * The Metal treeUI override
   */
  private static class CustomMetalTreeUI 
    extends MetalTreeUI {
    /**
     * Paints the expand (toggle) part of a row. The reciever should
     * NOT modify <code>clipBounds</code>, or <code>insets</code>.
     */
    protected void paintExpandControl(Graphics g,
                                      Rectangle clipBounds, Insets insets,
                                      Rectangle bounds, TreePath path,
                                      int row, boolean isExpanded,
                                      boolean hasBeenExpanded,
                                      boolean isLeaf) {

      Object value = path.getLastPathComponent();               
      Icon mExpanded = this.getExpandedIcon();
      Icon mCollapsed = this.getCollapsedIcon();

      if (isNonExpandableFolder(value)) {
        this.setExpandedIcon(null);
        this.setCollapsedIcon(null);
      }

      super.paintExpandControl(g, clipBounds, insets, bounds, path, 
                               row, isExpanded, hasBeenExpanded, isLeaf);

      this.setExpandedIcon(mExpanded);
      this.setCollapsedIcon(mCollapsed);
    }
  }

  /**
   * The Motif treeUI override
   */
  private static final
    class CustomMotifTreeUI extends MotifTreeUI {
    /**
     * Paints the expand (toggle) part of a row. The reciever should
     * NOT modify <code>clipBounds</code>, or <code>insets</code>.
     */
    protected void paintExpandControl(Graphics g,
                                      Rectangle clipBounds, Insets insets,
                                      Rectangle bounds, TreePath path,
                                      int row, boolean isExpanded,
                                      boolean hasBeenExpanded,
                                      boolean isLeaf) {

      Object value = path.getLastPathComponent();               
      Icon mExpanded = this.getExpandedIcon();
      Icon mCollapsed = this.getCollapsedIcon();

      if (isNonExpandableFolder(value)) {
        this.setExpandedIcon(null);
        this.setCollapsedIcon(null);
      }

      super.paintExpandControl(g, clipBounds, insets, bounds, path, 
                               row, isExpanded, hasBeenExpanded, isLeaf);

      this.setExpandedIcon(mExpanded);
      this.setCollapsedIcon(mCollapsed);
    }
  }

  /**
   * The Windows treeUI override
   */
  private static class CustomWindowsTreeUI 
    extends WindowsTreeUI {
    /**
     * Paints the expand (toggle) part of a row. The reciever should
     * NOT modify <code>clipBounds</code>, or <code>insets</code>.
     */
    protected void paintExpandControl(Graphics g,
                                      Rectangle clipBounds, Insets insets,
                                      Rectangle bounds, TreePath path,
                                      int row, boolean isExpanded,
                                      boolean hasBeenExpanded,
                                      boolean isLeaf) {

      Object value = path.getLastPathComponent();               
      Icon mExpanded = this.getExpandedIcon();
      Icon mCollapsed = this.getCollapsedIcon();

      if (isNonExpandableFolder(value)) {
        this.setExpandedIcon(null);
        this.setCollapsedIcon(null);
      }

      super.paintExpandControl(g, clipBounds, insets, bounds, path, 
                               row, isExpanded, hasBeenExpanded, isLeaf);

      this.setExpandedIcon(mExpanded);
      this.setCollapsedIcon(mCollapsed);
    }
  }


  /**
   * The kunststoff treeUI override
   */
  private static class CustomKunstStoffTreeUI 
    extends KunststoffTreeUI {

    CustomKunstStoffTreeUI(JComponent comp) {
      super(comp);
    }
    /**
     * Paints the expand (toggle) part of a row. The reciever should
     * NOT modify <code>clipBounds</code>, or <code>insets</code>.
     */
    protected void paintExpandControl(Graphics g,
                                      Rectangle clipBounds, Insets insets,
                                      Rectangle bounds, TreePath path,
                                      int row, boolean isExpanded,
                                      boolean hasBeenExpanded,
                                      boolean isLeaf) {

      Object value = path.getLastPathComponent();               
      Icon mExpanded = this.getExpandedIcon();
      Icon mCollapsed = this.getCollapsedIcon();

      if (CustomTreeUI.isNonExpandableFolder(value)) {
        this.setExpandedIcon(null);
        this.setCollapsedIcon(null);
      }

      super.paintExpandControl(g, clipBounds, insets, bounds, path, 
                               row, isExpanded, hasBeenExpanded, isLeaf);

      this.setExpandedIcon(mExpanded);
      this.setCollapsedIcon(mCollapsed);
    }
  }

  /**
   * is this non expandable folder?
   *
   * @param object the object to check
   * @return true if object is any of the folders, false otherwise
   */
  static boolean isNonExpandableFolder(Object object) {
    Object uObject = ((DefaultMutableTreeNode)object).getUserObject();

    Class clazz = uObject.getClass();
    return
      clazz.equals(AdminFolderNode.class) ||
      clazz.equals(GroupFolderNode.class) ||
      clazz.equals(UserFolderNode.class);
  }
}
