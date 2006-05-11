package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:57 AM
 */
public class ConfigWizardConsoleResultsStep extends BaseConsoleStep{
    public ConfigWizardConsoleResultsStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
        showNavigation = false;
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        ConfigurationWizard wizard = getParentWizard();

        if (wizard.isHadFailures()) {
            printText("There were errors during configuration, see below for details\n");
        } else {
            printText("The configuration was successfully applied.\n");
            printText("You must restart the SSG in order for the configuration to take effect.\n");
        }

        if (needsManualSteps(wizard.getClusteringType(), wizard.getKeystoreType())) {
            printText("\n**** Some manual steps are required to complete the configuration of the SSG ****\n");
            printText("\tThese manual steps have been saved to a the file: ssg_config_manual_steps.txt\n");
            saveManualSteps(wizard.getClusteringType(), wizard.getKeystoreType());
        }

        printText("\nThe following is a summary of the actions taken by the wizard\n");
        printText("\tThese logs have been saved to the file: ssgconfig0.log\n");

        printText("\n");

        List logs = ListHandler.getLogList();
        if (logs != null) {
            for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                if (s!= null) {
                    printText(s + "\n");
                }
            }
        }

        printText("Press <Enter> to finish the wizard\n");

        try {
            handleInput(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean needsManualSteps(int clusteringType, String keystoreType) {
        return (clusteringType != ClusteringConfigBean.CLUSTER_NONE || keystoreType.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME));
    }

    private boolean saveManualSteps(int clusteringType, String keystoreType) {
        boolean success = true;
        String eol = osFunctions.isWindows()?"\r\n":"\n";
        boolean lunaMentioned = false;
        StringBuilder stepsBuffer = new StringBuilder();

        String infoLine = "The following manual steps are required to complete the configuration of the SSG" + eol;
        String linuxLunaConfigCopy =    eol +
                                            "\tLUNA CONFIGURATION: Copy the etc/Chrystoki.conf file from the primary node to each SSG in the cluster" + eol +
                                            eol;

        String windowsLunaConfigCopy =  eol +
                                            "\tLUNA CONFIGURATION: Copy the LUNA_INSTALL_DIR/crystoki.ini file from the primary node to each SSG in the cluster" +
                                            eol;

        String windowsLunaString =  eol +
                                        "\t[Misc]" + eol +
                                            "\t\tApplicationInstance=HTTP_SERVER<br>" + eol +
                                            "\t\tAppIdMajor=1" + eol +
                                            "\t\tAppIdMinor=1" + eol +
                                            eol +
                                        "\twhere AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

        String windowsUpdateCrystokiLine =  "\tLUNA CONFIGURATION: Append the following to the LUNA_INSTALL_DIR/crystoki.ini file:" + eol +
                                                "\t\t" + windowsLunaString + eol +
                                            eol;

        String linuxLunaString =    eol +
                                        "\tMisc = {" + eol +
                                            "\t\tApplicationInstance=HTTP_SERVER;" + eol +
                                            "\t\tAppIdMajor=1;" + eol +
                                            "\t\tAppIdMinor=1;" + eol +
                                        "\t}" + eol +
                                    eol +
                                    "\twhere AppIdMajor and AppIdMinor correspond to your Luna configuration" + eol;

        String linuxUpdateCrystokiLine =    "\tLUNA CONFIGURATION: Append the following to the etc/Chrystoki.conf file:" + eol +
                                                 "\t\t" + linuxLunaString + eol +
                                            eol;

        String updateHostsFileLine =    "\tUPDATE HOSTS FILE: add a line which contains the IP address for this SSG node, then the " + eol +
                                        "\tcluster host name, then this SSG node's hostname" + eol +
                                        eol +
                                            "\t\tex:" + eol +
                                                "\t\t\t192.168.1.186      ssgcluster.domain.com ssgnode1.domain.com" + eol +
                                        eol;

        String timeSyncLine =   eol +
                                    "\tTIME SYNCHRONIZATION: Please ensure time is synchronized among all SSG nodes " + eol +
                                    "\twithin the cluster" + eol +
                                eol;

        String runSSgConfigLine =   "\tRUN THE SSG CONFIGURATION WIZARD: run the wizard on each of the " + eol +
                                    "\tmembers of the cluster to generate the keystores" + eol +
                                    eol +
                                        "\t\tNote:" + eol +
                                            "\t\t\tUse the same password for the keystore on each of the members of the cluster" + eol +
                                    eol;

        String copykeysLine =   "\tCOPY THE KEYS: copy the contents of the keystore directory on the first node" + eol +
                                "\tof the cluster to the keystore directory on the other SSGs in the cluster" + eol +
                                eol +
                                    "\t\tNote:" + eol +
                                        "\t\t\tThe SSG keystore directory is: \"" + osFunctions.getKeystoreDir() + "\"" + eol +
                                eol;

        if (clusteringType != ClusteringConfigBean.CLUSTER_NONE || keystoreType.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
            stepsBuffer.append(infoLine);

            if (clusteringType != ClusteringConfigBean.CLUSTER_NONE) {
                stepsBuffer.append(eol).append(
                                    updateHostsFileLine).append(
                                    timeSyncLine);


                if (clusteringType == ClusteringConfigBean.CLUSTER_JOIN) {

                    if (keystoreType == KeyStoreConstants.LUNA_KEYSTORE_NAME) {
                        lunaMentioned = true;
                        if (osFunctions.isLinux()) {
                            stepsBuffer.append(linuxLunaConfigCopy);
                        } else {
                            stepsBuffer.append(windowsLunaConfigCopy);
                        }
                    }
                    else {
                        stepsBuffer.append(runSSgConfigLine).append(
                            copykeysLine);
                    }
                }

                if (clusteringType == ClusteringConfigBean.CLUSTER_NEW) {
                    if (keystoreType.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
                        lunaMentioned = true;
                        //instructions for luna in a clustered environment
                        if (osFunctions.isLinux()) {
                            stepsBuffer.append(linuxUpdateCrystokiLine);
                        } else {
                            stepsBuffer.append(windowsUpdateCrystokiLine);
                        }
                    }
                }
                stepsBuffer.append(eol);
            } else {
                stepsBuffer.append(eol);
                if (keystoreType == KeyStoreConstants.LUNA_KEYSTORE_NAME && !lunaMentioned) {
                    if (osFunctions.isLinux()) {
                        stepsBuffer.append(linuxUpdateCrystokiLine);
                    } else {
                        stepsBuffer.append(windowsUpdateCrystokiLine);
                    }
                    stepsBuffer.append(eol);
                }
            }
            stepsBuffer.append(eol);
            stepsBuffer.append(eol);
            File manualStepsFile = new File("ssg_config_manual_steps.txt");
            try {
                PrintWriter saveWriter = new PrintWriter(new FileOutputStream(manualStepsFile));
                saveWriter.print(stepsBuffer.toString());
                saveWriter.close();
            } catch (FileNotFoundException e) {
                printText("Could not create file: " + manualStepsFile.getName() + "\n");
                printText(e.getMessage() + "\n");
                success = false;
            }
        }
        return success;
    }

    public String getTitle() {
        return "SSG Configuration Results";
    }

    boolean validateStep() {
        return true;
    }
}
