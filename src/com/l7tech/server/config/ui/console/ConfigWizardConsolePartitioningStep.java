package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.PartitionActions;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.config.commands.PartitionConfigCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
    private static final String HEADER_ADD_PARTITION = "-- Add A New Partition --" + getEolChar();
    private static final String HEADER_DELETE_PARTITION = "-- Select a Partition To Delete --" + getEolChar();

    private static final String HEADER_CONFIGURE_ENDPOINTS= "-- Select an Endpoint to configure for the \"{0}\" Partition --" + getEolChar();
    private static final String SELECT_EXISTING_PARTITION = ") Select an Existing Partition" + getEolChar();
    public static final String ADD_NEW_PARTITION = ") Add a new Partition: " + getEolChar();
    public static final String DELETE_PARTITION = ") Delete a Partition: " + getEolChar();
    private Set<String> partitionNames;
    private String pathSeparator = File.separator;


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
            doConfigureEndpointsPrompts(pinfo);
            getParentWizard().setPartitionName(pinfo);
            ((PartitionConfigBean)configBean).setPartition(pinfo);
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        storeInput();
    }

    private PartitionInformation doPartitionActionPrompts() throws IOException, WizardNavigationException {
        boolean showAgain = false;

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

            String whichAction = getData(prompts, "1", allowedValues);

            int choice = Integer.parseInt(whichAction);
            switch(choice) {
                case 1:
                    return doSelectPartitionPrompts();
                case 2:
                    return doAddPartitionPrompts();
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
        List<String> prompts = new ArrayList<String>();
        prompts.add(HEADER_ADD_PARTITION);
        String defaultValue = "New Partition";
        prompts.add("Enter the name of the new partition: ["+ defaultValue + "]");
            
        Pattern allowedEntries = Pattern.compile("[^" + pathSeparator +"]{1,128}");
        String newPartitionName = getData(prompts.toArray(new String[0]), "New Partition", allowedEntries, "*** Invalid Partition Name. Please re-enter ***");

        PartitionInformation pi = null;
        if (StringUtils.isNotEmpty(newPartitionName)) {
            PartitionActions pa = new PartitionActions(osFunctions);
            File newPartDir = pa.createNewPartition(osFunctions.getPartitionBase() + newPartitionName);
            pa.copyTemplateFiles(newPartDir);
            try {
                pa.setLinuxFilePermissions(
                    new String[] {
                        newPartDir.getAbsolutePath() + "/partitionControl.sh",
                        newPartDir.getAbsolutePath() + "/partition_defs.sh",
                    },
                    "775",
                    newPartDir, osFunctions);
            } catch (InterruptedException e) {
                logger.warning("Error while setting execute permissions on the startup scripts for partition \"" + pi.getPartitionId() + "\": " + e.getMessage());
            }

            PartitionManager.getInstance().addPartition(newPartitionName);
            partitionNames = PartitionManager.getInstance().getPartitionNames();
            pi = PartitionManager.getInstance().getPartition(newPartitionName);
        }

        return pi;
    }

    private void doDeletePartitionPrompts() throws IOException, WizardNavigationException {
        List<String> prompts = new ArrayList<String>();
        prompts.add(HEADER_DELETE_PARTITION);
        int index = 1;
        List<String> nameList = new ArrayList<String>(partitionNames);

        for (String partitionName : nameList) {
            prompts.add(String.valueOf(index++) + ") " + partitionName + getEolChar());
        }

        String defaultValue = "1";
        prompts.add("Please make a selection: [" + defaultValue + "] ");

        String[] allowedEntries = new String[index -1];
        for (int i = 0; i < allowedEntries.length; i++) {
            allowedEntries[i]= String.valueOf(i + 1);
        }

        String whichPartition = getData(prompts, defaultValue, allowedEntries);
        int whichIndex = Integer.parseInt(whichPartition) -1;

        String whichPartitionName = nameList.get(whichIndex);

        boolean confirmed = getConfirmationFromUser("Are you sure you want to delete the \"" + whichPartitionName + "\" partition? This cannot be undone.");
        if (confirmed) {
            PartitionActions pa = new PartitionActions(osFunctions);
            pa.removePartition(PartitionManager.getInstance().getPartition(whichPartitionName));
            PartitionManager.getInstance().removePartition(whichPartitionName);
            partitionNames = PartitionManager.getInstance().getPartitionNames();
        }
    }

    private PartitionInformation doSelectPartitionPrompts() throws IOException, WizardNavigationException {

        List<String> promptList =  new ArrayList<String>();

        promptList.add(HEADER_SELECT_PARTITION);
        String defaultValue = "1";
        int index = 1;

        List<String> partitions = new ArrayList<String>(partitionNames);

        for (String partitionName : partitions) {
            promptList.add(String.valueOf(index++) + ") " + partitionName + getEolChar());
        }

        promptList.add("Please make a selection: [" + defaultValue + "]");

        List<String> allowedEntries = new ArrayList<String>();

        for (int i = 0; i < index; i++) {
            allowedEntries.add(String.valueOf(i));
        }

        String input = getData(promptList, defaultValue, allowedEntries.toArray(new String[0]));
        PartitionInformation pinfo = null;
        if (input != null) {
            String whichPartition = partitions.get(Integer.parseInt(input) -1);
            pinfo  = PartitionManager.getInstance().getPartition(whichPartition);
        }
        return pinfo;
    }

    private void doConfigureEndpointsPrompts(PartitionInformation pinfo) throws IOException, WizardNavigationException {

        List<PartitionInformation.EndpointHolder> holders = new ArrayList<PartitionInformation.EndpointHolder>();
        holders.addAll(pinfo.getHttpEndpoints());
        holders.addAll(pinfo.getOtherEndpoints());

        List<String> promptList = new ArrayList<String>();
        boolean finishedEndpointConfig = true;
        do {
            promptList.clear();
            promptList.add(MessageFormat.format(HEADER_CONFIGURE_ENDPOINTS, pinfo.getPartitionId()));
            int index = 1;
            for (PartitionInformation.EndpointHolder holder : holders) {
                promptList.add(String.valueOf(index++) + ") " + holder.toString() + getEolChar());
            }
            promptList.add(String.valueOf(index) + ") Finished Configuring Endpoints" + getEolChar());

            String defaultValue = String.valueOf(index);

            promptList.add("Please make a selection: [" + defaultValue + "]");

            String[] allowedEntries = new String[index];
            for (int i = 0; i < allowedEntries.length; i++) {
                allowedEntries[i] = String.valueOf(i + 1);
            }

            String whichEndpointSelection = getData(promptList, defaultValue, allowedEntries);
            int whichEndpointIndex = Integer.parseInt(whichEndpointSelection);

            if (whichEndpointIndex == index) {
                finishedEndpointConfig = true;
            } else {
                PartitionInformation.EndpointHolder holder = holders.get(whichEndpointIndex -1);
                doCollectEndpointInfo(holder);
                finishedEndpointConfig = false;
            }

        } while (!finishedEndpointConfig);
    }

    private void doCollectEndpointInfo(PartitionInformation.EndpointHolder holder) throws IOException, WizardNavigationException {
        String input = null;
        List<String> prompts = null;
        Pattern portPattern = Pattern.compile("\\d{1,5}+");
        if (holder instanceof PartitionInformation.HttpEndpointHolder) {
            PartitionInformation.HttpEndpointHolder httpHolder = (PartitionInformation.HttpEndpointHolder) holder;
            String[] availableIpAddress = PartitionActions.getAvailableIpAddresses().toArray(new String[0]);

            List<String> ipAddressOptions = new ArrayList<String>();
            int index = 1;
            for (String availableIpAddres : availableIpAddress) {
                ipAddressOptions.add(String.valueOf(index++) + ") " + availableIpAddres + getEolChar());
            }

            String[] allowedEntries = new String[availableIpAddress.length];
            for (int i = 0; i < allowedEntries.length; i++) {
                allowedEntries[i] = String.valueOf(i + 1);
            }

            prompts = new ArrayList<String>();
            prompts.add("Please enter the IP Address for the \"" + httpHolder.endpointType + "\" endpoint" + getEolChar());
            prompts.addAll(ipAddressOptions);
            prompts.add("Please make a selection: [1]");
            input = getData(prompts, "1", allowedEntries);
            int ipIndex = Integer.parseInt(input) - 1;
            httpHolder.ipAddress = availableIpAddress[ipIndex];

            prompts = new ArrayList<String>();
            prompts.add("Please enter the port for the \"" + httpHolder.endpointType + "\" endpoint: [" + httpHolder.port + "]");
            input = getData(prompts.toArray(new String[0]), httpHolder.port, portPattern, "The port you have entered is invalid. Please re-enter");

            httpHolder.port = input;
            
        } else if (holder instanceof PartitionInformation.OtherEndpointHolder) {
            PartitionInformation.OtherEndpointHolder otherHolder = (PartitionInformation.OtherEndpointHolder) holder;

            input = getData(new String[] {
                "Please enter the port for the \"" + otherHolder.endpointType + "\" endpoint: [" + otherHolder.port + "] ",
            }, otherHolder.port, portPattern);
            otherHolder.port = input;
        }
    }

    public String getTitle() {
        return TITLE;
    }
}
