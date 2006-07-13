package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Jun 22, 2006
 * Time: 10:14:03 AM
 */
public class SystemConfigWizardNtpStep extends BaseConsoleStep {
    private static final Logger logger = Logger.getLogger(SystemConfigWizardNtpStep.class.getName());

    private static final String TITLE = "Configure Time Synchronization (NTP)";
    private NtpConfigurationBean ntpBean;


    public SystemConfigWizardNtpStep(ConfigurationWizard parentWizard) {
        super(parentWizard);
        init();
    }

    private void init() {
        configBean = new NtpConfigurationBean("Network Interface Configuration", "");
        configCommand = new NtpConfigurationCommand(configBean);
        ntpBean = (NtpConfigurationBean) configBean;}

    //each step must implement these.
    public boolean validateStep() {
        return true;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        try {
            String shouldConfigureNtp = getData(new String[] {
                    "Would you like to configure time synchronization on this system (NTP) [y]:"
            },"y");

            if (isYes(shouldConfigureNtp)) {
                String existingNtpServer = "";
                doNtpConfigurationPrompts(existingNtpServer);
            }
            storeInput();
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    private void doNtpConfigurationPrompts(String existingNtpServer) throws IOException, WizardNavigationException {

        String prompt = "Enter the time server to use for synchronization ";
        if (StringUtils.isNotEmpty(existingNtpServer))
            prompt += "[" + existingNtpServer + "]";
        else
            existingNtpServer = "";

        prompt += " : ";

        boolean isValid = false;

        String timeserverLine = null;
        String tsAddress = null;

        do {
            isValid = false;
            timeserverLine = getData(
                    new String[]{prompt},
                    existingNtpServer
            );

            if (StringUtils.isEmpty(timeserverLine))
                timeserverLine = "";

            try {
                tsAddress = consoleWizardUtils.resolveHostName(timeserverLine);

                isValid = (tsAddress != null);

                if (!isValid) {
                    printText("*** " + timeserverLine + " cannot be resolved to a valid host ***" + getEolChar());
                    isValid = false;
                }
            } catch (UnknownHostException e) {
                printText("*** " + timeserverLine + " cannot be resolved to a valid host ***" + getEolChar());
                isValid = false;
            }

        } while (!isValid);

        ntpBean.setTimeServerAddress(tsAddress);
        ntpBean.setTimeServerName(timeserverLine);
    }

    public String getTitle() {
        return TITLE;
    }
}
