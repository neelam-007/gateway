package com.l7tech.console.tree;

import com.l7tech.console.action.DeletePolicyTemplateAction;

import javax.swing.*;
import java.io.File;


/**
 * The class represents a node element in the TreeModel.
 * It represents the policy template.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyTemplateNode extends AbstractTreeNode {

    /**
     * construct the <CODE>PolicyTemplateNode</CODE> instance for
     * a given file namer.
     *
     * @param file  the file represented by this policy template
     * @exception IllegalArgumentException
     *                   thrown if file name is <b>null</b>
     */
    public PolicyTemplateNode(File file)
      throws IllegalArgumentException {
        super(file);
        if (file == null)
            throw new IllegalArgumentException();
    }


    /**
     * subclasses override this method
     */
    protected void loadChildren() {
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
            return new Action[]{
            //    new ServicePolicyPropertiesAction(this),
                // new EditServiceNameAction(this),
                new DeletePolicyTemplateAction(this)};
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
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
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return ((File)getUserObject()).getName();
    }

    /**
     * @return the tmplate file associated with this node
     */
    public File getFile() {
        return (File)getUserObject();
    }


    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }
}
