package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ClusteringConfigCommand;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 9:58:53 AM
 */
public class ConfigWizardConsoleClusteringStep extends BaseConsoleStep{
    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleClusteringStep.class.getName());

    ClusteringConfigBean clusterBean;

    public ConfigWizardConsoleClusteringStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
        init();
    }

    private void init() {
        configBean = new ClusteringConfigBean(osFunctions);
        clusterBean = (ClusteringConfigBean) configBean;
        configCommand = new ClusteringConfigCommand(configBean);
        showNavigation = false;
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {

        String defaultClusterHostname = getDefaultHostName();

        if (!validated) {
            printText("*** There were errors in your input, please try again. ***\n");
        }

        try {
            doHostnamePrompt(defaultClusterHostname);
            printText("\n");
            doClusterTypePrompt(false);
            storeInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDefaultHostName() {
        if (StringUtils.isEmpty(clusterBean.getClusterHostname())) {
            clusterBean.setClusterHostname(clusterBean.getLocalHostName());
        }

        return clusterBean.getClusterHostname();
    }

    private void doHostnamePrompt(String defaultClusterHostname) throws IOException, WizardNavigationException {
        //get cluster host name
        String input = getData(new String[] {
            "-- Specify Hostname -- \n",
            "The hostname will be used to identify the SSG and generate its certificates\n",
            "What hostname would you like to use? [" + defaultClusterHostname + "] : ",
        }, defaultClusterHostname, true);

        clusterBean.setClusterHostname(input.trim());
    }

    private void doClusterTypePrompt(boolean invalidInput) throws IOException, WizardNavigationException {

        if (invalidInput) {
            printText("Please select one of the options shown\n");
        }

        //get clustering type preference
        Set entries = ClusteringConfigBean.clusterTypes.entrySet();
        Map.Entry[] clusterPromptMapper = new Map.Entry[entries.size()];

        Iterator iter = entries.iterator();
        ArrayList clusterTypePrompts = new ArrayList();

        clusterTypePrompts.add("-- Creating or joining a cluster --\n");
        int i = 0;
        while (iter.hasNext() && i < clusterPromptMapper.length) {
            Map.Entry entry = (Map.Entry) iter.next();
            clusterPromptMapper[i] = entry;
            clusterTypePrompts.add(new String(i+1 + ") " + (String) entry.getKey() + "\n"));
            i++;
        }
        clusterTypePrompts.add("Please make a selection: [1]");


        String input = getData((String[])clusterTypePrompts.toArray(new String[clusterTypePrompts.size()]), "1", true);
        int clusterType = ClusteringConfigBean.CLUSTER_NONE;
        try {
            int selectedIndex = Integer.parseInt(input) - 1;
            if (selectedIndex >= 0 && selectedIndex < clusterPromptMapper.length) {
                clusterType = ((Integer)clusterPromptMapper[selectedIndex].getValue()).intValue();
            } else {
                doClusterTypePrompt(true);
            }
        } catch (NumberFormatException e) {
            doClusterTypePrompt(true);
        }
        clusterBean.setDoClusterType(clusterType);
    }

    public String getTitle() {
        return "SSG Clustering Setup";
    }

    protected boolean validateStep() {
        boolean validatedHostname = false;
        String clusterHostName = clusterBean.getClusterHostname();
        if (StringUtils.isNotEmpty(clusterHostName)) {
            validatedHostname = true;
        }

        int clusterType = clusterBean.getClusterType();
        boolean validatedType = false;
        switch (clusterType) {
            case ClusteringConfigBean.CLUSTER_NONE:
            case ClusteringConfigBean.CLUSTER_NEW:
            case ClusteringConfigBean.CLUSTER_JOIN:
                validatedType = true;
                break;
            default:
                validatedType = false;
        }

        return validatedHostname && validatedType;
    }
}
