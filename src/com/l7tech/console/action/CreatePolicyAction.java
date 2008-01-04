/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.security.rbac.AttemptedCreate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;

/**
 * SSM action to create a new {@link Policy}.
 */
public class CreatePolicyAction extends SecureAction {
    public CreatePolicyAction() {
        super(new AttemptedCreate(EntityType.POLICY), Include.class);
    }

    @Override
    public String getName() {
        return "Create Policy";
    }

    @Override
    public String getDescription() {
        return "Create a new Policy";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/xmlObject16.gif";
    }

    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        String xml = WspWriter.getPolicyXml(new AllAssertion( Collections.EMPTY_LIST ));
        // canUpdate == true because this action would be disabled if we couldn't create policies
        final OkCancelDialog<Policy> dlg = PolicyPropertiesPanel.makeDialog(mw, new Policy(PolicyType.INCLUDE_FRAGMENT, null, xml, false), true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (!dlg.wasOKed()) return;

                Policy policy = dlg.getValue();
                long oid;
                try {
                    String xml = WspWriter.getPolicyXml(new AllAssertion(Arrays.asList(new CommentAssertion("Policy Fragment: " + policy.getName()))));
                    policy.setXml( xml );
                    oid = Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                } catch (PolicyAssertionException e) {
                    throw new RuntimeException("Couldn't save Policy", e);
                } catch (SaveException e) {
                    throw new RuntimeException("Couldn't save Policy", e);
                }

                JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                if (tree == null) {
                    log.log(Level.WARNING, "Policy tree unreachable.");
                    return;
                }

                PoliciesFolderNode root = TopComponents.getInstance().getPoliciesFolderNode();
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(new EntityHeader(Long.toString(oid), com.l7tech.objectmodel.EntityType.POLICY, policy.getName(), null));
                model.insertNodeInto(sn, root, root.getInsertPosition(sn));
                tree.setSelectionPath(new TreePath(sn.getPath()));
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        new EditPolicyAction((PolicyEntityNode)sn).invoke();
                    }
                });
            }
        });
    }
}
