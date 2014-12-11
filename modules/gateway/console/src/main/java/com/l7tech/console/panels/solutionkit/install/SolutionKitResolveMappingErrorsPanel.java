package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 *
 */
public class SolutionKitResolveMappingErrorsPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(SolutionKitResolveMappingErrorsPanel.class.getName());
    private static final String STEP_LABEL = "Resolve entity conflicts";
    private static final String STEP_DESC = "Resolve entity conflicts.";

    private JPanel mainPanel;
    private JTextArea textArea1;

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
        String mappings = settings.getTestMappingResults().entrySet().iterator().next().getValue();
        textArea1.setText(mappings);
        textArea1.setCaretPosition(0);
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        //
    }

    private void initialize() {
        textArea1.setEditable(false);

        setLayout(new BorderLayout());
        add(mainPanel);
    }
}