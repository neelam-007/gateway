package com.l7tech.console;

import com.l7tech.console.tree.BasicTreeNode;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;

/**
 * A class that contains icon resources
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class IconRepository {
  /* this class classloader */
  private final ClassLoader cl = getClass().getClassLoader();

  /**
   * default constructor
   */
  protected IconRepository() {
  }

  /**
   * Get the Icon for the BasicTreeNode passed.
   *
   * @param node   the BasicTreeNode instance
   * @return ImageIcon for the given node
   */
  public static ImageIcon getIcon(BasicTreeNode node) {
    if (node == null) {
      throw new NullPointerException("node");
    }
     return null;
  }


    /**
     * Get the Icon for the ListResultEntry passed.
     *
     * @param node   the BasicTreeNode instance
     * @return ImageIcon for the given node
     */
    public static ImageIcon getIcon(EntityHeader node) {
      if (node == null) {
        throw new NullPointerException("node");
      }
       return null;
    }


  /**
   * Get the 32X32 Icon for the Entry passed.
   *
   * @param entry   the Entry instance
   * @return ImageIcon for the given directory object
   */
  public static ImageIcon get32Icon(BasicTreeNode entry) {
    return null;
  }


  /**
   * Returns an up button
   * enabled.
   */
  public static ImageIcon getUpButton() {
    return upButton;
  }

  /**
   * Returns a down button
   * enabled.
   */
  public static ImageIcon getDownButton() {
    return downButton;
  }

  /**
   * load icon images using this instance ClassLoader.
   *
   * @see java.lang.ClassLoader
   */
  public void loadCommonImages() {

    // icons for adding (all) and removing (all)
    iconAdd
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/Add16.gif"));
    iconAddAll
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/AddAll16.gif"));
    iconRemove
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/Remove16.gif"));
    iconRemoveAll
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/RemoveAll16.gif"));

    defaultEdit
      = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/Edit16.gif"));
    defaultDelete
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/Delete16.gif"));
    defaultNew
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/New16.gif"));

    upOneLevel
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/upOneLevel.gif"));

    openFolder
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/FolderOpen16.gif"));
    
    upButton
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/up-button.gif"));
    
    downButton
    = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/down-button.gif"));

  }


  public static ImageIcon getIconAdd() {
    return iconAdd;
  }

  public static ImageIcon getIconAddAll() {
    return iconAddAll;
  }

  public static ImageIcon getIconRemove() {
    return iconRemove;
  }

  public static ImageIcon getIconRemoveAll() {
    return iconRemoveAll;
  }

  /** @return the default Edit icon */
  public static ImageIcon getDefaultEditIcon() {
    return defaultEdit;
  }

  /** @return the default Delete icon */
  public static ImageIcon getDefaultDeleteIcon() {
    return defaultDelete;
  }

  /** @return the default New icon */
  public static ImageIcon getDefaultNewIcon() {
    return defaultNew;
  }

  /** @return the 'up one level' icon */
  public static ImageIcon getUpOneLevelIcon() {
    return upOneLevel;
  }

  /** @return the 'open folder' icon */
  public static ImageIcon getOpenFolderIcon() {
    return openFolder;
  }
  private static ImageIcon iconAdd;
  private static ImageIcon iconAddAll;
  private static ImageIcon iconRemove;
  private static ImageIcon iconRemoveAll;
  private static ImageIcon upButton;
  private static ImageIcon downButton;

  /** the default Edit icon */
  private static ImageIcon defaultEdit;
  /** the default Delete icon */
  private static ImageIcon defaultDelete;
  /** the default New icon */
  private static ImageIcon defaultNew;
  /** the 'up one level' icon */
  private static ImageIcon upOneLevel;
  /** the 'action open folder' icon */
  private static ImageIcon openFolder;
}
