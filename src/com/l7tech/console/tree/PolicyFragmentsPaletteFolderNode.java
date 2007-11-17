/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.common.policy.PolicyType;
import com.l7tech.console.tree.policy.IncludeAssertionPaletteNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.Vector;

/**
 * @author alex
 */
public class PolicyFragmentsPaletteFolderNode extends AbstractPaletteFolderNode {
    protected PolicyFragmentsPaletteFolderNode() {
        super("Policy Fragments", "policyFragments");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTree policiesTree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                final DefaultTreeModel policiesTreeModel = (DefaultTreeModel) policiesTree.getModel();
                policiesTreeModel.addTreeModelListener(new TreeModelListener() {
                    public void treeNodesChanged(TreeModelEvent e) { reloadChildren(); }
                    public void treeNodesInserted(TreeModelEvent e) { reloadChildren(); }
                    public void treeNodesRemoved(TreeModelEvent e) { reloadChildren(); }
                    public void treeStructureChanged(TreeModelEvent e) { reloadChildren(); }
                });
            }
        });
    }

    @Override
    public void reloadChildren() {
        super.reloadChildren();
        JTree paletteTree = (JTree) TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        final DefaultTreeModel paletteModel = (DefaultTreeModel) paletteTree.getModel();
        paletteModel.nodeStructureChanged(this);
    }

    @Override
    protected boolean isHiddenIfNoChildren() {
        return false;
    }

    @Override
    protected void loadChildren() {
        try {
            Vector<IncludeAssertionPaletteNode> kids = new Vector<IncludeAssertionPaletteNode>();
            Collection<EntityHeader> headers = Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType(PolicyType.INCLUDE_FRAGMENT);
            for (EntityHeader header : headers) {
                kids.add(new IncludeAssertionPaletteNode(header));
            }
            children = kids;
        } catch (FindException e) {
            throw new RuntimeException("Couldn't load policy fragments", e); // Can't happen
        }
    }
}
