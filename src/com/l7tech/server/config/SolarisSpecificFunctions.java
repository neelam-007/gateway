package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;

/**
 * User: megery
 * Date: May 7, 2007
 * Time: 11:46:03 AM
 */
public class SolarisSpecificFunctions extends UnixSpecificFunctions {
    public SolarisSpecificFunctions(String osname) {
        this(osname, null);
    }

    public SolarisSpecificFunctions(String osname, String partitionName) {
        super(osname, partitionName);
    }

    //put anything that is specific to the flabour of UNIX you are working with (i.e. solaris, Linux etc) in here.
    void doSpecialSetup() {
        //fix this once we knoew more about solaris
        networkConfigDir = "/etc/sysconfig/network-scripts";

        //these both are fictitious at this point, until we grok pkg more
        upgradeFileNewExt = "newpkgfiles";
        upgradeFileOldExt = "oldpkgfiles";

        keystoreInfos = new KeystoreInfo[]
        {
            new KeystoreInfo(KeystoreType.DEFAULT_KEYSTORE_NAME),
            new KeystoreInfo(KeystoreType.SCA6000_KEYSTORE_NAME),
        };
    }

    public String getFirewallRulesForPartition(PartitionInformation.HttpEndpointHolder basicEndpoint, PartitionInformation.HttpEndpointHolder sslEndpoint, PartitionInformation.HttpEndpointHolder noAuthSslEndpoint, PartitionInformation.OtherEndpointHolder rmiEndpoint) {
        return "!!! No Rules for IPF yet !!!";
    }
}
