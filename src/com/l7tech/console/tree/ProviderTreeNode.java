package com.l7tech.console.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.l7tech.objectmodel.EntityHeader;

/**
 * The class represents an tree node gui node element that
 * corresponds to the Provider entity.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 */
public class ProviderTreeNode implements BasicTreeNode {
  /**
   * construct the <CODE>ProviderTreeNode</CODE> instance for
   * a given entry.
   * The parameter entry must be Realm, otherwise the runtime
   * IllegalArgumentException exception is thrown.
   * 
   * @param entry  the Entry instance, must be Realm
   * @exception IllegalArgumentException
   *                   thrown if the Entry instance is not a Realm
   */
  public ProviderTreeNode(EntityHeader entry) {
      if (entry == null) {
          throw new IllegalArgumentException("id == null");
      }
      this.entry = entry;
  }

    public boolean isLeaf() {
        return false;
    }

    /**
   * Returns the children of the reciever as an Enumeration.
   * 
   * @return the Enumeration of the child nodes.
   * @exception Exception thrown when an erro is encountered when
   *                      retrieving child nodes.
   */
  public Enumeration children() throws Exception {
    List list = 
      Arrays.asList(
      new BasicTreeNode[] {
      new AdminFolderTreeNode(entry),
      new UserFolderTreeNode(entry),
      new GroupFolderTreeNode(entry)
    });
    return Collections.enumeration(list);
  }

    public boolean getAllowsChildren() {
        return true;
    }

    public String getLabel() {
        return entry.getName();
    }

    public String getFqName() {
        return getLabel();
    }

    private EntityHeader entry = null;
}
