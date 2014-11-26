package com.l7tech.console.panels.bundles;

import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.util.Map;
import java.util.Properties;

/**
 * A class holds a panel containing migration target type such as "Target Exists" and "Target Not Found".
 */
public class MigrationErrorTypePanel {
    private JPanel contentPane;
    private JPanel errorTypeContainerPanel;

    private final JDialog parent;
    private final ConflictDisplayerDialog.ErrorType errorType;
    private final String targetType;
    private final boolean versionModified;
    private final Map<String, Pair<ConflictDisplayerDialog.MappingAction, Properties>> selectedMigrationResolutions;

    public MigrationErrorTypePanel(JDialog parent, ConflictDisplayerDialog.ErrorType errorType, String targetType,
                                   boolean versionModified,
                                   Map<String, Pair<ConflictDisplayerDialog.MappingAction, Properties>> selectedMigrationResolutions) {
        this.parent = parent;
        this.errorType = errorType;
        this.targetType = targetType;
        this.versionModified = versionModified;
        this.selectedMigrationResolutions = selectedMigrationResolutions;

        Border border = contentPane.getBorder();
        if (border instanceof TitledBorder) {
            ((TitledBorder) border).setTitle(targetType);
        }
    }

    public void addMigrationError(final String name, final String srcId, final String extraInfo) { // For Policy or Service entity, extraInfo is a policy xml.
        errorTypeContainerPanel.add(
                new MigrationEntityDetailPanel(parent, errorType, targetType, name, srcId, versionModified, extraInfo, selectedMigrationResolutions).getContentPane()
        );
    }

    private void createUIComponents() {
        errorTypeContainerPanel = new JPanel();
        errorTypeContainerPanel.setLayout(new BoxLayout(errorTypeContainerPanel, BoxLayout.Y_AXIS));
    }

    public JPanel getContentPane() {
        return contentPane;
    }
}