package com.l7tech.console.tree;

import com.l7tech.service.ServiceManager;

import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class ServicesFolderNode implements BasicTreeNode {
    private ServiceManager serviceManager;
    private String title;


    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given servcie manager with the name.
     */
    public ServicesFolderNode(ServiceManager sm, String name) {
        serviceManager = sm;
        title = name;
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
        Enumeration e = new EntitiesEnumeration(new ServiceEntitiesCollection(serviceManager));
        return TreeNodeFactory.getTreeNodeEnumeration(e);
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Returns the node name.
     * Gui nodes have name to facilitate handling in
     * components such as JTree.
     *
     * @return the name as a String
     */
    public String getName() {
        return title;
    }

}
