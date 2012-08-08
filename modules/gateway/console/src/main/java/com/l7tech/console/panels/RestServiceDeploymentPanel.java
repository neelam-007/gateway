package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * <p>The first panel in the {@link PublishRestServiceWizard}.  This page allows the user to select how a RESTful service is being deployed.</p>
 */
public class RestServiceDeploymentPanel extends WizardStepPanel {
    private static final String DESCRIPTION = "Specify if the REST service to be imported is from a WADL or manual entry.";
    private JRadioButton rbImportWadl;
    private JRadioButton rbManualEntry;
    private JPanel mainPanel;

    public RestServiceDeploymentPanel(final WizardStepPanel next){
        super(next);
        initialize();
    }

    private void initialize(){
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public String getStepLabel() {
        return "Deploy REST Service From";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public void storeSettings(final Object settings) throws IllegalArgumentException {
        if(settings instanceof PublishRestServiceWizard.RestServiceConfig){
            PublishRestServiceWizard.RestServiceConfig config = (PublishRestServiceWizard.RestServiceConfig)settings;
            config.setManualEntry(rbManualEntry.isSelected());
        }
    }
}
