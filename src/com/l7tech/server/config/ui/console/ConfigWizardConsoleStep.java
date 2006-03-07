package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Dec 19, 2005
 * Time: 4:53:32 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ConfigWizardConsoleStep {
    ConfigurationCommand getConfigurationCommand();
    void collectInput();

    void setPreviousStep(ConfigWizardConsoleStep prev);
    void setNextStep(ConfigWizardConsoleStep next);

    ConfigWizardConsoleStep getPreviousStep();
    ConfigWizardConsoleStep getNextStep();

    void goForward();
    void goBack();
}
