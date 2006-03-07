package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.WindowsServiceCommand;
import com.l7tech.server.config.beans.WindowsServiceBean;

import javax.swing.*;
import java.util.HashMap;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 25, 2005
 * Time: 10:49:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardWinServicePanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;
    private JRadioButton yesService;
    private JRadioButton noService;

    public ConfigWizardWinServicePanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        configBean = new WindowsServiceBean(osFunctions);
        configCommand = new WindowsServiceCommand(configBean);

        stepLabel = "Configure the SSG as a service";

        ButtonGroup group = new ButtonGroup();
        group.add(yesService);
        group.add(noService);

        yesService.setSelected(true);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateModel(HashMap settings) {
        ((WindowsServiceBean)configBean).setDoService(yesService.isSelected());
    }

    protected void updateView(HashMap settings) {
    }
}
