package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.SocketException;

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
        doSpecialSetup();
    }

    //put anything that is specific to the flabour of UNIX you are working with (i.e. solaris, Linux etc) in here.
    abstract void doSpecialSetup();

    public boolean isUnix() {
        return true;
    }

    public String getOriginalPartitionControlScriptName() {
        return getSsgInstallRoot() + "bin/" + partitionControlScriptName;
    }
    
    public String getSpecificPartitionControlScriptName() {
        return getPartitionBase() + getPartitionName() + "/" + partitionControlScriptName;
    }

    public List<NetworkingConfigurationBean.NetworkConfig> getNetworkConfigs() throws SocketException {
        List<NetworkingConfigurationBean.NetworkConfig> networkConfigs = new ArrayList<NetworkingConfigurationBean.NetworkConfig>();

        Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
        while (allInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = allInterfaces.nextElement();
            if (!networkInterface.isLoopback()) {
                NetworkingConfigurationBean.NetworkConfig aConfig = createNetworkConfig(networkInterface);
                if (aConfig != null) networkConfigs.add(aConfig);

                Enumeration subInterfaces = networkInterface.getSubInterfaces();
                while (subInterfaces.hasMoreElements()) {
                    NetworkInterface subInterface = (NetworkInterface) subInterfaces.nextElement();
                    if (!subInterface.isLoopback()) {
                        NetworkingConfigurationBean.NetworkConfig subConfig = createNetworkConfig(subInterface);
                        if (subConfig != null) networkConfigs.add(subConfig);
                    }
                }
            }
        }

        return networkConfigs;
    }

    abstract NetworkingConfigurationBean.NetworkConfig createNetworkConfig(NetworkInterface networkInterface);


    public abstract String getFirewallRulesForPartition(PartitionInformation.HttpEndpointHolder basicEndpoint,
                                                        PartitionInformation.HttpEndpointHolder sslEndpoint,
                                                        PartitionInformation.HttpEndpointHolder noAuthSslEndpoint,
                                                        PartitionInformation.FtpEndpointHolder basicFtpEndpoint,
                                                        PartitionInformation.FtpEndpointHolder sslFtpEndpoint,
                                                        PartitionInformation.OtherEndpointHolder rmiEndpoint);
}
