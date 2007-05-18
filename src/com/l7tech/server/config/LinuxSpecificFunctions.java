package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;

import java.util.List;
import java.util.ArrayList;

public class LinuxSpecificFunctions extends UnixSpecificFunctions {
    private static final String IP_MARKER = "<IP>";
    private static final String PORT_MARKER = "<PORT>";
    private static final String TEMPLATE_IP_PORT = "[0:0] -I INPUT $Rule_Insert_Point -d " + IP_MARKER + " -p tcp -m tcp --dport " + PORT_MARKER + " -j ACCEPT\n";
    private static final String TEMPLATE_PORT = "[0:0] -I INPUT $Rule_Insert_Point -p tcp -m tcp --dport " + PORT_MARKER + " -j ACCEPT\n";

    public LinuxSpecificFunctions(String osname) {
        this(osname, null);
    }

    public LinuxSpecificFunctions(String osname, String partitionName) {
        super(osname, partitionName);
    }

    void doSpecialSetup() {

        networkConfigDir = "/etc/sysconfig/network-scripts";
        upgradeFileNewExt = "rpmnew";
        upgradeFileOldExt = "rpmsave";

        List<KeystoreInfo> infos = new ArrayList<KeystoreInfo>();
        infos.add(new KeystoreInfo(KeystoreType.DEFAULT_KEYSTORE_NAME));

        if (KeystoreInfo.isLunaEnabled()) {
            KeystoreInfo lunaInfo = new KeystoreInfo(KeystoreType.LUNA_KEYSTORE_NAME);
            lunaInfo.addMetaInfo("INSTALL_DIR", "/usr/lunasa");
            lunaInfo.addMetaInfo("JSP_DIR", "/usr/lunasa/jsp");
            lunaInfo.addMetaInfo("CMU_PATH", "bin/cmu");
            infos.add(lunaInfo);
        }
        
        if (KeystoreInfo.isHSMEnabled()) {
            KeystoreInfo hsmInfo = new KeystoreInfo(KeystoreType.SCA6000_KEYSTORE_NAME);
            hsmInfo.addMetaInfo("KEYSTORE_DATA_DIR", "/var/opt/sun/sca6000/keydata/");
            infos.add(hsmInfo);
        }
        keystoreInfos = infos.toArray(new KeystoreInfo[0]);

        timeZonesDir = "/usr/share/zoneinfo/";
    }

    public boolean isLinux() {
        return true;
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
}
