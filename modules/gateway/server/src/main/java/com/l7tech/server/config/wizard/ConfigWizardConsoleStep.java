package com.l7tech.server.config.wizard;

import com.l7tech.server.config.exceptions.WizardNavigationException;

/**
 * User: megery
 * Date: Dec 19, 2005
 * Time: 4:53:32 PM
 */
public interface ConfigWizardConsoleStep {
    void showTitle();

    void showStep(boolean isValidated) throws WizardNavigationException;

    boolean shouldApplyConfiguration();

    boolean isShowNavigation();

    boolean isShowQuitMessage();
}
