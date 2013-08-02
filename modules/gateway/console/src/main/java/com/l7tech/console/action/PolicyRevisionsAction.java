package com.l7tech.console.action;

import com.l7tech.console.panels.PolicyRevisionsDialog;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;

/**
 * Views revision history for a policy (or published service).
 */
public class PolicyRevisionsAction extends NodeAction {
    private final boolean service;

    public PolicyRevisionsAction(EntityWithPolicyNode node) {
        super(node);
        service = node instanceof ServiceNode;
    }

    public String getName() {
        return "Revision History";
    }

    public String getDescription() {
        return service ? "View Web service revision history" : " View revision history";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/policy16.gif";
    }

    protected void performAction() {
        final EntityWithPolicyNode policyNode = (EntityWithPolicyNode)node;
        try {
            Policy policy = policyNode.getPolicy();
            PolicyRevisionsDialog dlg = new PolicyRevisionsDialog(TopComponents.getInstance().getTopParent(), policyNode, policy.getGoid());
            dlg.pack();
            DialogDisplayer.display(dlg);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }
}