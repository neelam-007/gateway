package com.l7tech.console.tree.identity;

import com.l7tech.console.action.NewProviderAction;
import com.l7tech.console.action.NewSamlProviderAction;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntitiesEnumeration;
import com.l7tech.console.tree.RefreshTreeNodeAction;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The class represents an <code>AbstractTreeNode</code> specialization
 * element that represents the identity providers and SAML providers
 * elements root.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class IdentitiesRootNode extends AbstractTreeNode {
    /**
     * construct the <CODE>AssertionsPaletteRootNode</CODE> instance
     */
    public IdentitiesRootNode(String title)
      throws IllegalArgumentException {
        super(null);
        if (title == null)
            throw new IllegalArgumentException();
        label = title;
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
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        Registry r = Registry.getDefault();
        List nodeList = new ArrayList();
        Enumeration e =
          TreeNodeFactory.
          getTreeNodeEnumeration(new EntitiesEnumeration(new ProviderEntitiesCollection(r.getProviderConfigManager())));
        nodeList.addAll(Collections.list(e));

        AbstractTreeNode[] nodes = (AbstractTreeNode[])nodeList.toArray(new AbstractTreeNode[]{});

        children = null;
        for (int i = 0; i < nodes.length; i++) {
            insert(nodes[i], i);
        }
    }


    /**
     * test whether the node can refresh its children. The provider
     * node can always refresh its children
     *
     * @return always true
     */
    public boolean canRefresh() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * <p/>
     * <P>
     * By default returns the empty actions arrays.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{
            new NewProviderAction(this),
       //     new NewSamlProviderAction(this),
            new RefreshTreeNodeAction(this)};
    }

    /**
     * @return the root name
     */
    public String getName() {
        return label;
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/identity.png";
    }

    private String label;
}
