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
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

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
    @NotNull private final Option<Folder> folder;
    @NotNull private final Option<AbstractTreeNode> abstractTreeNode;

    public CreatePolicyAction() {
        this( Option.<Folder>none(), Option.<AbstractTreeNode>none() );
    }

    public CreatePolicyAction( @NotNull final Folder folder,
                               @NotNull final AbstractTreeNode abstractTreeNode ) {
        this( Option.some( folder ), Option.<AbstractTreeNode>some( abstractTreeNode ) );
    }

    public CreatePolicyAction( @NotNull final Option<Folder> folder,
                               @NotNull final Option<AbstractTreeNode> abstractTreeNode ) {
        super(new AttemptedCreate(EntityType.POLICY), Include.class);
        this.folder = folder;
        this.abstractTreeNode = abstractTreeNode;
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

                final Policy newPolicy = dlg.getValue();
                Pair<Long,String> oidAndGuid = null;
                try {
                    //if the editor didn't already create some policy content, create a default here
                    if (!(newPolicy.getType() == PolicyType.INTERNAL)) {
                        String xml = WspWriter.getPolicyXml(new AllAssertion(Arrays.<Assertion>asList(new AuditDetailAssertion("Policy Fragment: " + newPolicy.getName()))));
                        newPolicy.setXml( xml );
                    } else if (newPolicy.getXml() == null) {
                        final String defaultPolicyXml =
                                Registry.getDefault().getPolicyAdmin().getDefaultPolicyXml(newPolicy.getType(), newPolicy.getInternalTag());

                        String xml = (defaultPolicyXml != null)? defaultPolicyXml: WspWriter.getPolicyXml(
                                new AllAssertion(Arrays.<Assertion>asList(new AuditDetailAssertion("Internal Policy: " + newPolicy.getName()))));
                        newPolicy.setXml( xml );
                    }
                    newPolicy.setFolder( folder.orSome( ((RootNode) root).getFolder() ) );
                    oidAndGuid = Registry.getDefault().getPolicyAdmin().savePolicy(newPolicy);
                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                } catch ( DuplicateObjectException doe) {
                    String message = "Unable to save the policy '" + newPolicy.getName() + "'.\n";
                    if ( newPolicy.getType() == PolicyType.GLOBAL_FRAGMENT ) {
                        message += "The policy name is already in use or there is an existing\n" +
                                   "Global Policy Fragment with the '"+newPolicy.getInternalTag()+"' tag.";
                    } else if (newPolicy.getType() == PolicyType.INTERNAL && PolicyType.getAuditMessageFilterTags().contains(newPolicy.getInternalTag())){
                        message += "The policy name is already in use or there is an existing\n" +
                                   "Internal Policy with the '"+newPolicy.getInternalTag()+"' tag.";
                    }
                    else {
                        message += "The policy name is already used, please choose a different\n name and try again.";

                    }
                    DialogDisplayer.showMessageDialog(parent, "Duplicate policy", message, null, new Runnable() {
                        @Override
                        public void run() {
                            // callback when dialog dismissed
                            doEdit( parent, newPolicy );
                        }
                     });
                } catch (PolicyAssertionException e) {
                    throw new RuntimeException("Couldn't save Policy", e);
                } catch (SaveException e) {
                    throw new RuntimeException("Couldn't save Policy", e);
                }

                if ( oidAndGuid != null ) {
                    newPolicy.setOid( oidAndGuid.left );
                    newPolicy.setGuid( oidAndGuid.right );

                    final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Policy tree unreachable.");
                        return;
                    }

                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    AbstractTreeNode parent = abstractTreeNode.orSome( root );

                    //Remove any filter before insert
                    TopComponents.getInstance().clearFilter();

                    PolicyHeader ph = new PolicyHeader(newPolicy);
                    final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(ph, null);
                    model.insertNodeInto(sn, parent, parent.getInsertPosition(sn, RootNode.getComparator()));
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
