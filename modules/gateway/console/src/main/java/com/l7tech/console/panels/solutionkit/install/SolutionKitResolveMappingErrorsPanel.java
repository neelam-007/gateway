package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitMappingsPanel;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.sun.istack.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Wizard panel which allows the user resolve entity mapping errors in a solution kit.
 */
public class SolutionKitResolveMappingErrorsPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final String STEP_LABEL = "Resolve entity conflicts";
    private static final String STEP_DESC = "Resolve entity conflicts.";

    private JPanel mainPanel;
    private JTabbedPane solutionKitMappingsTabbedPane;
    private JButton resolveButton;

    private Map<SolutionKit, Map<String, String>> resolvedEntityIdsMap = new HashMap<>();    // the map of a value: key = from id. value  = to id.

    public SolutionKitResolveMappingErrorsPanel() {
        super(null);
        initialize();
    }

    @Override
    public String getStepLabel() {
        return STEP_LABEL;
    }

    @Override
    public String getDescription() {
        return STEP_DESC;
    }

    @Override
    public boolean canAdvance() {
        return super.canAdvance();
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        resolvedEntityIdsMap.clear();
        solutionKitMappingsTabbedPane.removeAll();

        for (final SolutionKit solutionKit: settings.getSelectedSolutionKits()) {
            Bundle bundle = settings.getBundle(solutionKit);
            if (bundle == null) continue;

            Mappings mappings = settings.getTestMappings(solutionKit);
            if (mappings == null) continue;

            Map<String, String> resolvedEntityIds = new HashMap<>();
            resolvedEntityIdsMap.put(solutionKit, resolvedEntityIds);

            final SolutionKitMappingsPanel solutionKitMappingsPanel = new SolutionKitMappingsPanel();
            solutionKitMappingsPanel.setData(mappings, bundle, resolvedEntityIds);
            solutionKitMappingsPanel.setDoubleClickAction(resolveButton);
            solutionKitMappingsPanel.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    refreshMappingsTableButtons();
                }
            });

            solutionKitMappingsTabbedPane.add(solutionKit.getName(), solutionKitMappingsPanel);
        }

        refreshMappingsTableButtons();
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setResolvedEntityIds(resolvedEntityIdsMap);
    }

    private void initialize() {
        resolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SolutionKitMappingsPanel selectedSKMappingsPanel =
                    (SolutionKitMappingsPanel) solutionKitMappingsTabbedPane.getSelectedComponent();
                onResolve(selectedSKMappingsPanel);
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void refreshMappingsTableButtons() {
        boolean enabled = false;
        final Component selectedComponent = solutionKitMappingsTabbedPane.getSelectedComponent();

        if (selectedComponent instanceof SolutionKitMappingsPanel) {
            final SolutionKitMappingsPanel solutionKitMappingsPanel = (SolutionKitMappingsPanel) selectedComponent;
            Mapping mapping = solutionKitMappingsPanel.getSelectedMapping();
            if (mapping != null) {
                Mapping.ErrorType errorType = mapping.getErrorType();
                if (errorType != null) {
                    enabled = true;
                }
            }
        }

        resolveButton.setEnabled(enabled);
    }

    private void onResolve(@NotNull final SolutionKitMappingsPanel solutionKitMappingsPanel) {
        Mapping mapping = solutionKitMappingsPanel.getSelectedMapping();
        if (mapping == null) {
            return;
        }

        Mapping.ErrorType errorType = mapping.getErrorType();
        if (errorType == null) {
            return;
        }

        final SolutionKitResolveMappingDialog dlg = new SolutionKitResolveMappingDialog(this.getOwner(), mapping,
            solutionKitMappingsPanel.getBundleItems().get(mapping.getSrcId())
        );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);

        if (dlg.isConfirmed()) {
            solutionKitMappingsPanel.getResolvedEntityIds().put(mapping.getSrcId(), dlg.getResolvedId());
            solutionKitMappingsPanel.reload();
        }
    }
}