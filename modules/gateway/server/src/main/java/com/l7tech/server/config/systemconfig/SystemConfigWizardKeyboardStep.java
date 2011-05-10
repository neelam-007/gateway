package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.wizard.BaseConsoleStep;
import com.l7tech.server.config.wizard.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;

/**
 * @author jbufu
 */
public class SystemConfigWizardKeyboardStep extends BaseConsoleStep<KeyboardConfigurationBean, KeyboardConfigurationCommand> {

    // - PUBLIC

    public SystemConfigWizardKeyboardStep(ConfigurationWizard parentWizard) {
        super(parentWizard);
        configBean = new KeyboardConfigurationBean("Keyboard Configuration", "");
        configCommand = new KeyboardConfigurationCommand(configBean);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void doUserInterview(boolean validated) throws WizardNavigationException {
        try {
            doKeyboardConfig();
            storeInput();
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    @Override
    public boolean validateStep() {
        return true;
    }

    @Override
    public boolean isShowNavigation() {
        return false;
    }

    // - PACKAGE

    void keyboardConfig(String dir) throws IOException, WizardNavigationException {
        doKeyboardConfigurationPrompts(new File(dir));
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(SystemConfigWizardKeyboardStep.class.getName());

    private static final int KEYMAPS_PER_PAGE = 10;
    private static final String TITLE = "Configure Keyboard Language / Layout";

    private void doKeyboardConfig() throws IOException, WizardNavigationException {
        OSSpecificFunctions osFunctions = OSSpecificFunctions.getOSSpecificFunctions();
        String keymapsDir = osFunctions.getKeymapsDir();
        if (StringUtils.isEmpty(keymapsDir)) {
            logger.log(Level.WARNING, "Keymaps directory not found (" + keymapsDir + "), skipping keyboard configuration step.");
        } else {
            boolean shouldConfigureKeyboard = getConfirmationFromUser("Would you like to choose the keyboard layout for this system?","y");
            if (shouldConfigureKeyboard) {
                configBean.setKeymap(doKeyboardConfigurationPrompts(new File(keymapsDir)));
            }
        }
    }

    private String doKeyboardConfigurationPrompts(File dir) throws IOException, WizardNavigationException {
        String selectedKeymap = null;
        if (dir == null || ! dir.exists()) {
            printText("*** " + "Keyboard mapping directory (" + dir + ") does not exist" + " ***" + EOL);
            logger.warning("Keyboard mapping directory (" + dir + ") does not exist");
        } else {
            OSSpecificFunctions osFunctions = OSSpecificFunctions.getOSSpecificFunctions();
            String[] allKeymaps = getKeymaps(dir);
            String[] keymapsToDisplay;
            int keymapsCount = allKeymaps.length;
            List<String> allowedEntries = new ArrayList<String>();
            for (int i = 0; i < keymapsCount; i++) {
                allowedEntries.add(String.valueOf(i+1));
            }

            int whichIndex;
            if (keymapsCount > KEYMAPS_PER_PAGE) {
                //try paging the view
                int displayedCount = 0;
                keymapsToDisplay = new String[KEYMAPS_PER_PAGE];
                System.arraycopy(allKeymaps, 0, keymapsToDisplay, 0, KEYMAPS_PER_PAGE);
                boolean hasMoreEntries;
                while (displayedCount < keymapsCount && selectedKeymap == null) {
                    hasMoreEntries = (displayedCount + KEYMAPS_PER_PAGE) < allKeymaps.length;
                    whichIndex = showKeymaps(keymapsToDisplay, dir.equals(new File(osFunctions.getKeymapsDir()))?null:dir, displayedCount + 1, allowedEntries, hasMoreEntries);
                    displayedCount += KEYMAPS_PER_PAGE;
                    if (whichIndex >= 0) {
                        selectedKeymap = allKeymaps[whichIndex];
                    } else {
                        int length = KEYMAPS_PER_PAGE;
                        if ( (displayedCount + KEYMAPS_PER_PAGE) > allKeymaps.length) {
                            length = allKeymaps.length - displayedCount;
                        }
                        keymapsToDisplay = new String[length];
                        System.arraycopy(allKeymaps, displayedCount, keymapsToDisplay, 0, length);
                    }
                }
            } else {
                whichIndex = showKeymaps(allKeymaps, dir, 1, allowedEntries, false);
                selectedKeymap = allKeymaps[whichIndex];
            }
        }
        return selectedKeymap;
    }
    
    private String[] getKeymaps(File baseDir) {
        Set<String> keymaps = new HashSet<String>();
        Stack<File> fileStack = new Stack<File>();
        fileStack.addAll(Arrays.asList(baseDir.listFiles()));
        Set<File> visitedDirs = new HashSet<File>();

        File f;
        while(! fileStack.isEmpty()) {
            f = fileStack.pop();
            if (f.isDirectory() && ! visitedDirs.contains(f)) {
                visitedDirs.add(f);
                if (f.exists() && f.canRead()) {
                    fileStack.addAll(Arrays.asList(f.listFiles()));
                }
            } else if (f.isFile() && (f.getName().endsWith(".map") || f.getName().endsWith(".map.gz"))) {
                keymaps.add(f.getName().split("\\.")[0]);
            }
        }

        String[] keymapStrings = keymaps.toArray(new String[keymaps.size()]);
        Arrays.sort(keymapStrings);
        return keymapStrings;
    }

    private int showKeymaps(String[] keymaps, File baseDir, int startingIndex, List<String> allowedEntries, boolean hasMoreEntries) throws IOException, WizardNavigationException {
        List<String> prompts = new ArrayList<String>();
        prompts.add("Select a keymap from the following list " + EOL);

        if (baseDir != null && baseDir.isDirectory())
            prompts.add("[" + baseDir.getName() + "]" + EOL);

        int x = startingIndex;
        for (String keymap : keymaps) {
            String indexStr = String.valueOf(x);
            x++;
            String prompt = indexStr + ") " + keymap;
            prompts.add(prompt + EOL);
        }

        String[] acceptedEntries;
        String defaultValue = "";
        if (!hasMoreEntries) {
            acceptedEntries = allowedEntries.toArray(new String[allowedEntries.size()]);
            prompts.add("Please make a selection : ");
        } else {
            List<String> tempList = new ArrayList<String>();
            tempList.addAll(allowedEntries);
            tempList.add(defaultValue);
            acceptedEntries = tempList.toArray(new String[tempList.size()]);
            prompts.add("Please make a selection (press [Enter] for next page): ");
        }

        String tzSelection = getData(prompts.toArray(new String[prompts.size()]), defaultValue, acceptedEntries,null);
        if (tzSelection.equals(defaultValue)) {
            return -1;
        }
        int whichChoice = Integer.parseInt(tzSelection);
        return whichChoice - 1;
    }
}
