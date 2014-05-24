package com.l7tech.console.action;

import com.l7tech.console.panels.stepdebug.PolicyStepDebugDialog;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.util.PolicyRevisionUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedOther;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;

/**
 * The action used to display the {@link PolicyStepDebugDialog}.
 */
public class PolicyStepDebugAction extends NodeAction {

    private final PolicyAdmin policyAdmin = Registry.getDefault().getPolicyAdmin();

    /**
     * Creates <code>PolicyStepDebugAction</code>.
     *
     * @param policyNode the policy node
     */
    public PolicyStepDebugAction(EntityWithPolicyNode<? extends Entity, ? extends EntityHeader> policyNode) {
        super(policyNode, (Class) null, new AttemptedOther(EntityType.POLICY, OtherOperationName.DEBUGGER.getOperationName()));
    }

    @Override
    public String getName() {
        return "Service Debugger";
    }

    @Override
    public String getDescription() {
        return "Service Debugger";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/bug16.gif";
    }

    @Override
    protected void performAction() {
        final EntityWithPolicyNode policyNode = (EntityWithPolicyNode) node;
        try {
            // Get the active version of policy.
            //
            Policy policy = policyAdmin.findPolicyByPrimaryKey(policyNode.getPolicy().getGoid());
            String displayName = PolicyRevisionUtils.getDisplayName(
                node.getName(),
                policy.getVersionOrdinal(),
                policyAdmin.findLatestRevisionForPolicy(policy.getGoid()).getOrdinal(),
                policy.isVersionActive());
            PolicyStepDebugDialog dlg = new PolicyStepDebugDialog(policy, displayName);
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
            dlg.setVisible(true);
        } catch (FindException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null, "The policy is not found.", e, null);
        }
    }
}