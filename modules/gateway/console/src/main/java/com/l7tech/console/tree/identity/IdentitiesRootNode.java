package com.l7tech.console.tree.identity;

import com.l7tech.console.action.*;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * The class represents an <code>AbstractTreeNode</code> specialization
 * element that represents the identity providers and SAML providers
 * elements root.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class IdentitiesRootNode extends AbstractTreeNode {
    static final Logger log = Logger.getLogger(IdentitiesRootNode.class.getName());

    /**
     * construct the <CODE>AssertionsPaletteRootNode</CODE> instance
     */
    public IdentitiesRootNode(String title)
      throws IllegalArgumentException {
        super(null, EntityHeaderNode.IGNORE_CASE_NAME_COMPARATOR);
        if (title == null)
            throw new IllegalArgumentException();
        label = title;
    }

    /**
     * Returns true if the receiver is a leaf.
     * 
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * subclasses override this method
     */
    @SuppressWarnings({"unchecked"})
    @Override
    protected void doLoadChildren() {
        IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
        List nodeList = new ArrayList();
        Enumeration e = TreeNodeFactory.
          getTreeNodeEnumeration(new EntitiesEnumeration(new ProviderEntitiesCollection(admin)));
        nodeList.addAll(Collections.list(e));

        AbstractTreeNode[] nodes = (AbstractTreeNode[])nodeList.toArray(new AbstractTreeNode[nodeList.size()]);

        children = null;
        int i = 0;
        for (; i < nodes.length; i++) {
            insert(nodes[i], getInsertPosition(nodes[i]));
        }
    }


    /**
     * test whether the node can refresh its children. The provider
     * node can always refresh its children
     *
     * @return always true
     */
    @Override
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
    @Override
    public Action[] getActions() {
        return new Action[]{
            new NewLdapProviderAction(this),
            new NewBindOnlyLdapProviderAction(this),
            new NewFederatedIdentityProviderAction(this),
            new NewPolicyBackedIdentityProviderAction(this),
            new RevokeCertificatesAction(),
            new RefreshTreeNodeAction(this)};
    }

    /**
     * @return the root name
     */
    @Override
    public String getName() {
        return label;
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/identity.png";
    }

    private String label;
}
