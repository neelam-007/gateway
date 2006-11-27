package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionManager;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 25, 2006
 * Time: 11:07:41 PM
 */
public class ConfigWizardConsolePartitioningStep extends BaseConsoleStep{
    private static final Logger logger = Logger.getLogger(ConfigWizardConsolePartitioningStep.class.getName());

    private static final String TITLE = "Configure Partitions";
    private static final String STEP_INFO = "This step lets you create or set up a connection to the SSG database";
    private static final String HEADER_SELECT_PARTITION = "-- Select The Partition To Configure --" + getEolChar();

    public static final String ADD_NEW_PARTITION = ") Add a new Partition: " + getEolChar();
    public static final String DELETE_PARTITION = ") Delete a Partition: " + getEolChar();
    private Set<String> partitionNames;


    public ConfigWizardConsolePartitioningStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        init();
    }

    private void init() {
        partitionNames = PartitionManager.getInstance().getPartitionNames();
    }

    //each step must implement these.
    public boolean validateStep() {
        return true;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + getEolChar());

        try {
            doSelectPartitionPrompts();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        storeInput();
    }

    private void doSelectPartitionPrompts() throws IOException, WizardNavigationException {

        String[] prompts = new String[partitionNames.size() + 4];
        prompts[0] = HEADER_SELECT_PARTITION;
        int index = 1;
        for (String partitionName : partitionNames) {
            prompts[index] = String.valueOf(index++) + ") " +partitionName + getEolChar();
        }

        prompts[index] = String.valueOf(index++) + ADD_NEW_PARTITION;
        prompts[index] = String.valueOf(index++) + DELETE_PARTITION;
        prompts[index] = "Please make a selection: [1] ";

        List<String> allowedEntries = new ArrayList<String>();

        for (int i = 0; i < index; i++) {
            allowedEntries.add(String.valueOf(i));
        }

        String input = getData(prompts, "1", allowedEntries.toArray(new String[0]));

    }

    public String getTitle() {
        return TITLE;
    }
}
