package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
            doTimeZoneConfig();
            doNtpConfig();
            storeInput();
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    private void doTimeZoneConfig() throws IOException, WizardNavigationException {
        if (StringUtils.isNotEmpty(osFunctions.getTimeZonesDir())) {
            String shouldConfigureTz = getData(new String[] {"Would you like to configure the timezone on this system  [y]:"}, "y");
            if (isYes(shouldConfigureTz)) {
                File tzInfo = doTzConfigurationPrompts(new File(osFunctions.getTimeZonesDir()));
                String absolutePath = tzInfo.getAbsolutePath();
                String base = osFunctions.getTimeZonesDir();
                ntpBean.setTimeZoneInfo(absolutePath.replace(base, ""));
            }
        }
    }

    private File doTzConfigurationPrompts(File dir) throws IOException, WizardNavigationException {
        if (dir != null) {
            if (!dir.isDirectory()) {
                return dir;
            }
        }
        List<String> prompts = new ArrayList<String>();
        prompts.add("Select a timezone from the following list " + getEolChar());
        if (dir.isDirectory())
            prompts.add("[" + dir.getName() + "]" + getEolChar());

        File f = null;
        if (!dir.exists()) {
            printText("*** " + "Could not determine available timezones. Timezone directory \"" + dir.getAbsolutePath() + "\" does not exist" + " ***" + getEolChar());
            logger.warning("Could not determine available timezones. Timezone directory \"" + dir.getAbsolutePath() + "\" does not exist");
        } else {
            File[] files = dir.listFiles();

            int x = 1;
            for (File file : files) {
                String indexStr = String.valueOf(x++);
                String prompt = indexStr + ") " + file.getName() + (file.isDirectory()?"[more choices]":"");
                prompts.add(prompt + getEolChar());
            }
            
            prompts.add("Please make a selection [1]: ");
            String[] allowedEntries = new String[x];
            for (int index=1; index <= x; ++index) {
                allowedEntries[index-1] = String.valueOf(index);
            }

            String tzSelection = getData(prompts, "1", allowedEntries);
            int whichChoice = Integer.parseInt(tzSelection);

            f = doTzConfigurationPrompts(files[whichChoice -1]);
        }
        return f;
    }

    private void doNtpConfig() throws IOException, WizardNavigationException {
        String shouldConfigureNtp = getData(
            new String[] {"Would you like to configure time synchronization on this system (NTP) [y]:"},"y");

        if (isYes(shouldConfigureNtp)) {
            doNtpConfigurationPrompts("");
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
