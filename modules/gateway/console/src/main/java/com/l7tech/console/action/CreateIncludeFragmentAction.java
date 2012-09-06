package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Creates an include policy fragment from selected nodes and inserts an include assertion into policy.
 */
public class CreateIncludeFragmentAction extends NodeAction {
    private static final int MAX_NAME_LENGTH = 255;
    private final AssertionTreeNode assertionNode;
    private final CompositeAssertionTreeNode parentNode;

    public CreateIncludeFragmentAction(final AssertionTreeNode assertionNode) {
        super(assertionNode, "Create Included Fragment", "Create Include Fragment", "com/l7tech/console/resources/folder.gif");
        this.assertionNode = assertionNode;
        final TreeNode parent = assertionNode.getParent();
        if (parent instanceof CompositeAssertionTreeNode) {
            this.parentNode = (CompositeAssertionTreeNode) parent;
        } else {
            throw new IllegalArgumentException("Parent node must be a CompositeAssertionTreeNode.");
        }
    }

    @Override
    public boolean supportMultipleSelection() {
        return true;
    }

    /**
     * Overridden so we can check multiple AttemptOperations.
     */
    @Override
    public final boolean isAuthorized() {
        boolean authorized = false;
        try {
            if (assertionNode.getService() != null) {
                // node belongs to a published service
                authorized = canAttemptOperation(new AttemptedUpdate(EntityType.SERVICE, assertionNode.getService()))
                        && canAttemptOperation(new AttemptedCreate(EntityType.POLICY));
            } else if (assertionNode.getPolicy() != null) {
                // node belongs to a policy fragment
                authorized = canAttemptOperation(new AttemptedUpdate(EntityType.POLICY, assertionNode.getPolicy()))
                        && canAttemptOperation(new AttemptedCreate(EntityType.POLICY));
            }
        } catch (final FindException e) {
            log.log(Level.WARNING, "Unable to determine authorization: " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
        return authorized;
    }

    @Override
    protected void performAction() {
        final JTree policyTree = TopComponents.getInstance().getPolicyTree();
        final List<AssertionTreeNode> selected = getSelectedNodes(policyTree);
        if (!selected.isEmpty()) {
            final Frame topParent = TopComponents.getInstance().getTopParent();
            DialogDisplayer.showInputDialog(topParent,
                    "Fragment Name", "Fragment Name", JOptionPane.QUESTION_MESSAGE,
                    null, null, null, new DialogDisplayer.InputListener() {
                @Override
                public void reportResult(final Object option) {
                    if (option != null) {
                        if (option.toString().isEmpty()) {
                            showErrorDialog(topParent, "Fragment Name must be specified.", true);
                        } else {
                            String fragmentName = option.toString();
                            if (fragmentName.length() > MAX_NAME_LENGTH) {
                                // truncate
                                fragmentName = fragmentName.substring(0, MAX_NAME_LENGTH);
                            }
                            try {
                                final String fragmentGuid = createFragment(fragmentName, selected);
                                updatePolicy(policyTree, selected, fragmentGuid);
                            } catch (final DuplicateObjectException e) {
                                showErrorDialog(topParent, "Fragment name is already in use.", true);
                            } catch (final PolicyAssertionException e) {
                                log.log(Level.WARNING, "Error with fragment policy: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                                showErrorDialog(topParent, "Error creating fragment due to invalid policy.", false);
                            } catch (final SaveException e) {
                                log.log(Level.WARNING, "Error saving fragment: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                                showErrorDialog(topParent, "Error saving fragment.", false);
                            }
                        }
                    } else {
                        // dialog was cancelled
                        log.log(Level.FINE, "Operation cancelled");
                    }
                }
            });
        }
    }

    /**
     * Input errors will ask for input again.
     */
    private void showErrorDialog(final Frame topParent, final String error, final boolean inputError) {
        DialogDisplayer.showMessageDialog(topParent,
                error, "Create Include Fragment Error",
                JOptionPane.ERROR_MESSAGE, inputError ? new Runnable() {
            @Override
            public void run() {
                // start over when the error dialog is dismissed
                performAction();
            }
        } : null);
    }

    /**
     * Updates the policy tree by inserting an include assertion and removing the assertions that were added to the include.
     */
    private void updatePolicy(final JTree policyTree, final List<AssertionTreeNode> selected, final String fragmentGuid) {
        final DefaultTreeModel policyModel = (DefaultTreeModel) policyTree.getModel();
        final int insertPosition = assertionNode.getParent().getIndex(assertionNode) + 1;
        policyModel.insertNodeInto(AssertionTreeNodeFactory.asTreeNode(new Include(fragmentGuid)), parentNode, insertPosition);
        for (final AssertionTreeNode assertionNode : selected) {
            policyModel.removeNodeFromParent(assertionNode);
        }
    }

    /**
     * Updates the services and policy tree so that the newly created fragment is displayed.
     */
    private void addFragmentToServicesTree(final Policy fragment) {
        final JTree servicesTree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        final DefaultTreeModel servicesModel = (DefaultTreeModel) servicesTree.getModel();
        final AbstractTreeNode policiesFolderNode = TopComponents.getInstance().getPoliciesFolderNode();
        final PolicyHeader fragmentHeader = new PolicyHeader(fragment);
        final AbstractTreeNode fragmentNode = TreeNodeFactory.asTreeNode(new PolicyHeader(fragment), null);
        servicesModel.insertNodeInto(fragmentNode, policiesFolderNode, policiesFolderNode.getInsertPosition(fragmentNode, RootNode.getComparator()));
        final RootNode rootNode = (RootNode) servicesModel.getRoot();
        rootNode.addEntity(fragmentHeader.getOid(), fragmentNode);
    }

    /**
     * Creates an include fragment with policy xml from the selected nodes.
     */
    private String createFragment(final String fragmentName, final List<AssertionTreeNode> selected) throws PolicyAssertionException, SaveException {
        final AllAssertion allAssertion = new AllAssertion();
        for (final AssertionTreeNode node : selected) {
            allAssertion.addChild(node.asAssertion());
        }
        final String xml = WspWriter.getPolicyXml(allAssertion);
        final Policy fragment = new Policy(PolicyType.INCLUDE_FRAGMENT, fragmentName, xml, false);
        final PolicyAdmin policyAdmin = Registry.getDefault().getPolicyAdmin();
        final Pair<Long, String> oidUuid = policyAdmin.savePolicy(fragment);
        fragment.setOid(oidUuid.left);
        fragment.setGuid(oidUuid.right);
        addFragmentToServicesTree(fragment);
        return oidUuid.right;
    }

    private List<AssertionTreeNode> getSelectedNodes(final JTree policyTree) {
        final TreePath[] paths = policyTree.getSelectionPaths();
        final List<AssertionTreeNode> nodeList = new ArrayList<AssertionTreeNode>();
        if (paths != null) {
            for (TreePath path : paths) {
                nodeList.add((AssertionTreeNode) path.getLastPathComponent());
            }
        }
        return nodeList;
    }
}
