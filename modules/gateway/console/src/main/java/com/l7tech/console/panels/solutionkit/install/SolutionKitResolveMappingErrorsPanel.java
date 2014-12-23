package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitMappingsPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class SolutionKitResolveMappingErrorsPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(SolutionKitResolveMappingErrorsPanel.class.getName());
    private static final String STEP_LABEL = "Resolve entity conflicts";
    private static final String STEP_DESC = "Resolve entity conflicts.";

    private JPanel mainPanel;
    private SolutionKitMappingsPanel solutionKitMappingsPanel;
    private JButton resolveButton;

    private Map<String, Item> bundleItems = new HashMap<>();            // key = bundle reference item id. value = bundle reference item.
    private Map<String, String> resolvedEntityIds = new HashMap<>();    // key = from id. value  = to id.

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
        // todo (kpak) - check all resolved.
        return super.canAdvance();
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        SolutionKit solutionKit = settings.getSingleSelectedSolutionKit();
        Bundle bundle = settings.getBundle(solutionKit);
        Mappings mappings = settings.getTestMappings(solutionKit);

        bundleItems.clear();
        for (Item aItem : bundle.getReferences()) {
            bundleItems.put(aItem.getId(), aItem);
        }
        resolvedEntityIds.clear();
        solutionKitMappingsPanel.setData(mappings, bundle, resolvedEntityIds);
        refreshMappingsTableButtons();
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        SolutionKit solutionKit = settings.getSingleSelectedSolutionKit();
        Map<SolutionKit, Map<String, String>> map = new HashMap<>();
        map.put(solutionKit, resolvedEntityIds);
        settings.setResolvedEntityIds(map);
    }

    private void initialize() {
        solutionKitMappingsPanel.hideTargetIdColumn();
        solutionKitMappingsPanel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refreshMappingsTableButtons();
            }
        });

        resolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onResolve();
            }
        });

        solutionKitMappingsPanel.setDoubleClickAction(resolveButton);

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void refreshMappingsTableButtons() {
        boolean enabled = false;
        Mapping mapping = solutionKitMappingsPanel.getSelectedMapping();
        if (mapping != null) {
            Mapping.ErrorType errorType = mapping.getErrorType();
            if (errorType != null) {
                enabled = true;
            }
        }
        resolveButton.setEnabled(enabled);
    }

    private void onResolve() {
        Mapping mapping = solutionKitMappingsPanel.getSelectedMapping();
        if (mapping == null) {
            return;
        }

        Mapping.ErrorType errorType = mapping.getErrorType();
        if (errorType == null) {
            return;
        }

        final SolutionKitResolveMappingDialog dlg = new SolutionKitResolveMappingDialog(this.getOwner(), mapping, bundleItems.get(mapping.getSrcId()));
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);

        if (dlg.isConfirmed()) {
            resolvedEntityIds.put(mapping.getSrcId(), dlg.getResolvedId());
            solutionKitMappingsPanel.reload();
        }
    }
}