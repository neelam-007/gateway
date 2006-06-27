package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: May 23, 2006
 * Time: 2:27:59 PM
 */
public class NetworkingConfigurationBean extends BaseConfigurationBean {
    public static final String DYNAMIC_BOOT_PROTO = "dhcp";
    public static final String STATIC_BOOT_PROTO = "static";

    private String hostname;

    public static class NetworkConfig {
        private String interfaceName;
        private String bootProto;
        private String ipAddress;
        private String netMask;
        private String gateway;
        private boolean isNewInterface;
        private String[] nameservers;

        public NetworkConfig(String interfaceName, String bootProto) {
            this.interfaceName = interfaceName;
            this.bootProto = bootProto;
            isNewInterface = false;
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

        public String describe() {
            if (StringUtils.isEmpty(getInterfaceName())) return "Configure an interface not listed";

            StringBuilder sb = new StringBuilder();
            sb.append(getInterfaceName()).append(" (").append(getBootProto()).append(")");
            return sb.toString();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("--device ").append(interfaceName).append(eol);
            sb.append("--bootproto=").append(bootProto).append(eol);
            if (STATIC_BOOT_PROTO.equals(bootProto)) {
                sb.append("--ip=").append(ipAddress).append(eol);
                sb.append("--netmask=").append(netMask).append(eol);
                sb.append("--gateway=").append(gateway).append(eol);
                if (nameservers != null) {
                    for (String ns : nameservers) {
                        sb.append("--nameserver").append(ns).append(eol);
                    }
                }
            }

            return sb.toString();
        }

        public void isNew(boolean isItNew) {
            isNewInterface = isItNew;
        }

        public boolean isNew() {
            return isNewInterface;
        }

        public void setNameServer(String[] nameServers) {
            this.nameservers = nameServers;
        }

        public String[] getNameServers() {
            return nameservers;
        }
    }

    public static NetworkConfig makeNetworkConfig(String interfaceName, String bootProto) {
        return new NetworkConfig(interfaceName, bootProto);
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
            explanations.add("Configure \"" + networkConfig.getInterfaceName() + "\" interface");
            explanations.add("\tBOOTPROTO=" + networkConfig.getBootProto());
            if (networkConfig.getBootProto().equals(STATIC_BOOT_PROTO)) {
                explanations.add("\tIPADDR=" + networkConfig.getIpAddress());
                explanations.add("\tNETMASK=" + networkConfig.getNetMask());
                explanations.add("\tGATEWAY=" + networkConfig.getGateway());
                if (networkConfig.getNameServers() != null) {
                    for (String ns : networkConfig.getNameServers()) {
                        explanations.add("\tNAMESERVER=" + ns);
                    }
                }
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

}
