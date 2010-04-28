package com.l7tech.console.poleditor;

import com.l7tech.console.tree.AssertionLineNumbersTree;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;

/**
 * This is a pane to hold the assertion-line-numbers tree and the policy tree.
 *
 * @author ghuang
 */
public class NumberedPolicyTreePane {
    private JPanel policyTreePane;
    private AssertionLineNumbersTree assertionLineNumbersTree;
    private PolicyTree policyTree;

    public JPanel getPolicyTreePane() {
        return policyTreePane;
    }

    public AssertionLineNumbersTree getAssertionLineNumbersTree() {
        return assertionLineNumbersTree;
    }

    private void createUIComponents() {
        policyTree = (PolicyTree) TopComponents.getInstance().getPolicyTree();
        assertionLineNumbersTree = new AssertionLineNumbersTree(policyTree);
    }
}
