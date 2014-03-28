package com.l7tech.console.panels.stepdebug;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeCellRenderer;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * The tree cell renderer for the debug policy tree. Highlights current line in execution.
 */
public class DebugPolicyTreeCellRenderer extends PolicyTreeCellRenderer{
    private final PolicyStepDebugDialog policyStepDebugDialog;
    private final Color defaultBackgroundSelectionColor;
    private final Color defaultBackgroundNoneSelectionColor;

    /**
     *  Creates <code>DebugPolicyTreeCellRenderer</code>.
     *
     * @param policyStepDebugDialog the policy step debug dialog
     */
    DebugPolicyTreeCellRenderer(@NotNull PolicyStepDebugDialog policyStepDebugDialog) {
        super();
        this.policyStepDebugDialog = policyStepDebugDialog;
        this.defaultBackgroundSelectionColor = this.getBackgroundSelectionColor();
        this.defaultBackgroundNoneSelectionColor = this.getBackgroundNonSelectionColor();
        setPolicyVersion(policyStepDebugDialog);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value,  sel, expanded, leaf, row, hasFocus);

        if (!(value instanceof AssertionTreeNode)) {
            return this;
        }

        AssertionTreeNode node = (AssertionTreeNode) value;
        if (policyStepDebugDialog.isCurrentLine(node)) {
            this.setBackgroundNonSelectionColor(Color.YELLOW);
            this.setBackgroundSelectionColor(Color.YELLOW);
            this.setForeground(this.getTextNonSelectionColor());
        } else {
            this.setBackgroundNonSelectionColor(defaultBackgroundNoneSelectionColor);
            this.setBackgroundSelectionColor(defaultBackgroundSelectionColor);
        }

        return this;
    }

    /**
     * Set policy version for this renderer for displaying tree node name with comments or not.
     * @param policyStepDebugDialog: this dialog will provide a Policy object
     */
    private void setPolicyVersion(@NotNull PolicyStepDebugDialog policyStepDebugDialog) {
        Policy policy = policyStepDebugDialog.getPolicy();
        try {
            PolicyVersion activePolicyVersion = Registry.getDefault().getPolicyAdmin().findActivePolicyVersionForPolicy(policy.getGoid());
            setPolicyVersion(activePolicyVersion);
        } catch (FindException e) {
            // Do nothing here
        }
    }
}