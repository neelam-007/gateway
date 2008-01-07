package com.l7tech.server.config.ui.console;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.server.config.*;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Jan 3, 2008
 * Time: 2:15:26 PM
 */
public class SoftwareConfigWizard extends ConfigurationWizard {
    private static final Logger logger = Logger.getLogger(SoftwareConfigWizard.class.getName());

    public SoftwareConfigWizard(InputStream in, PrintStream out) {
        super(in, out);
    }

    public void applyConfiguration() {
        hadFailures = false;

        SilentConfigData silentConfig = getSilentConfigData();
        boolean isSilentMode = (silentConfig != null);
        if (isSilentMode) {
            commands = silentConfig.getCommands();

            KeystoreType ksType = silentConfig.getKeystoreType();
            if (ksType == KeystoreType.SCA6000_KEYSTORE_NAME) {
                if (!prepareHsmRestore(commands)) {
                    logger.severe("Could not prepare the configuration data for the new node. The configuration data is not valid. Exiting.");
                    wizardUtils.printText("Could not prepare the configuration data for the new node. The configuration data is not valid. "+ ConsoleWizardUtils.EOL_CHAR);
                    wizardUtils.printText("The configuration cannot proceed and the wizard will now exit."+ ConsoleWizardUtils.EOL_CHAR);
                    hadFailures = true;
                    return;
                }
            }

            PartitionInformation pInfo = silentConfig.getPartitionInfo();
            PartitionManager.getInstance().setActivePartition(pInfo);

            File f = new File(getOsFunctions().getPartitionBase() + pInfo.getPartitionId());
            if (!f.exists()) {
                PartitionActions pa = new PartitionActions(getOsFunctions());
                Exception ex = null;
                try {
                    pa.createNewPartition(pInfo.getPartitionId());
                    PartitionActions.prepareNewpartition(pInfo);
                } catch (IOException e) {
                    ex = e;
                } catch (InterruptedException e) {
                    ex = e;
                } catch (SAXException e) {
                    ex = e;
                } finally {
                    if (ex != null) {
                        logger.severe("there was an error while creating the partition " + pInfo.getPartitionId() + ": " + ExceptionUtils.getMessage(ex));
                        hadFailures = true;
                        return;
                    }
                }
            }
            //restore the keystore to the partition before running the commands
            File keystoreDir = new File(pInfo.getOSSpecificFunctions().getKeystoreDir());
            keystoreDir.mkdirs();

            try {
                writeFileBytes(silentConfig.getCaKeystore(), new File(keystoreDir, "ca.ks"));
                writeFileBytes(silentConfig.getCaCert(), new File(keystoreDir, "ca.cer"));
                writeFileBytes(silentConfig.getSslKeystore(), new File(keystoreDir, "ssl.ks"));
                writeFileBytes(silentConfig.getSslCert(), new File(keystoreDir, "ssl.cer"));
            } catch (IOException e) {
                hadFailures = true;
            }
        } else {
            if (additionalCommands != null)
                commands.addAll(additionalCommands);
        }

        Iterator<ConfigurationCommand> iterator = commands.iterator();


        wizardUtils.printText("Please wait while the configuration is applied ..." + ConsoleWizardUtils.EOL_CHAR);

        while (iterator.hasNext()) {
            ConfigurationCommand command = iterator.next();
            boolean successful;
            if (isSilentMode) successful = command.executeSilent();
            else successful= command.execute();

            if (!successful) {
                hadFailures = true;
            }
        }

        try {
            saveConfigData();
        } catch (IOException e) {
            logger.severe("There was an error while saving the configuration to the database: " + ExceptionUtils.getMessage(e));
            hadFailures = true;
        }
    }

    private boolean prepareHsmRestore(Collection<ConfigurationCommand> commands) {
        boolean found = false;
        for (ConfigurationCommand command : commands) {
            if (command instanceof KeystoreConfigCommand) {
                KeystoreConfigCommand keystoreConfigCommand = (KeystoreConfigCommand) command;
                KeystoreConfigBean ksBean = (KeystoreConfigBean) keystoreConfigCommand.getConfigBean();

                //ensure that we don't try to initialize the HSM, we are doing a restore on subsequent nodes.
                ksBean.setInitializeHSM(false);
                found = true;
            }
        }
        return found;
    }

    private void saveConfigData() throws IOException {

        ConfigurationType configType = SharedWizardInfo.getInstance().getConfigType();
        if (configType == ConfigurationType.CONFIG_CLUSTER) {
            ClusteringType clusterType = SharedWizardInfo.getInstance().getClusterType();
            if (clusterType == ClusteringType.CLUSTER_MASTER) {
                logger.info("Saving configuration data to the database.");
                PartitionInformation pInfo = PartitionManager.getInstance().getActivePartition();
                byte[] sslKeystoreBytes = getFileBytes(pInfo, "ssl.ks");
                byte[] sslCertBytes = getFileBytes(pInfo, "ssl.cer");
                byte[] caKeystoreBytes = getFileBytes(pInfo, "ca.ks");
                byte[] caCertBytes = getFileBytes(pInfo, "ca.cer");

                SilentConfigData configData = new SilentConfigData();
                configData.setCommands(commands);
                configData.setDbInfo(sharedWizardInfo.getDbinfo());
                configData.setPartitionInfo(pInfo);

                configData.setKeystoreType(sharedWizardInfo.getKeystoreType());
                configData.setSslKeystore(sslKeystoreBytes);
                configData.setSslCert(sslCertBytes);
                configData.setCaKeystore(caKeystoreBytes);
                configData.setCaCert(caCertBytes);
                SilentConfigurator sc = new SilentConfigurator(osFunctions);
                sc.saveConfigToDb(sharedWizardInfo.getDbinfo(), getConfigDataPassphrase().toCharArray() ,configData);
            }
        }
    }

    private byte[] getFileBytes(PartitionInformation pInfo, String fileName) throws IOException {
        byte[] bytes = null;
        File path = new File(pInfo.getOSSpecificFunctions().getKeystoreDir(), fileName);
        if (path.isFile()) {
            try {
                bytes = HexUtils.slurpFile(path);
                if (bytes.length == 0)
                    logger.warning("While reading the config, the file \"" + path.getAbsolutePath() + "\" was found but is empty");
            } catch (IOException e) {
                logger.severe("Error while attempting to read the file \"" + path.getAbsolutePath() + "\" for config storage. " + ExceptionUtils.getMessage(e));
                throw e;
            }
        }
        return bytes;
    }

    private void writeFileBytes(byte[] bytes, File file) throws IOException {
        if (bytes == null) return;

        if (file.exists()) {
            logger.warning(file.getAbsolutePath() + " already exists. It will be overwritten");
        }

        try {
            HexUtils.spewStream(bytes, new FileOutputStream(file));
        } catch (IOException e) {
            logger.severe("Error while attempting to save the file \"" + file.getAbsolutePath() + "\" for config storage. " + ExceptionUtils.getMessage(e));
            throw e;
        }
    } 

}
