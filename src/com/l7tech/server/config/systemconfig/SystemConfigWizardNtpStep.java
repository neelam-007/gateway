package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
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
    public static final int TIMEZONES_PER_PAGE = 10;


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
            boolean shouldConfigureTz = getConfirmationFromUser("Would you like to configure the timezone on this system?","y");

            if (shouldConfigureTz) {
                File tzInfo = doTzConfigurationPrompts(new File(osFunctions.getTimeZonesDir()));
                String absolutePath = tzInfo.getAbsolutePath();
                String base = osFunctions.getTimeZonesDir();
                ntpBean.setTimeZoneInfo(absolutePath.replace(base, ""));
            }
        }
    }

    private File doTzConfigurationPrompts(File dir) throws IOException, WizardNavigationException {
        if (dir == null)
            return null;

        if (!dir.isDirectory())
            return dir;

        File selectedTimeZone = null;
        if (!dir.exists()) {
            printText("*** " + "Could not determine available timezones. Timezone directory \"" + dir.getAbsolutePath() + "\" does not exist" + " ***" + getEolChar());
            logger.warning("Could not determine available timezones. Timezone directory \"" + dir.getAbsolutePath() + "\" does not exist");
        } else {
            File[] allTimeZones = getTimezones(dir);
            File[] timezonesToDisplay;
            int howManyTimeZones = allTimeZones.length;
            List<String> allowedEntries = new ArrayList<String>();
            for (int i = 0; i < howManyTimeZones; i++) {
                allowedEntries.add(String.valueOf(i+1));
            }

            int whichIndex;
            if (howManyTimeZones > TIMEZONES_PER_PAGE) {
                //try paging the view
                int displayedCount = 0;

                timezonesToDisplay = new File[TIMEZONES_PER_PAGE];
                System.arraycopy(allTimeZones, 0, timezonesToDisplay, 0, TIMEZONES_PER_PAGE);
                boolean hasMoreEntries = true;
                while (displayedCount < howManyTimeZones && selectedTimeZone == null) {
                    hasMoreEntries = (displayedCount + TIMEZONES_PER_PAGE) < allTimeZones.length;
                    whichIndex = showTimeZones(timezonesToDisplay, dir.equals(new File(osFunctions.getTimeZonesDir()))?null:dir, displayedCount + 1, allowedEntries, hasMoreEntries);
                    displayedCount += TIMEZONES_PER_PAGE;
                    if (whichIndex >= 0) {
                        selectedTimeZone = allTimeZones[whichIndex];
                    } else {
                        int length = TIMEZONES_PER_PAGE;
                        if ( (displayedCount + TIMEZONES_PER_PAGE) > allTimeZones.length) {
                            length = allTimeZones.length - displayedCount;
                        }
                        timezonesToDisplay = new File[length];
                        System.arraycopy(allTimeZones, displayedCount, timezonesToDisplay, 0, length);
                    }
                }
            } else {
                whichIndex = showTimeZones(allTimeZones, dir, 1, allowedEntries, false);
                selectedTimeZone = allTimeZones[whichIndex];
            }

            selectedTimeZone = doTzConfigurationPrompts(selectedTimeZone);
        }
        return selectedTimeZone;
    }

    private File[] getTimezones(File baseDir) {
        File[] subdirs = baseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (subdirs != null) Arrays.sort(subdirs);

        File[] filesonly = baseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return !pathname.isDirectory();
            }
        });
        if (filesonly != null) Arrays.sort(filesonly);

        List<File> allOfThem = new ArrayList<File>(Arrays.asList(subdirs));
        allOfThem.addAll(Arrays.asList(filesonly));

        return allOfThem.toArray(new File[0]);
    }

    private int showTimeZones(File[] timezones, File baseDir, int startingIndex, List<String> allowedEntries, boolean hasMoreEntries) throws IOException, WizardNavigationException {
        List<String> prompts = new ArrayList<String>();
        prompts.add("Select a timezone from the following list " + getEolChar());

        if (baseDir != null && baseDir.isDirectory())
            prompts.add("[" + baseDir.getName() + "]" + getEolChar());

        int x = startingIndex;
        for (File file : timezones) {
            String indexStr = String.valueOf(x++);
            String prompt = indexStr + ") " + file.getName() + (file.isDirectory()?"[more choices]":"");
            prompts.add(prompt + getEolChar());
        }

        String[] acceptedEntries;
        String defaultValue = "";
        if (!hasMoreEntries) {
            acceptedEntries = allowedEntries.toArray(new String[0]);
            prompts.add("Please make a selection : ");
        } else {
            List<String> tempList = new ArrayList<String>();
            tempList.addAll(allowedEntries);
            tempList.add(defaultValue);
            acceptedEntries = tempList.toArray(new String[0]);
            prompts.add("Please make a selection [Enter for next page]: ");
        }

        String tzSelection = getData(prompts.toArray(new String[0]), defaultValue, acceptedEntries,null);
        if (tzSelection.equals(defaultValue)) {
            return -1;
        }
        int whichChoice = Integer.parseInt(tzSelection);
        return whichChoice - 1;
    }

    private void doNtpConfig() throws IOException, WizardNavigationException {
        boolean shouldConfigureNtp = getConfirmationFromUser("Would you like to configure time synchronization on this system (NTP)?", "y");
        if (shouldConfigureNtp) {
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

        boolean isValid;

        String timeserverLine;
        String tsAddress = null;

        do {
            isValid = false;
            timeserverLine = getData(
                    new String[]{prompt},
                    existingNtpServer,
                    (String[]) null,
                    null
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
