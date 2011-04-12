/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Arrays;
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

    @Override
    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        // canUpdate == true because this action would be disabled if we couldn't create policies
        final Policy policy = new Policy( PolicyType.INCLUDE_FRAGMENT, null, null, false);
        doEdit( mw, policy );
    }

    private void doEdit( final Frame parent, final Policy policy ) {
        final OkCancelDialog<Policy> dlg = PolicyPropertiesPanel.makeDialog(parent, policy, true);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (!dlg.wasOKed()) return;

                AbstractTreeNode root = TopComponents.getInstance().getPoliciesFolderNode();

                final Policy dlgPolicy = dlg.getValue();
                long oid = -1;
                try {
                    //if the editor didn't already create some policy content, create a default here
                    if (!(dlgPolicy.getType() == PolicyType.INTERNAL)) {
                        String xml = WspWriter.getPolicyXml(new AllAssertion(Arrays.<Assertion>asList(new AuditDetailAssertion("Policy Fragment: " + dlgPolicy.getName()))));
                        dlgPolicy.setXml( xml );
                    } else if (dlgPolicy.getXml() == null) {
                        final String defaultPolicyXml =
                                Registry.getDefault().getPolicyAdmin().getDefaultPolicyXml(dlgPolicy.getType(), dlgPolicy.getInternalTag());

                        String xml = (defaultPolicyXml != null)? defaultPolicyXml: WspWriter.getPolicyXml(
                                new AllAssertion(Arrays.<Assertion>asList(new AuditDetailAssertion("Internal Policy: " + dlgPolicy.getName()))));
                        dlgPolicy.setXml( xml );
                    }
                    dlgPolicy.setFolder(((RootNode)root).getFolder());
                    oid = Registry.getDefault().getPolicyAdmin().savePolicy(dlgPolicy);
                } catch ( DuplicateObjectException doe) {
                    String message = "Unable to save the policy '" + dlgPolicy.getName() + "'.\n";
                    if ( dlgPolicy.getType() == PolicyType.GLOBAL_FRAGMENT ) {
                        message += "The policy name is already in use or there is an existing\n" +
                                   "Global Policy Fragment with the '"+dlgPolicy.getInternalTag()+"' tag.";
                    } else if (dlgPolicy.getType() == PolicyType.INTERNAL && PolicyType.getAuditMessageFilterTags().contains(dlgPolicy.getInternalTag())){
                        message += "The policy name is already in use or there is an existing\n" +
                                   "Internal Policy with the '"+dlgPolicy.getInternalTag()+"' tag.";
                    }
                    else {
                        message += "The policy name is already used, please choose a different\n name and try again.";

                    }
                    DialogDisplayer.showMessageDialog(parent, "Duplicate policy", message, null, new Runnable() {
                        @Override
                        public void run() {
                            // callback when dialog dismissed
                            doEdit( parent, dlgPolicy );
                        }
                     });
                } catch (PolicyAssertionException e) {
                    throw new RuntimeException("Couldn't save Policy", e);
                } catch (SaveException e) {
                    throw new RuntimeException("Couldn't save Policy", e);
                }

                Policy savedPolicy;
                try {
                    savedPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(oid);
                } catch (FindException e) {
                    throw new RuntimeException("Policy saved, but couldn't be retrieved", e);
                }

                if ( oid > 0 ) {
                    final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Policy tree unreachable.");
                        return;
                    }

                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

                    //Remove any filter before insert
                    TopComponents.getInstance().clearFilter();

                    PolicyHeader ph = new PolicyHeader(savedPolicy);
                    final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(ph, null);
                    model.insertNodeInto(sn, root, root.getInsertPosition(sn, RootNode.getComparator()));
                    RootNode rootNode = (RootNode) model.getRoot();
                    rootNode.addEntity(ph.getOid(), sn);

                    tree.setSelectionPath(new TreePath(sn.getPath()));

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            new EditPolicyAction((PolicyEntityNode)sn).invoke();

                            //reset filter to display all
                            ((ServicesAndPoliciesTree) tree).filterTreeToDefault();
                        }
                    });
                }
            }
        });
    }
}
