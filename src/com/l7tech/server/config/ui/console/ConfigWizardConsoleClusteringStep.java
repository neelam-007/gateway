package com.l7tech.server.config.ui.console;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.config.*;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.commands.ClusteringConfigCommand;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.text.ParseException;
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
    private static final String TITLE = "Configuration Type";
    private static final String HEADER_DB_INFO = "-- Database Connection Information --";
    private static final String COPY_MASTER_CONFIG = "Enter the database information for the database used by the cluster.";
    private static final String REPLICATED_HOSTNAME_INSTRUCTIONS = "Specify the database hostname. If you are using a replicated database, enter the hostnames of the replicated pair in failover order, separated by commas.";

    public ConfigWizardConsoleClusteringStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        init();
    }

    private void init() {
        configBean = new ClusteringConfigBean();
        clusterBean = (ClusteringConfigBean) configBean;
        configCommand = new ClusteringConfigCommand(configBean);
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        if (!validated) {
            printText("*** There were errors in your input, please try again. ***" + getEolChar());
        }

        try {
            boolean continueInterview = doGetConfigTypePrompt(false);
            if (continueInterview) {
                doHostnamePrompt(getDefaultHostName());
            }
            clusterBean.setPartitionInformation(PartitionManager.getInstance().getActivePartition());
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
            "The hostname is used to identify the SSG and generate its certificates" + getEolChar(),
            "What hostname would you like to use? (Enter a fully qualified hostname - ex ssg.mydomain.com) [" + defaultClusterHostname + "] : ",
        }, defaultClusterHostname, (String[]) null, null);

        clusterBean.setClusterHostname(input.trim());

        getParentWizard().setHostname(input.trim());
    }

    private boolean doGetConfigTypePrompt(boolean invalidInput) throws IOException, WizardNavigationException {

        if (invalidInput) {
            printText("Please select one of the options shown" + getEolChar());
        }

        List<String> configTypePrompts = new ArrayList<String>();

        Map<String, ConfigurationType> configTypeMapper = new TreeMap<String, ConfigurationType>();
        //get config type preference
        int i = 1;
        for (ConfigurationType clusterType : ConfigurationType.values()) {
            if (clusterType != ConfigurationType.UNDEFINED)
                configTypeMapper.put(String.valueOf(i++), clusterType);
        }

        Set<String> keys = configTypeMapper.keySet();
        for (String key : keys) {
            ConfigurationType ctype = configTypeMapper.get(key);
            configTypePrompts.add(key + ") " + ctype + getEolChar());
        }

        configTypePrompts.add("Please make a selection: [1]");

        String input = getData(
                configTypePrompts.toArray(new String[configTypePrompts.size()]),
                "1",
                configTypeMapper.keySet().toArray(new String[configTypeMapper.keySet().size()]),
                null
        );

        ConfigurationType configType = configTypeMapper.get(input);
        clusterBean.setConfigType(configType);
        SharedWizardInfo.getInstance().setConfigType(configType);
        if (configType == ConfigurationType.CONFIG_CLUSTER) {
            doClusteringPrompts();
            if (clusterBean.getClusterType() == ClusteringType.CLUSTER_CLONE) {
                doCloningPrompts();
                SilentConfigData configData = loadSettingsFromDb();
                if (configData != null) {
                    if (shouldApplyNewSettings()) {
                        parent.setSilentConfigData(configData);
                        parent.setJumpToApply(true);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean shouldApplyNewSettings() throws IOException, WizardNavigationException {
        printText(getEolChar());
        return getConfirmationFromUser(
                "Settings have been successfully extracted and are ready to apply." + getEolChar() +
                "Would you like to apply this configuration?", "no");
    }

    private SilentConfigData loadSettingsFromDb() throws IOException, WizardNavigationException {
        SilentConfigData configData = null;
        DBInformation dbInfo = SharedWizardInfo.getInstance().getDbinfo();
        String msg = "Connecting to Database using " + dbInfo.getUsername() + "@" + dbInfo.getHostname() + "/" + dbInfo.getDbName();
        printText(msg + getEolChar());
        logger.info(msg);

        SilentConfigurator silentConf = new SilentConfigurator(osFunctions);
        byte[] configBytes = silentConf.loadConfigFromDb(dbInfo);

        String exceptionMessage = null;
        if (configBytes != null) {
            String passphrase = getSecretData(new String[]{
                    "Retrieved configuration settings from the database." + getEolChar(),
                    "Please enter the passphrase to extract these settings: "},
                    null, null, null);
        try {
                configData = silentConf.decryptConfigSettings(passphrase.toCharArray(), new String(configBytes, "UTF-8"));
            } catch (IllegalBlockSizeException e) {
                exceptionMessage = "there was an error while trying to extract the settings. " + ExceptionUtils.getMessage(e);
            } catch (InvalidKeyException e) {
                exceptionMessage = "there was an error while trying to extract the settings. " + ExceptionUtils.getMessage(e);
            } catch (ParseException e) {
                exceptionMessage = "there was an error while trying to extract the settings. " + ExceptionUtils.getMessage(e);
            } catch (BadPaddingException e) {
                exceptionMessage = "there was an error while trying to extract the settings. " + ExceptionUtils.getMessage(e);
            } catch (InvalidAlgorithmParameterException e) {
                exceptionMessage = "there was an error while trying to extract the settings. " + ExceptionUtils.getMessage(e);
            } finally {
                if (exceptionMessage != null) {
                    logger.severe(exceptionMessage);
                    configData = null;
                }
            }
        }
        return configData;
    }

    private void doCloningPrompts() throws IOException, WizardNavigationException {
        printText(getEolChar() + HEADER_DB_INFO + getEolChar() + getEolChar());
        printText(COPY_MASTER_CONFIG + getEolChar());
        printText(getEolChar() + REPLICATED_HOSTNAME_INSTRUCTIONS + getEolChar());
        DBInformation dbInfo = new DBInfoGetter(parent.getWizardUtils(),isShowNavigation()).getDbInfo("localhost","ssg","gateway","",false);
        parent.setDbInfo(dbInfo);
    }

    private void doClusteringPrompts() throws IOException, WizardNavigationException {
        List<String> clusteringTypePrompts = new ArrayList<String>();

        Map<String, ClusteringType> clusterTypeMapper = new TreeMap<String, ClusteringType>();
        //get clustering type preference
        int i = 1;
        for (ClusteringType clusterType : ClusteringType.values()) {
            if (clusterType != ClusteringType.UNDEFINED)
                clusterTypeMapper.put(String.valueOf(i++), clusterType);
        }

        clusteringTypePrompts.add(getEolChar() + "-- Clustering Configuration --" + getEolChar() + getEolChar());
        Set<String> keys = clusterTypeMapper.keySet();
        for (String key : keys) {
            ClusteringType ctype = clusterTypeMapper.get(key);
            clusteringTypePrompts.add(key + ") " + ctype + getEolChar());
        }

        clusteringTypePrompts.add("Please make a selection: [1]");

        String input = getData(
                clusteringTypePrompts.toArray(new String[clusteringTypePrompts.size()]),
                "1",
                clusterTypeMapper.keySet().toArray(new String[clusterTypeMapper.keySet().size()]),
                null
        );

        ClusteringType clusteringType = clusterTypeMapper.get(input);
        clusterBean.setClusterType(clusteringType);
        SharedWizardInfo.getInstance().setClusterType(clusterBean.getClusterType());
    }

    public String getTitle() {
        return TITLE;
    }

    public boolean validateStep() {
//        boolean validatedHostname = false;
//        String clusterHostName = clusterBean.getClusterHostname();
//        if (StringUtils.isNotEmpty(clusterHostName)) {
//            validatedHostname = true;
//        }

//        boolean validatedType;
//        switch (clusterBean.getClusterType()) {
//            case CLUSTER_MASTER:
//            case CLUSTER_CLONE:
//            case CLUSTER_JOIN:
//                validatedType = true;
//                break;
//            default:
//                validatedType = false;
//        }

//        return validatedHostname;
        return true;
    }
}
