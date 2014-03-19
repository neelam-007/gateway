package com.l7tech.console.panels.stepdebug;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeCellRenderer;
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
}