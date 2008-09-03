package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import com.l7tech.server.config.OSDetector;
import org.apache.commons.lang.StringUtils;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: May 23, 2006
 * Time: 2:27:59 PM
 */
public class NetworkingConfigurationBean extends BaseConfigurationBean {

    private static final Logger logger = Logger.getLogger(NetworkingConfigurationBean.class.getName());

    public static final String DYNAMIC_BOOT_PROTO = "dhcp";
    public static final String STATIC_BOOT_PROTO = "static";

    private String hostname;
    private String domain = "";

    private List<NetworkConfig> networkingConfigs;

    public static NetworkConfig makeNetworkConfig(NetworkInterface nic, String bootProto, boolean includeIPV6) {
        return new NetworkConfig(nic, bootProto, includeIPV6);
    }

    public NetworkingConfigurationBean(String name, String description) {
        super(name, description);
        init();
    }

    private void init() {
        getExistingInterfaces();
    }

    public void reset() {}

    protected void populateExplanations() {
        for (NetworkConfig networkConfig : networkingConfigs) {
            if (networkConfig != null && networkConfig.isDirtyFlag()) {
                explanations.add("Configure \"" + networkConfig.getInterfaceName() + "\" interface");
                explanations.add("\tBOOTPROTO=" + networkConfig.getBootProto());
                if (networkConfig.getBootProto().equals(STATIC_BOOT_PROTO)) {
                    explanations.add("\tIPADDR=" + networkConfig.getIpAddresses().get(0));
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
    }

    public void addNetworkingConfig(NetworkConfig netConfig) {
        if (!networkingConfigs.contains(netConfig))
            networkingConfigs.add(netConfig);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public List<NetworkConfig> getAllNetworkInterfaces() {
        return networkingConfigs;
    }

    private void getExistingInterfaces() {
        logger.info("Determining existing interface information.");
        try {
            networkingConfigs  = OSDetector.getOSSpecificFunctions().getNetworkConfigs(false, false);
        } catch (SocketException e) {
            logger.warning("Error while determining the IP Addresses for this machine:" + e.getMessage());
        }
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public static class NetworkConfig {
        private String interfaceName;
        private String bootProto;
        private List<String> ipAddresses;
        private String netMask;
        private String gateway;
        private String[] nameservers;
        private boolean dirtyFlag;
        NetworkInterface nic;

        protected NetworkConfig(NetworkInterface nic, String bootProto, boolean includeIPV6) {
            this.nic = nic;
            ipAddresses = new ArrayList<String>();
            if (nic != null) {
                List<InterfaceAddress> addrs = nic.getInterfaceAddresses();
                for (InterfaceAddress addr : addrs) {
                    if (addr.getAddress() instanceof Inet6Address && includeIPV6)
                        ipAddresses.add(addr.getAddress().getHostAddress());
                    else
                        ipAddresses.add(addr.getAddress().getHostAddress());
                }
            }
            this.bootProto = bootProto;
        }

        public String getInterfaceName() {
            if (nic == null)
                return interfaceName;
            return nic.getName();
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

        public List<InterfaceAddress> getInterfaceAddresses() {
            if (nic == null)
                return Collections.emptyList();
            return nic.getInterfaceAddresses();
        }

        public List<String> getIpAddresses() {
           return ipAddresses;
        }

        public void setIpAddress(String ipAddress, boolean overWrite) {
            if (overWrite)
                ipAddresses = new ArrayList<String>();
            ipAddresses.add(ipAddress);
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
            sb.append("--device ").append(getInterfaceName()).append(eol);
            sb.append("--bootproto=").append(bootProto).append(eol);
            if (STATIC_BOOT_PROTO.equals(bootProto)) {
                sb.append("--ip=").append(getIpAddresses().get(0)).append(eol);
                sb.append("--netmask=").append(netMask).append(eol);
                sb.append("--gateway=").append(gateway).append(eol);
                if (nameservers != null) {
                    for (String ns : nameservers) {
                        sb.append("--nameserver=").append(ns).append(eol);
                    }
                }
            }

            return sb.toString();
        }

        public void setNameServer(String[] nameServers) {
            this.nameservers = nameServers;
        }

        public String[] getNameServers() {
            return nameservers;
        }

        public boolean isDirtyFlag() {
            return dirtyFlag;
        }

        public void setDirtyFlag(boolean dirtyFlag) {
            this.dirtyFlag = dirtyFlag;
        }

        public NetworkInterface getNetworkInterface() {
            return nic;
        }

        public byte[] getHardwareAddress() throws SocketException {
            if (nic != null)
                return nic.getHardwareAddress();
            return null;
        }

        public boolean isVirtual() {
            return (nic != null && nic.isVirtual());
        }
    }

}
