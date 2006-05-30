package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: May 23, 2006
 * Time: 2:27:59 PM
 */
public class NetworkingConfigurationBean extends BaseConfigurationBean {
    public static final String DYNAMIC_BOOT_PROTO = "dhcp";
    public static final String STATIC_BOOT_PROTO = "static";

    private static final String INTERFACE_CFG_FILE = "/etc/sysconfig/network-scripts/ifcfg-";

    public static class NetworkConfig {
        String interfaceName;
        String bootProto;
        String ipAddress;
        String netMask;
        String gateway;

        public NetworkConfig(String interfaceName, String bootProto, String ipAddress, String netMask) {
            this.interfaceName = interfaceName;
            this.bootProto = bootProto;
            this.ipAddress = ipAddress;
            this.netMask = netMask;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getBootProto() {
            return bootProto;
        }

        public void setBootProto(String bootProto) {
            this.bootProto = bootProto;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getNetMask() {
            return netMask;
        }

        public void setNetMask(String netMask) {
            this.netMask = netMask;
        }

        public String getGateway() {
            return gateway;
        }

        public void setGateway(String gateway) {
            this.gateway = gateway;
        }
    }

    public static NetworkConfig makeNetworkConfig(String interfaceName, String bootProto, String ipAddress, String netMask) {
        return new NetworkConfig(interfaceName, bootProto, ipAddress, netMask);
    }

    private List<NetworkConfig> networkingConfigs;

    public NetworkingConfigurationBean(String name, String description) {
        super(name, description);
        init();
    }

    private void init() {
        networkingConfigs = new ArrayList<NetworkConfig>();
    }

    public void reset() {
    }

    protected void populateExplanations() {
        for (NetworkConfig networkConfig : networkingConfigs) {
            explanations.add("Edit file: " + INTERFACE_CFG_FILE + networkConfig.getInterfaceName());
            explanations.add("Configure \"" + networkConfig.getInterfaceName() + "\" interface");
            explanations.add("\tBOOTPROTO=" + networkConfig.getBootProto());
            if (networkConfig.getBootProto().equals(STATIC_BOOT_PROTO)) {
                explanations.add("\tIPADDR=" + networkConfig.getIpAddress());
                explanations.add("\tNETMASK=" + networkConfig.getNetMask());
                explanations.add("\tGATEWAY=" + networkConfig.getGateway());
            }
            explanations.add("");
        }
    }

    public List<String> getManualSteps() {
        return null;
    }

    public List<NetworkConfig> getNetworkingConfigurations() {
        return networkingConfigs;
    }

    public void addNetworkingConfig(NetworkConfig netConfig) {
        networkingConfigs.add(netConfig);
    }
}
