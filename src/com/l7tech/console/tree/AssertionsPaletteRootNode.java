package com.l7tech.console.tree;

import com.l7tech.console.action.NewProviderAction;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The class represents an <code>AbstractTreeNode</code> specialization
 * element that represents the assertions palette root.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AssertionsPaletteRootNode extends AbstractTreeNode {
    /**
     * construct the <CODE>AssertionsPaletteRootNode</CODE> instance
     */
    public AssertionsPaletteRootNode(String title)
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
        String homePath = null;
        homePath = Preferences.getPreferences().getHomePath();
        Enumeration e =
          TreeNodeFactory.
          getTreeNodeEnumeration(
            new EntitiesEnumeration(new ProviderEntitiesCollection(r.getProviderConfigManager())));
        List nodeList = new ArrayList();
        nodeList.addAll(Collections.list(e));
        nodeList.add(new PoliciesFolderNode(homePath));
        nodeList.add(new AuthMethodFolderNode());
        nodeList.add(new TransportLayerSecurityFolderNode());
        nodeList.add(new XmlSecurityFolderNode());
        nodeList.add(new XmlFolderNode());
        nodeList.add(new RoutingFolderNode());

        AbstractTreeNode[] nodes = (AbstractTreeNode[])nodeList.toArray(new AbstractTreeNode[]{});

        children = null;
        for (int i = 0; i < nodes.length; i++) {
            insert(nodes[i], i);
        }
    }



    /**
     * test whether the node can refresh its children. The provider
     * node can always refresh its children
     * @return always true
     */
    public boolean canRefresh() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * <P>
     * By default returns the empty actions arrays.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{
            new NewProviderAction(this),
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
        return "com/l7tech/console/resources/policy16.gif";
    }

    private String label;
}
