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

    public static class NetworkConfig {
        String interfaceName;
        String bootProto;
        String ipAddress;
        String netMask;
        String gateway;
        private boolean isNewInterface;
        
        public NetworkConfig(String interfaceName, String bootProto, String ipAddress, String netMask, String gateway) {
            this.interfaceName = interfaceName;
            this.bootProto = bootProto;
            this.ipAddress = ipAddress;
            this.netMask = netMask;
            this.gateway = gateway;
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
            sb.append(getInterfaceName()).append(" (").append(getBootProto());
            if (StringUtils.equalsIgnoreCase(STATIC_BOOT_PROTO, getBootProto()))
                sb.append(", IP = ").append(getIpAddress()).append(", NETMASK = ").append(getNetMask()).append(", GATEWAY =").append(getGateway());

            sb.append(")");
            return sb.toString();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DEVICE=").append(interfaceName).append(eol);
            sb.append("ONBOOT=yes").append(eol);
            sb.append("BOOTPROTO=").append(bootProto).append(eol);
            //sb.append("TYPE=Ethernet");
            if (STATIC_BOOT_PROTO.equals(bootProto)) {
                sb.append("IPADDR=").append(ipAddress).append(eol);
                sb.append("NETMASK=").append(netMask).append(eol);
                sb.append("GATEWAY=").append(gateway).append(eol);
            } else { //if this is a DHCP interface
                sb.append("check_link_down() {").append(eol).append("\treturn 1;").append(eol).append("\n}");
            }
            return sb.toString();
        }

        public void isNew(boolean isItNew) {
            isNewInterface = isItNew;
        }

        public boolean isNew() {
            return isNewInterface;
        }
    }

    public static NetworkConfig makeNetworkConfig(String interfaceName, String bootProto, String ipAddress, String netMask, String gateway) {
        return new NetworkConfig(interfaceName, bootProto, ipAddress, netMask, gateway);
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
