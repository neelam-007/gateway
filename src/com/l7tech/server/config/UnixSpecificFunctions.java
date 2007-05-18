package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;

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

    public abstract String getFirewallRulesForPartition(PartitionInformation.HttpEndpointHolder basicEndpoint,
                                                        PartitionInformation.HttpEndpointHolder sslEndpoint,
                                                        PartitionInformation.HttpEndpointHolder noAuthSslEndpoint,
                                                        PartitionInformation.FtpEndpointHolder basicFtpEndpoint,
                                                        PartitionInformation.FtpEndpointHolder sslFtpEndpoint,
                                                        PartitionInformation.OtherEndpointHolder rmiEndpoint);
}
