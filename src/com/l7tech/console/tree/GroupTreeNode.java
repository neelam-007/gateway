package com.l7tech.console.tree;
import com.l7tech.objectmodel.EntityHeader;

import java.util.Collections;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with agents for a company.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 */
public class GroupTreeNode implements BasicTreeNode {
    /**
     * construct the <CODE>UserFolderTreeNode</CODE> instance for
     * a given entry.
     *
     * @param entry  the Entry instance, must be Company
     * @exception IllegalArgumentException
     *                   thrown if the Entry instance is not a Comapny
     */
    public GroupTreeNode(EntityHeader entry)
      throws IllegalArgumentException {
        this.entry = entry;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns the children of the reciever as an Enumeration.
     *
     * @return the Enumeration of the child nodes.
     * @exception Exception thrown when an erro is encountered when
     *                      retrieving child nodes.
     */
    public Enumeration children() throws Exception {
      return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Returns the node FQ name.
     * Gui nodes have FQ name to facilitate handling in
     * hierarchical gui components such as JTree.
     *
     * @return the FQ name as a String
     */
    public String getFqName() {
        if (fqName == null) {
            if (!"".equals(entry.getName())) {
                fqName = getLabel() + "." + entry.getName();
            } else {
                fqName = getLabel();
            }
        }
        return fqName;
    }

    /**
     * Returns the label
     */
    public String getLabel() {
        return entry.getName();
    }

    private EntityHeader entry;
    private String fqName;
}
