package com.l7tech.console.tree;

import com.l7tech.console.action.DeleteServiceAction;
import com.l7tech.console.action.ServicePolicyPropertiesAction;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.Wsdl;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.io.StringReader;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the SSL transport node.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SslTransportNode extends AbstractTreeNode {
    /**
     * construct the <CODE>SslTransportNode</CODE> instance.
     */
    public SslTransportNode(){
        super(null);
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{};
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
     * subclasses override this method
     */
    protected void loadChildren() {}

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "SSL transport";

    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/ssl.gif";
    }
}
