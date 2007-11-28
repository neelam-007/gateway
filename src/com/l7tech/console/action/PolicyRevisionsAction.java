package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.policy.Policy;
import com.l7tech.console.panels.PolicyRevisionsDialog;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;

/**
 * Views revision history for a policy (or published service).
 */
public class PolicyRevisionsAction extends NodeAction {
    private final boolean service;

    public PolicyRevisionsAction(PolicyEntityNode node) {
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
        final PolicyEntityNode policyNode = (PolicyEntityNode)node;
        try {
            Policy policy = policyNode.getPolicy();
            PolicyRevisionsDialog dlg = new PolicyRevisionsDialog(TopComponents.getInstance().getTopParent(), policy.getOid());
            dlg.pack();
            DialogDisplayer.display(dlg);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }
}