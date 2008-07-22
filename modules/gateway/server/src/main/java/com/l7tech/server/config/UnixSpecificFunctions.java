package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.IOUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collections;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.File;
import java.io.IOException;

/**
 * User: megery
 * Date: Apr 26, 2007
 * Time: 3:59:23 PM
 */
public abstract class UnixSpecificFunctions extends OSSpecificFunctions {

    public UnixSpecificFunctions(String OSName) {
        this(OSName, null);
    }

    public UnixSpecificFunctions(String OSName, String partitionName) {
        super(OSName, partitionName);
    }

    void doOsSpecificSetup() {
        if (isEmptyString(installRoot)) {
            installRoot = "/ssg/";
        }
        pathToJdk = "jdk/";
        partitionControlScriptName = "partitionControl.sh";
        configWizardLauncher = installRoot + "configwizard/ssgconfig.sh";
        doSpecialSetup();
    }

    //put anything that is specific to the flabour of UNIX you are working with (i.e. solaris, Linux etc) in here.
    abstract void doSpecialSetup();

    public boolean isUnix() {
        return true;
    }

    public boolean isPartitionRunning(String partitionName) throws IOException, OsSpecificFunctionUnavailableException {
        if (partitionName != null && !partitionName.equalsIgnoreCase(getPartitionName())) {
            // It's a query about some other partition -- pass the buck to it
            return OSDetector.getOSSpecificFunctions(partitionName).isPartitionRunning(null);
        }

        // It's a query about the current partition
        File confDir = new File(getConfigurationBase());
        File pidFile = new File(confDir, "ssg.pid");
        if (!pidFile.exists())
            return false;
        String pidStr = new String( IOUtils.slurpFile(pidFile)).trim();
        if (pidStr.length() < 1)
            return false;
        ProcResult result = ProcUtils.exec(null, new File("/bin/ps"), new String[] { "-p", pidStr }, null, true);
        return result.getExitStatus() == 0;
    }

    public String getOriginalPartitionControlScriptName() {
        return getSsgInstallRoot() + "bin/" + partitionControlScriptName;
    }
    
    public String getSpecificPartitionControlScriptName() {
        return getPartitionBase() + getPartitionName() + "/" + partitionControlScriptName;
    }

    public List<NetworkingConfigurationBean.NetworkConfig> getNetworkConfigs(boolean getLoopBack, boolean getIPV6) throws SocketException {
        List<NetworkingConfigurationBean.NetworkConfig> networkConfigs = new ArrayList<NetworkingConfigurationBean.NetworkConfig>();

        List<NetworkInterface> allInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface networkInterface : allInterfaces) {
            boolean addIt = ( (!networkInterface.isLoopback()) || (networkInterface.isLoopback() && getLoopBack));
            if (addIt) {
                addNetworkConfigToList(networkInterface, networkConfigs, getIPV6);
            }
        }

        return networkConfigs;
    }

    private void addNetworkConfigToList(NetworkInterface networkInterface, List<NetworkingConfigurationBean.NetworkConfig> networkConfigs, boolean getIPV6) throws SocketException {
        NetworkingConfigurationBean.NetworkConfig aConfig = createNetworkConfig(networkInterface, false);
        if (aConfig != null) networkConfigs.add(aConfig);

        Enumeration subInterfaces = networkInterface.getSubInterfaces();
        while (subInterfaces.hasMoreElements()) {
            NetworkInterface subInterface = (NetworkInterface) subInterfaces.nextElement();
            if (!subInterface.isLoopback()) {
                NetworkingConfigurationBean.NetworkConfig subConfig = createNetworkConfig(subInterface, false);
                if (subConfig != null) networkConfigs.add(subConfig);
            }
        }
    }

    abstract NetworkingConfigurationBean.NetworkConfig createNetworkConfig(NetworkInterface networkInterface, boolean includeIPV6);


    public abstract String getFirewallRulesForPartition(PartitionInformation.HttpEndpointHolder basicEndpoint,
                                                        PartitionInformation.HttpEndpointHolder sslEndpoint,
                                                        PartitionInformation.HttpEndpointHolder noAuthSslEndpoint,
                                                        PartitionInformation.FtpEndpointHolder basicFtpEndpoint,
                                                        PartitionInformation.FtpEndpointHolder sslFtpEndpoint,
                                                        PartitionInformation.OtherEndpointHolder rmiEndpoint);
}
