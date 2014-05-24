package com.l7tech.console.action;

import com.l7tech.console.panels.policydiff.PolicyDiffContext;
import com.l7tech.console.panels.policydiff.PolicyDiffWindow;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.PolicyRevisionUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.io.IOException;

/**
 *  The action is used to diff entity node(s) from the Services And Polices Tree.
 */
public class DiffPolicyAction extends EntityWithPolicyNodeAction<EntityWithPolicyNode> {
    public DiffPolicyAction(EntityWithPolicyNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return "Compare Policy: " + (PolicyDiffContext.hasLeftDiffPolicy()? "Right" : "Left");
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/policyDiff16.png";
    }

    @Override
    protected OperationType getOperation() {
        return OperationType.READ;
    }

    @Override
    protected void performAction() {
        if (PolicyDiffContext.hasLeftDiffPolicy()) {
            new PolicyDiffWindow(PolicyDiffContext.getLeftDiffPolicyInfo(), getPolicyInfo()).setVisible(true);
        } else {
            PolicyDiffContext.setLeftDiffPolicyInfo(getPolicyInfo());
        }
    }

    /**
     * Obtain policy information such as policy full name and policy xml
     *
     * @return a pair of two strings: policy full name (policy name, resolution, version, and active status) and policy xml.
     */
    private Pair<String, PolicyTreeModel> getPolicyInfo() {
        final Policy policy;
        try {
            //noinspection unchecked
            policy = entityNode.getPolicy();
        } catch (FindException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                "Cannot find the policy, '" + entityNode.getName() + "'", "Policy Comparison Error",
                JOptionPane.WARNING_MESSAGE, null
            );
            return null;
        }

        final Goid policyGoid = policy.getGoid();
        final PolicyVersion latestPolicyVersion = Registry.getDefault().getPolicyAdmin().findLatestRevisionForPolicy(policyGoid);
        final String policyFullName = PolicyRevisionUtils.getDisplayName(node.getName(), policy.getVersionOrdinal(), latestPolicyVersion.getOrdinal(), policy.isVersionActive());

        final String policyXml = policy.getXml();
        PolicyTreeModel policyTreeModel;
        try {
            policyTreeModel = new PolicyTreeModel(WspReader.getDefault().parsePermissively(policyXml, WspReader.Visibility.includeDisabled));
        } catch (IOException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "Cannot parse the policy XML", "Policy Comparison Error", JOptionPane.WARNING_MESSAGE, null);
            return null;
        }

        return new Pair<>(policyFullName, policyTreeModel);
    }
}