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
    private static final String TITLE = "SSG Clustering Setup";

    public ConfigWizardConsoleClusteringStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        init();
    }

    private void init() {
        configBean = new ClusteringConfigBean();
        clusterBean = (ClusteringConfigBean) configBean;
        configCommand = new ClusteringConfigCommand(configBean);
        showNavigation = false;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {

        String defaultClusterHostname = getDefaultHostName();

        if (!validated) {
            printText("*** There were errors in your input, please try again. ***" + getEolChar());
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
        printText(new String[]{});

        String input = getData(new String[] {
            "-- Specify Hostname -- " + getEolChar(),
            "The hostname will be used to identify the SSG and generate its certificates" + getEolChar(),
            "What hostname would you like to use? [" + defaultClusterHostname + "] : ",
        }, defaultClusterHostname);

        clusterBean.setClusterHostname(input.trim());

        getParentWizard().setHostname(input.trim());
    }

    private void doClusterTypePrompt(boolean invalidInput) throws IOException, WizardNavigationException {

        if (invalidInput) {
            printText("Please select one of the options shown" + getEolChar());
        }

        List<String> clusterTypePrompts = new ArrayList<String>();

        clusterTypePrompts.add("-- Creating or joining a cluster --" + getEolChar());

        Map<String, ClusteringConfigBean.ClusterTypePair> clusterPromptMapper = new TreeMap<String, ClusteringConfigBean.ClusterTypePair>();

        //get clustering type preference
        int i = 1;
        for (ClusteringConfigBean.ClusterTypePair clusterTypePair : ClusteringConfigBean.clusterTypes) {
            if (clusterTypePair != null) {
                clusterPromptMapper.put(String.valueOf(i++), clusterTypePair);
            }
        }

        Set<String> keys = clusterPromptMapper.keySet();
        for (String key : keys) {
            ClusteringConfigBean.ClusterTypePair ctp = clusterPromptMapper.get(key);
            clusterTypePrompts.add(key + ") " + ctp.getClusterTypeDescription() + getEolChar());
        }

        clusterTypePrompts.add("Please make a selection: [1]");

        String input = getData(
                clusterTypePrompts,
                "1",
                clusterPromptMapper.keySet().toArray(new String[]{}));

        int clusterType = clusterPromptMapper.get(input).getClusterType();

        clusterBean.setDoClusterType(clusterType);
        getParentWizard().setClusteringType(clusterBean.getClusterType());
    }

    public String getTitle() {
        return TITLE;
    }

    public boolean validateStep() {
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
