/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.policy;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.composite.ClientCompositeAssertion;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Datamodel for displaying the policy tree in the Bridge.
 * @author mike
 * @version 1.0
 */
public class PolicyTreeModel extends DefaultTreeModel {
    /**
     * Creates a new instance of PolicyTreeModel with root set
     * to the node represnting the root assertion.
     *
     * @param root
     */
    public PolicyTreeModel(ClientAssertion root) {
        super(make(root));
    }

    private static DefaultMutableTreeNode make(ClientAssertion root) {
        if (root instanceof ClientCompositeAssertion) {
            ClientCompositeAssertion comp = (ClientCompositeAssertion) root;
            DefaultMutableTreeNode container = new DefaultMutableTreeNode(root, true);
            ClientAssertion[] kids = comp.getChildren();
            for (int i = 0; i < kids.length; i++) {
                ClientAssertion kid = kids[i];
                container.add(make(kid));
            }
            return container;
        } else {
            return makeLeaf(root);
        }
    }

    private static DefaultMutableTreeNode makeLeaf(ClientAssertion root) {
        return new DefaultMutableTreeNode(root, false);
    }
}
