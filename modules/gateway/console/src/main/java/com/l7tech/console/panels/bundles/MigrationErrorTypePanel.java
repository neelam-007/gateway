package com.l7tech.console.panels.bundles;

import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.util.Map;

/**
 * A class holds a panel containing migration target type such as "Target Exists" and "Target Not Found".
 */
public class MigrationErrorTypePanel {
    private JPanel contentPane;
    private JPanel errorTypeContainerPanel;

    public MigrationErrorTypePanel(JDialog parent, String errorType, String targetType, java.util.List<Pair<String, String>> targetList, Map<String, String> selectedMigrationResolutions) {
        Border border = contentPane.getBorder();
        if (border instanceof TitledBorder) {
            ((TitledBorder) border).setTitle(targetType);
        }

        for (Pair<String, String> targetPair: targetList) {
            errorTypeContainerPanel.add(
                new MigrationEntityDetailPanel(parent, errorType, targetType, targetPair.left, targetPair.right, selectedMigrationResolutions).getContentPane()
            );
        }
    }

    private void createUIComponents() {
        errorTypeContainerPanel = new JPanel();
        errorTypeContainerPanel.setLayout(new BoxLayout(errorTypeContainerPanel, BoxLayout.Y_AXIS));
    }

    public JPanel getContentPane() {
        return contentPane;
    }
}