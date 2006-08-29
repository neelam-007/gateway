package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.ClusteringType;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.commands.ClusteringConfigCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

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

        Map<String, ClusteringType> clusterPromptMapper = new TreeMap<String, ClusteringType>();

        //get clustering type preference
        int i = 1;
        for (ClusteringType clusterType : ClusteringType.values()) {
            clusterPromptMapper.put(String.valueOf(i++), clusterType);
        }

        Set<String> keys = clusterPromptMapper.keySet();
        for (String key : keys) {
            ClusteringType ctype = clusterPromptMapper.get(key);
            clusterTypePrompts.add(key + ") " + ctype + getEolChar());
        }

        clusterTypePrompts.add("Please make a selection: [1]");

        String input = getData(
                clusterTypePrompts,
                "1",
                clusterPromptMapper.keySet().toArray(new String[]{}));

        ClusteringType clusterType = clusterPromptMapper.get(input);

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

        boolean validatedType = false;
        switch (clusterBean.getClusterType()) {
            case CLUSTER_NONE:
            case CLUSTER_NEW:
            case CLUSTER_JOIN:
                validatedType = true;
                break;
            default:
                validatedType = false;
        }

        return validatedHostname && validatedType;
    }
}
