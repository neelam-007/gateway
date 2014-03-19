package com.l7tech.console.action;

import com.l7tech.console.panels.stepdebug.PolicyStepDebugDialog;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
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
     * @param policyNode the entity with policy node
     */
    public PolicyStepDebugAction(EntityWithPolicyNode policyNode) {
        super(policyNode);
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