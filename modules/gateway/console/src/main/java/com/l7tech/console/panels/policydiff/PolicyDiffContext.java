package com.l7tech.console.panels.policydiff;

import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * The class maintains the context of performing the policy diff function such as the status of choosing left policy
 * and the state of the policy diff menu item (under File of the policy manager main menu).
 */
public class PolicyDiffContext {
    // Store the left policy information such as the policy name and the policy tree model.
    private static Pair<String, PolicyTreeModel> leftDiffPolicyInfo;

    // The reference of the policy diff menu item under File
    private static JMenuItem policyDiffMenuItem;

    public static boolean hasLeftDiffPolicy() {
        return getLeftDiffPolicyInfo() != null;
    }

    @Nullable
    public static Pair<String, PolicyTreeModel> getLeftDiffPolicyInfo() {
        return leftDiffPolicyInfo;
    }

    public static void setLeftDiffPolicyInfo(final Pair<String, PolicyTreeModel> leftDiffPolicyInfo) {
        PolicyDiffContext.leftDiffPolicyInfo = leftDiffPolicyInfo;
        updatePolicyDiffMenuItemText();
    }

    public static void setPolicyDiffMenuItem(@Nullable JMenuItem policyDiffMenuItem) {
        PolicyDiffContext.policyDiffMenuItem = policyDiffMenuItem;
        updatePolicyDiffMenuItemText();
    }

    private static void updatePolicyDiffMenuItemText() {
        if (policyDiffMenuItem == null) return;

        policyDiffMenuItem.setText("Compare Policy: " + (hasLeftDiffPolicy()? "Right" : "Left"));
    }
}