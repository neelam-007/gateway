package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitMappingsPanel;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.common.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Pair;
import com.sun.istack.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Wizard panel which allows the user resolve entity mapping errors in a solution kit.
 */
public class SolutionKitResolveMappingErrorsPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final String STEP_LABEL = "Resolve entity conflicts";
    private static final String STEP_DESC = "Resolve entity conflicts (or click Finish if none exist).";

    private JPanel mainPanel;
    private JTabbedPane solutionKitMappingsTabbedPane;
    private JButton resolveButton;

    private Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIdsMap = new LinkedHashMap<>();    // the map of a value: key = from id. value  = to id.

    private Map<String, Integer> guidToActiveErrorMap = new HashMap<>(); //key = solution kit guid, value = number of active errors

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
        guidToActiveErrorMap.clear();
        solutionKitMappingsTabbedPane.removeAll();

        for (final SolutionKit solutionKit: settings.getSelectedSolutionKits()) {
            Bundle bundle = settings.getBundle(solutionKit);
            if (bundle == null) continue;

            Mappings mappings = settings.getTestMappings(solutionKit);
            if (mappings == null) continue;

            Map<String, String> resolvedEntityIds = new HashMap<>();
            resolvedEntityIdsMap.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, resolvedEntityIds));

            final SolutionKitMappingsPanel solutionKitMappingsPanel = new SolutionKitMappingsPanel();
            solutionKitMappingsPanel.setData(mappings, bundle, resolvedEntityIds);
            solutionKitMappingsPanel.setDoubleClickAction(resolveButton);
            solutionKitMappingsPanel.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    refreshMappingsTableButtons();
                }
            });

            String solutionKitName = solutionKit.getName();
            solutionKitMappingsTabbedPane.add(solutionKitName, solutionKitMappingsPanel);

            String solutionKitGuid = solutionKit.getSolutionKitGuid();

            Object[] solutionKitGuidArray = resolvedEntityIdsMap.keySet().toArray();
            // Look through mappings for error type
            for (Mapping mapping : mappings.getMappings()) {
                if (mapping.getErrorType() != null) {
                    if (guidToActiveErrorMap.containsKey(solutionKitGuid)) {
                        guidToActiveErrorMap.put(solutionKitGuid, guidToActiveErrorMap.get(solutionKitGuid) + 1);
                    } else {
                        guidToActiveErrorMap.put(solutionKitGuid, 1);
                        //Set the tab containing errors to red
                        final int tabIndex = Arrays.asList(solutionKitGuidArray).indexOf(solutionKitGuid);
                        solutionKitMappingsTabbedPane.setBackgroundAt(tabIndex, Color.red);
                        solutionKitMappingsTabbedPane.setForegroundAt(tabIndex, Color.red);
                    }
                }

            }
        }
        refreshMappingsTableButtons();
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setResolvedEntityIds(resolvedEntityIdsMap);
    }

    @Override
    public boolean canFinish() {
        return areAllConflictsResolved();
    }

    private void initialize() {
        resolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SolutionKitMappingsPanel selectedSKMappingsPanel =
                    (SolutionKitMappingsPanel) solutionKitMappingsTabbedPane.getSelectedComponent();
                onResolve(selectedSKMappingsPanel);
                notifyListeners();
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

    private boolean areAllConflictsResolved() {
        Object[] solutionKitGuidArray = resolvedEntityIdsMap.keySet().toArray();

        for (int i = 0; i < solutionKitMappingsTabbedPane.getTabCount(); i++) {
            if (guidToActiveErrorMap.containsKey(solutionKitGuidArray[i]) && guidToActiveErrorMap.get(solutionKitGuidArray[i]) > 0) {
                return false;
            }
        }

        return true;
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

        if (!SolutionKitsConfig.allowOverride(mapping)) {
            DialogDisplayer.showMessageDialog(SolutionKitResolveMappingErrorsPanel.this, "<html>This Solution Kit does not allow overriding of this mapping." +
                            "<br>This mapping requires the property '" + SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' be set to true by the .skar file author.</html>",
                    "Error Resolving Entity Conflict", JOptionPane.ERROR_MESSAGE, null);
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

            Object[] solutionKitGuidArray = resolvedEntityIdsMap.keySet().toArray();

            //The guid according to the current TabbedPane index (the tabbed index corresponds to index in solutionKitGuidArray because of linkedHashMap)
            String solutionKitGuid = (String) solutionKitGuidArray[solutionKitMappingsTabbedPane.getSelectedIndex()];

            int count = guidToActiveErrorMap.get(solutionKitGuid) -1;
            guidToActiveErrorMap.put(solutionKitGuid, count);

            //set the tab back to white if conditions are good
            if (count < 1) {
                guidToActiveErrorMap.remove(solutionKitGuid);
                solutionKitMappingsTabbedPane.setBackgroundAt(solutionKitMappingsTabbedPane.getSelectedIndex(),Color.WHITE);
                solutionKitMappingsTabbedPane.setForegroundAt(solutionKitMappingsTabbedPane.getSelectedIndex(), Color.BLACK);
            }
        }
    }
}