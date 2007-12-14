package com.l7tech.server.config.ui.console;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PartitionActionListener;
import com.l7tech.server.config.PartitionActions;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.config.commands.PartitionConfigCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * User: megery
 * Date: Nov 25, 2006
 * Time: 11:07:41 PM
 */
public class ConfigWizardConsolePartitioningStep extends BaseConsoleStep implements PartitionActionListener {
    private static final Logger logger = Logger.getLogger(ConfigWizardConsolePartitioningStep.class.getName());

    private static final String TITLE = "Select A Partition";
    private static final String STEP_INFO = "This step lets you select or manage SecureSpan Gateway partitions";
    private static final String HEADER_SELECT_PARTITION = "-- Select The Partition To Configure --" + getEolChar();
    private static final String HEADER_ADD_PARTITION = "-- Add A New Partition --" + getEolChar();
    private static final String HEADER_DELETE_PARTITION = "-- Select a Partition To Delete (the default_ partition cannot be deleted)--" + getEolChar();

    private static final String SELECT_EXISTING_PARTITION = ") Select an Existing Partition" + getEolChar();
    private static final String ADD_NEW_PARTITION = ") Add a new Partition: " + getEolChar();
    private  static final String DELETE_PARTITION = ") Delete a Partition: " + getEolChar();
    private Set<String> partitionNames;

    private int newPartitionIndex = 0;



    public ConfigWizardConsolePartitioningStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        init();
    }

    private void init() {
        configBean = new PartitionConfigBean();
        configCommand = new PartitionConfigCommand(configBean);
        partitionNames = PartitionManager.getInstance().getPartitionNames();
    }

    //each step must implement these.
    public boolean validateStep() {
        return true;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + getEolChar());

        try {
            PartitionInformation pinfo = doPartitionActionPrompts();
            pinfo = doEditPartitionPrompts(pinfo, !pinfo.isNewPartition() && !StringUtils.equals(pinfo.getPartitionId(), PartitionInformation.DEFAULT_PARTITION_NAME));
            getParentWizard().setPartitionName(pinfo);
            ((PartitionConfigBean)configBean).setPartitionInfo(pinfo);
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        storeInput();
    }

    private PartitionInformation doEditPartitionPrompts(final PartitionInformation pinfo,  boolean doNamePrompts) throws IOException, WizardNavigationException {

        String existingName;
        String newPartitionName;

        PartitionInformation newPartitionInfo = pinfo;
        if (doNamePrompts) {
            existingName = newPartitionInfo.getPartitionId();
            newPartitionName = getData(
                new String[] {"To rename this partition, type a new name here or press Enter to keep the existing name: [" + existingName + "]"},
                existingName,
                Pattern.compile(PartitionInformation.ALLOWED_PARTITION_NAME_PATTERN),
                null
            );

            if (!StringUtils.equals(existingName, newPartitionName)) {
                try {

                    if (PartitionActions.renamePartition(newPartitionInfo, newPartitionName, this)) {
                        partitionNames = PartitionManager.getInstance().getPartitionNames();
                        newPartitionInfo = PartitionManager.getInstance().getPartition(newPartitionName);
                        getData(
                            new String[]{
                                getEolChar(),
                                "The partition has been renamed." + getEolChar(),
                                "Please ensure that you complete the configuration of this partition using this wizard or the partition will not start correctly." + getEolChar(),
                                "Press Enter to continue" + getEolChar(),
                                getEolChar(),
                                },
                            "",
                            (String[]) null,
                            null
                        );
                    } else {
                        printText("The partition was not renamed" + getEolChar());
                    }
                } catch (IOException e) {
                    printText("*** Could not rename the \"" + existingName  + "\" partition to \"" + newPartitionName + "\" ***" + getEolChar());
                    printText("*** " + e.getMessage() + " ***" + getEolChar());
                    logger.warning("Could not rename the \"" + existingName  + "\" partition to \"" + newPartitionName + "\" [" + e.getMessage() + "]");
                }
            }
        }
        return newPartitionInfo;
    }

    private PartitionInformation doPartitionActionPrompts() throws IOException, WizardNavigationException {
        boolean showAgain;

        do {
            int index = 1;

            String[] prompts = new String[] {
                String.valueOf(index++) + SELECT_EXISTING_PARTITION,
                String.valueOf(index++) + ADD_NEW_PARTITION,
                String.valueOf(index++) + DELETE_PARTITION,
                "Please make a selection: [1] ",
            };

            String[] allowedValues = new String[index-1];
            for (int i = 0; i < allowedValues.length; i++) {
                allowedValues[i] = String.valueOf(i+1);
            }

            String whichAction = getData(prompts, "1", allowedValues,null);

            int choice = Integer.parseInt(whichAction);
            switch(choice) {
                case 1:
                    return doSelectPartitionPrompts();
                case 2:
                    PartitionInformation pi = doAddPartitionPrompts();
                    return (pi == null?doPartitionActionPrompts():pi);
                case 3:
                    //we want to delete a partition, but not go to the next step yet, so we'll loop here
                    doDeletePartitionPrompts();
                    showAgain = true;
                    break;
                default:
                    return doSelectPartitionPrompts();
            }
        } while(showAgain);
        return null;
    }

    private PartitionInformation doAddPartitionPrompts() throws IOException, WizardNavigationException {
        PartitionInformation pi = null;
        if (partitionNames.size() >= PartitionInformation.MAX_PARTITIONS) {
            String message = MessageFormat.format("*** A maximum of {0} partitions is supported ***" + getEolChar(), PartitionInformation.MAX_PARTITIONS);
            getData(
                    new String[] {
                        getEolChar(),
                        message,
                        "Press enter to continue",
                        getEolChar(),
                    },
                    "",
                    (String[]) null,
                    null);

        } else {
            List<String> prompts = new ArrayList<String>();
            prompts.add(HEADER_ADD_PARTITION);
            String defaultValue = getNextNewName();
            prompts.add("Enter the name of the new partition: ["+ defaultValue + "]");

            Pattern allowedEntries = Pattern.compile(PartitionInformation.ALLOWED_PARTITION_NAME_PATTERN);

            String newPartitionName;
            do {
                newPartitionName = getData(prompts.toArray(new String[prompts.size()]), defaultValue, allowedEntries, "*** Invalid Partition Name. Please re-enter ***");
                if (partitionNames.contains(newPartitionName))
                    printText(new String[] {
                        "*** " + newPartitionName + " already exists. Please choose another name ***" + getEolChar(),
                        ""
                    });
            } while (partitionNames.contains(newPartitionName));

            if (StringUtils.isNotEmpty(newPartitionName)) {
                pi = new PartitionInformation(newPartitionName);
                PartitionActions pa = new PartitionActions(osFunctions);
                try {
                    pa.createNewPartition(pi.getPartitionId());
                    PartitionActions.prepareNewpartition(pi);
                } catch (SAXException e) {
                    logger.warning("Error while trying to prepare the newly added partition. Please finish the configuration for this partition to ensure that it works properly. [" + e.getMessage() + "]");
                } catch (InterruptedException e) {
                    logger.warning("Error while trying to prepare the newly added partition. Please finish the configuration for this partition to ensure that it works properly. [" + e.getMessage() + "]");
                }
                PartitionManager.getInstance().addPartition(pi);
                partitionNames = PartitionManager.getInstance().getPartitionNames();
                pi = PartitionManager.getInstance().getPartition(newPartitionName);
            }
        }

        return pi;
    }

    private String getNextNewName() {
        String newName;
        do {
            newName = "NewPartition" + (newPartitionIndex == 0?"":String.valueOf(newPartitionIndex));
            newPartitionIndex++;
        } while (partitionNames.contains(newName));
        return newName;
    }

    private void doDeletePartitionPrompts() throws IOException, WizardNavigationException {
        List<String> prompts = new ArrayList<String>();
        prompts.add(HEADER_DELETE_PARTITION);
        int index = 1;
        List<String> nameList = new ArrayList<String>(partitionNames);
        nameList.remove(PartitionInformation.DEFAULT_PARTITION_NAME);
        if (nameList.isEmpty()) {
            printText("\nThere are no partitions other than the default_ partition, which cannot be deleted.\n");
            printText("Please select another option.\n\n");

        } else {

            for (String partitionName : nameList) {
                prompts.add(String.valueOf(index++) + ") " + partitionName + getEolChar());
            }

            String defaultValue = "1";
            prompts.add("Please make a selection: [" + defaultValue + "] ");

            String[] allowedEntries = new String[index -1];
            for (int i = 0; i < allowedEntries.length; i++) {
                allowedEntries[i]= String.valueOf(i + 1);
            }

            String whichPartition = getData(prompts.toArray(new String[prompts.size()]), defaultValue, allowedEntries,null);
            int whichIndex = Integer.parseInt(whichPartition) -1;

            String whichPartitionName = nameList.get(whichIndex);

            PartitionInformation partitionToRemove = PartitionManager.getInstance().getPartition(whichPartitionName);
            boolean isUnix = partitionToRemove.getOSSpecificFunctions().isUnix();
            String warningMessage;
            if (isUnix) {
                warningMessage = "Removing the \"" + partitionToRemove.getPartitionId() + "\" partition will remove all the associated configuration.\n\n" +
                    "This cannot be undone.\n" +
                    "Do you wish to proceed?";
            } else {
                warningMessage = "Removing the \"" + partitionToRemove.getPartitionId() + "\" partition will stop the service and remove all the associated configuration.\n\n" +
                    "This cannot be undone.\n" +
                    "Do you wish to proceed?";
            }
            boolean confirmed = getConfirmationFromUser(warningMessage, "n");
            if (confirmed) {
                if (PartitionActions.removePartition(partitionToRemove, this)) {
                    PartitionManager.getInstance().removePartition(whichPartitionName);
                    partitionNames = PartitionManager.getInstance().getPartitionNames();
                    printText(getEolChar() + "The selected partition has been deleted. You may continue to use the wizard to configure other partitions or exit now." + getEolChar());
                }
            }
        }
    }

    private String getRunMarker(String partitionName) {
        try {
            return osFunctions.isPartitionRunning(partitionName) ? "! " : "  ";
        } catch (OSSpecificFunctions.OsSpecificFunctionUnavailableException e) {
            return "  ";
        } catch (IOException e) {
            printText("*** Unable to check if partition is running: " + partitionName + ": " + ExceptionUtils.getMessage(e) + getEolChar());
            return "? ";
        }
    }

    private PartitionInformation doSelectPartitionPrompts() throws IOException, WizardNavigationException {
        for (; ;) {
            List<String> promptList = new ArrayList<String>();

            promptList.add(HEADER_SELECT_PARTITION);
            String defaultValue = "1";
            int index = 1;

            List<String> partitions = new ArrayList<String>(partitionNames);

            for (String partitionName : partitions) {
                String runMarker = getRunMarker(partitionName);
                promptList.add(String.valueOf(index++) + ") " + runMarker + partitionName + getEolChar());
            }

            promptList.add("Please make a selection: [" + defaultValue + "]");

            List<String> allowedEntries = new ArrayList<String>();

            for (int i = 1; i < index; i++) {
                allowedEntries.add(String.valueOf(i));
            }

            String input = getData(
                    promptList.toArray(new String[promptList.size()]),
                    defaultValue,
                    allowedEntries.toArray(new String[allowedEntries.size()]),
                    null);

            PartitionInformation pinfo = null;
            if (input != null) {
                String whichPartition = partitions.get(Integer.parseInt(input) - 1);
                pinfo = PartitionManager.getInstance().getPartition(whichPartition);
                try {
                    if (osFunctions.isPartitionRunning(whichPartition)) {
                        printText(getEolChar() + "This partition is currently running and cannot be reconfigured at this time." + getEolChar());
                        continue;
                    }
                } catch (OSSpecificFunctions.OsSpecificFunctionUnavailableException e) {
                    printText(getEolChar() + "Please ensure that this partition is stopped." + getEolChar());
                    readLine();
                }
            }
            return pinfo;
        }
    }

    public String getTitle() {
        return TITLE;
    }

    public boolean getPartitionActionsConfirmation(String message) {
        boolean ok = false;
        try {
            ok = getConfirmationFromUser(message, "n");
        } catch (IOException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        } catch (WizardNavigationException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");                                                                              
        }

        return ok;
    }

    public void showPartitionActionErrorMessage(String message) {
        List<String> messages = new ArrayList<String>();
        messages.add("**** " + message + " ****");
        messages.add("Press Enter to continue");
        try {
            getData(messages.toArray(new String[messages.size()]),"", (String[]) null,null);
        } catch (IOException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");            
        } catch (WizardNavigationException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        }
    }


    public void showPartitionActionMessage(String message) {
        List<String> messages = new ArrayList<String>();
        messages.add(message);
        messages.add("Press Enter to continue");
        try {
            getData(messages.toArray(new String[messages.size()]),"", (String[]) null,null);
        } catch (IOException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        } catch (WizardNavigationException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        }
    }
}