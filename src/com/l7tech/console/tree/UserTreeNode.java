/*
 * $Header$
 */
package com.l7tech.console.tree;

import com.l7tech.adminservicestub.identities.IdentityWSService;
import com.l7tech.adminservicestub.identities.IdentityWSServiceLocator;
import com.l7tech.adminservicestub.identities.IdentityWS;
import com.l7tech.adminservicestub.ListResultEntry;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with agents for a company.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 */
public class UserTreeNode implements BasicTreeNode {
    /**
     * construct the <CODE>UserFolderTreeNode</CODE> instance for
     * a given entry.
     *
     * @param entry  the Entry instance, must be Company
     * @exception IllegalArgumentException
     *                   thrown if the Entry instance is not a Comapny
     */
    public UserTreeNode(ListResultEntry entry)
      throws IllegalArgumentException {
        this.entry = entry;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
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

    private ListResultEntry entry;
    private String fqName;
}
