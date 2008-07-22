package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;

import java.util.List;
import java.util.ArrayList;
import java.net.NetworkInterface;
import java.io.File;

/**
 * User: megery
 * Date: May 7, 2007
 * Time: 11:46:03 AM
 */
public class SolarisSpecificFunctions extends UnixSpecificFunctions {
//uncomment these if we decide that solaris gets the easy PORT IP format in firewall_rules
//    private static final String IP_MARKER = "<IP>";
//    private static final String PORT_MARKER = "<PORT>";
//    private static final String TEMPLATE_IP_PORT = PORT_MARKER + " " + IP_MARKER + "\n";
//    private static final String TEMPLATE_PORT = PORT_MARKER + "\n";

//for now, solaris firewall_rules will look like the linux ones (iptables)
    private static final String IP_MARKER = "<IP>";
    private static final String PORT_MARKER = "<PORT>";
    private static final String TEMPLATE_IP_PORT = "[0:0] -I INPUT $Rule_Insert_Point -d " + IP_MARKER + " -p tcp -m tcp --dport " + PORT_MARKER + " -j ACCEPT\n";
    private static final String TEMPLATE_PORT = "[0:0] -I INPUT $Rule_Insert_Point -p tcp -m tcp --dport " + PORT_MARKER + " -j ACCEPT\n";

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

        List<KeystoreInfo> infos = new ArrayList<KeystoreInfo>();
        infos.add(new KeystoreInfo(KeystoreType.DEFAULT_KEYSTORE_NAME));
        if (KeystoreInfo.isHSMEnabled())
            infos.add(new KeystoreInfo(KeystoreType.SCA6000_KEYSTORE_NAME));

        keystoreInfos = infos.toArray(new KeystoreInfo[0]);

        timeZonesDir = "/usr/share/lib/zoneinfo/";
    }

    NetworkingConfigurationBean.NetworkConfig createNetworkConfig(NetworkInterface networkInterface, boolean includeIPV6) {
        //get the corresponding info on this interface from the various places they exist in solaris
        return collectInterfaceInfo(networkInterface, includeIPV6);
    }

    private NetworkingConfigurationBean.NetworkConfig collectInterfaceInfo(NetworkInterface networkInterface, boolean includeIPV6) {
        String ifName = networkInterface.getName();
        File isInterfaceDhcpFile = new File("/etc/", "dhcp."+ifName);
        if (isInterfaceDhcpFile.exists()) {
            return NetworkingConfigurationBean.makeNetworkConfig(networkInterface, NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO, includeIPV6);
        } else {
            return NetworkingConfigurationBean.makeNetworkConfig(networkInterface, NetworkingConfigurationBean.STATIC_BOOT_PROTO, includeIPV6);
        }
    }

    public String getFirewallRulesForPartition(PartitionInformation.HttpEndpointHolder basicEndpoint,
                                               PartitionInformation.HttpEndpointHolder sslEndpoint,
                                               PartitionInformation.HttpEndpointHolder noAuthSslEndpoint,
                                               PartitionInformation.FtpEndpointHolder basicFtpEndpoint,
                                               PartitionInformation.FtpEndpointHolder sslFtpEndpoint,
                                               PartitionInformation.OtherEndpointHolder rmiEndpoint) {
        StringBuffer firewallRules = new StringBuffer();

        // HTTP Basic
        if (basicEndpoint.isEnabled()) {
            firewallRules.append(buildIPPortRule(basicEndpoint.getIpAddress(), basicEndpoint.getPort().toString()));
        }

        // HTTP SSL
        if (sslEndpoint.isEnabled()) {
            firewallRules.append(buildIPPortRule(sslEndpoint.getIpAddress(), sslEndpoint.getPort().toString()));
        }

        // HTTP SSL (no client cert)
        if (noAuthSslEndpoint.isEnabled()) {
            firewallRules.append(buildIPPortRule(noAuthSslEndpoint.getIpAddress(), noAuthSslEndpoint.getPort().toString()));
        }

        // FTP Basic
        if (basicFtpEndpoint.isEnabled()) {
            firewallRules.append(buildIPPortRule(basicFtpEndpoint.getIpAddress(), basicFtpEndpoint.getPort().toString()));
            firewallRules.append(buildIPPortRule(basicFtpEndpoint.getIpAddress(),
                    basicFtpEndpoint.getPassivePortStart() + ":" +
                    (basicFtpEndpoint.getPassivePortStart().intValue() + (basicFtpEndpoint.getPassivePortCount().intValue()-1))));
        }

        // FTP SSL
        if (sslFtpEndpoint.isEnabled()) {
            firewallRules.append(buildIPPortRule(sslFtpEndpoint.getIpAddress(), sslFtpEndpoint.getPort().toString()));
            firewallRules.append(buildIPPortRule(sslFtpEndpoint.getIpAddress(),
                    sslFtpEndpoint.getPassivePortStart() + ":" +
                    (sslFtpEndpoint.getPassivePortStart().intValue() + (sslFtpEndpoint.getPassivePortCount().intValue()-1))));
        }

        // RMI
        if (rmiEndpoint.isEnabled()) {
            firewallRules.append(TEMPLATE_PORT.replaceAll(PORT_MARKER, rmiEndpoint.getPort().toString()));
        }

        return firewallRules.toString();
    }

     private String buildIPPortRule(String ipAddress, String port) {
         String rule = TEMPLATE_IP_PORT;

         if (ipAddress.equals("*"))
             rule= rule.replaceAll("-d " + IP_MARKER, "");
         else
             rule = rule.replaceAll(IP_MARKER, ipAddress);

         rule = rule.replaceAll(PORT_MARKER, port);

         return rule;
     }
}
