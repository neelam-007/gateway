/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;
import com.l7tech.console.tree.policy.IncludeAssertionPaletteNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.logging.Level;

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
                if (policiesTreeModel != null) policiesTreeModel.addTreeModelListener(new TreeModelListener() {
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
            List<PolicyHeader> headers = new ArrayList<PolicyHeader>(Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType( PolicyType.INCLUDE_FRAGMENT));
            Resolver<PolicyHeader, String> resolver = new Resolver<PolicyHeader, String>(){
                public String resolve( PolicyHeader key ) {
                    return key.getName().toLowerCase();
                }
            };
            //noinspection unchecked
            Comparator<PolicyHeader> comp = new ResolvingComparator(resolver, false);
            Collections.sort( headers, comp );
            for ( PolicyHeader header : headers) {
                kids.add(new IncludeAssertionPaletteNode(header));
            }
            children = kids;
        } catch (LicenseRuntimeException e) {
            logger.info("Can't load policies at this time");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to load Policy Fragments", e);
        }
    }
}
