package com.l7tech.console.action;

import com.l7tech.console.panels.stepdebug.PolicyStepDebugDialog;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedOther;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;

/**
 * The action used to display the {@link PolicyStepDebugDialog}.
 */
public class PolicyStepDebugAction extends NodeAction {

    /**
     * Creates <code>PolicyStepDebugAction</code>.
     *
     * @param serviceNode the service node
     */
    public PolicyStepDebugAction(ServiceNode serviceNode) {
        super(serviceNode, (Class) null, new AttemptedOther(EntityType.SERVICE, OtherOperationName.DEBUGGER.getOperationName()));
    }

    /**
     * Creates <code>PolicyStepDebugAction</code>.
     *
     * @param policyNode the policy entity node
     */
    public PolicyStepDebugAction(PolicyEntityNode policyNode) {
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
            Goid entityGoid = policyNode.getEntityGoid();
            Policy policy = policyNode.getPolicy();
            PolicyStepDebugDialog dlg = new PolicyStepDebugDialog(TopComponents.getInstance().getTopParent(), entityGoid, policy);
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
            DialogDisplayer.display(dlg);
        } catch (FindException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null, "The policy is not found.", e, null);
        }
    }
}