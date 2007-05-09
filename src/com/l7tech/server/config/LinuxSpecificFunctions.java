package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;

import java.util.List;
import java.util.ArrayList;

public class LinuxSpecificFunctions extends UnixSpecificFunctions {
    private static final String BASIC_IP_MARKER = "<HTTP_BASIC_IP>";
    private static final String BASIC_PORT_MARKER = "<HTTP_BASIC_PORT>";
    private static final String SSL_IP_MARKER = "<SSL_IP>";
    private static final String SSL_PORT_MARKER = "<SSL_PORT>";
    private static final String NOAUTH_SSL_IP_MARKER = "<SSL_NOAUTH_IP>";
    private static final String NOAUTH_SSL_PORT_MARKER = "<SSL_NOAUTH_PORT>";
    private static final String RMI_PORT_MARKER = "<RMI_PORT>";

    public LinuxSpecificFunctions(String osname) {
        this(osname, null);
    }

    public LinuxSpecificFunctions(String osname, String partitionName) {
        super(osname, partitionName);
    }

    void doSpecialSetup() {
        KeystoreInfo lunaInfo = new KeystoreInfo(KeystoreType.LUNA_KEYSTORE_NAME);
        lunaInfo.addMetaInfo("INSTALL_DIR", "/usr/lunasa");
        lunaInfo.addMetaInfo("JSP_DIR", "/usr/lunasa/jsp");
        lunaInfo.addMetaInfo("CMU_PATH", "bin/cmu");

        networkConfigDir = "/etc/sysconfig/network-scripts";
        upgradeFileNewExt = "rpmnew";
        upgradeFileOldExt = "rpmsave";

        List<KeystoreInfo> infos = new ArrayList<KeystoreInfo>();
        infos.add(new KeystoreInfo(KeystoreType.DEFAULT_KEYSTORE_NAME));
        infos.add(lunaInfo);
        if (KeystoreInfo.isHSMEnabled())
            infos.add(new KeystoreInfo(KeystoreType.SCA6000_KEYSTORE_NAME));

        keystoreInfos = infos.toArray(new KeystoreInfo[0]);
    }

    public boolean isLinux() {
        return true;
    }

    public String getFirewallRulesForPartition(PartitionInformation.HttpEndpointHolder basicEndpoint, PartitionInformation.HttpEndpointHolder sslEndpoint, PartitionInformation.HttpEndpointHolder noAuthSslEndpoint, PartitionInformation.OtherEndpointHolder rmiEndpoint) {
        String firewallRules = new String(
            "[0:0] -I INPUT $Rule_Insert_Point -d "+ BASIC_IP_MARKER +" -p tcp -m tcp --dport " + BASIC_PORT_MARKER +" -j ACCEPT\n" +
            "[0:0] -I INPUT $Rule_Insert_Point -d " + SSL_IP_MARKER +" -p tcp -m tcp --dport " + SSL_PORT_MARKER + " -j ACCEPT\n" +
            "[0:0] -I INPUT $Rule_Insert_Point -d " + NOAUTH_SSL_IP_MARKER + " -p tcp -m tcp --dport " + NOAUTH_SSL_PORT_MARKER + " -j ACCEPT\n" +
            "[0:0] -I INPUT $Rule_Insert_Point -p tcp -m tcp --dport " + RMI_PORT_MARKER + " -j ACCEPT\n"
        );

        if (basicEndpoint.getIpAddress().equals("*"))
            firewallRules= firewallRules.replaceAll("-d " + BASIC_IP_MARKER, "");
        else
            firewallRules = firewallRules.replaceAll(BASIC_IP_MARKER, basicEndpoint.getIpAddress());
        firewallRules = firewallRules.replaceAll(BASIC_PORT_MARKER, basicEndpoint.getPort());

        if (sslEndpoint.getIpAddress().equals("*"))
            firewallRules = firewallRules.replaceAll("-d " + SSL_IP_MARKER, "");
        else
            firewallRules = firewallRules.replaceAll(SSL_IP_MARKER, sslEndpoint.getIpAddress());
        firewallRules = firewallRules.replaceAll(SSL_PORT_MARKER, sslEndpoint.getPort());

        if (noAuthSslEndpoint.getIpAddress().equals("*"))
            firewallRules = firewallRules.replaceAll("-d " + NOAUTH_SSL_IP_MARKER, "");
        else
            firewallRules = firewallRules.replaceAll(NOAUTH_SSL_IP_MARKER, noAuthSslEndpoint.getIpAddress());
        firewallRules = firewallRules.replaceAll(NOAUTH_SSL_PORT_MARKER, noAuthSslEndpoint.getPort());

        firewallRules = firewallRules.replaceAll(RMI_PORT_MARKER, rmiEndpoint.getPort());

        return firewallRules;
    }
}
