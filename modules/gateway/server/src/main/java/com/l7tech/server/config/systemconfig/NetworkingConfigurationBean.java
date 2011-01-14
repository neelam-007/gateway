package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.util.IpProtocol;
import org.apache.commons.lang.StringUtils;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet6Address;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: May 23, 2006
 * Time: 2:27:59 PM
 */
public class NetworkingConfigurationBean extends BaseConfigurationBean {

    // - PUBLIC

    public static final String DYNAMIC_BOOT_PROTO = "dhcp";
    public static final String STATIC_BOOT_PROTO = "static";
    public static final String NO_BOOT_PROTO = "none";

    public static InterfaceConfig makeNetworkConfig(NetworkInterface nic, String bootProto, boolean includeIPV6) {
        return new InterfaceConfig(nic, bootProto, includeIPV6);
    }

    public NetworkingConfigurationBean(String name, String description) {
        super(name, description);
        getExistingInterfaces();
    }

    /**
     * @return network configuration data formatted for /etc/network
     */
    public String getNetworkConfig() {
        return concatConfigLines(EOL, getNetworkConfigLines());
    }

    /**
     * @return nameserver configuration data formatted for /etc/resolv.conf
     */
    public String getResolvConf() {
        return concatConfigLines(EOL, getResolvConfLines());
    }

    @Override
    public void reset() {}

    @Override
    protected void populateExplanations() {
        List<String> network = getNetworkConfigLines();
        if (! network.isEmpty()) {
            explanations.add("\nNetwork configuration: \n\t");
            explanations.add(concatConfigLines(EOL + "\t", network));
        }

        List<String> resolvConf = getResolvConfLines();
        if (! resolvConf.isEmpty() ) {
            explanations.add("\nNameserver configuration: \n\t");
            explanations.add(concatConfigLines(EOL + "\t", resolvConf));
        }

        for (InterfaceConfig ifConfig : interfaceConfigs) {
            if (ifConfig != null && ifConfig.isDirtyFlag()) {
                explanations.add("\nConfigure \"" + ifConfig.getInterfaceName() + "\" interface\n\t");
                explanations.add(concatConfigLines(EOL + "\t", ifConfig.getConfigLines()));
            }
        }
    }

    public void addNetworkingConfig(InterfaceConfig ifConfig) {
        if (!interfaceConfigs.contains(ifConfig)) {
            interfaceConfigs.add(ifConfig);
            ifConfig.networkConfig = this;
        }
    }

    public List<InterfaceConfig> getAllNetworkInterfaces() {
        return interfaceConfigs;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public String getDefaultGatewayIp(IpProtocol ipProtocol) {
        return defaultGatewayIps.get(ipProtocol);
    }

    public void setDefaultGatewayIp(String defaultIpv4GatewayIp, IpProtocol ipProtocol) {
        defaultGatewayIps.put(ipProtocol, defaultIpv4GatewayIp);
    }

    public String getDefaultGatewayDevice(IpProtocol ipProtocol) {
        return defaultGatewayDevices.get(ipProtocol);
    }

    public void setDefaultGatewayDevice(String defaultGatewayDevice, IpProtocol ipProtocol) {
        defaultGatewayDevices.put(ipProtocol, defaultGatewayDevice);
    }

    public List<String> getNameServers() {
        return nameServers;
    }

    public String getNameservesLine() {
        return concatConfigLines(", ", nameServers);
    }

    public void addNameservers(List<String> newServers) {
        nameServers.addAll(newServers);
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(NetworkingConfigurationBean.class.getName());

    private String hostname;
    private String domain = "";

    private Map<IpProtocol,String> defaultGatewayIps = new HashMap<IpProtocol,String>();
    private Map<IpProtocol,String> defaultGatewayDevices = new HashMap<IpProtocol,String>();

    private List<String> nameServers = new ArrayList<String>();

    private List<InterfaceConfig> interfaceConfigs;

    private void getExistingInterfaces() {
        logger.info("Determining existing interface information.");
        try {
            interfaceConfigs = OSSpecificFunctions.getOSSpecificFunctions().getNetworkConfigs(false);
        } catch (SocketException e) {
            logger.warning("Error while determining the IP Addresses for this machine:" + e.getMessage());
        }
    }

    private List<String> getNetworkConfigLines() {
        List<String> network = new ArrayList<String>();

        network.add("NETWORKING=" + ( (isIpv4Enabled() || isIpv6Enabled()) ? "yes" : "no") );
        network.add("HOSTNAME=" + hostname + (! StringUtils.isEmpty(domain) ? "." + domain : "" ));

        if (! StringUtils.isEmpty(defaultGatewayIps.get(IpProtocol.IPv4)))
            network.add("GATEWAY=" + defaultGatewayIps.get(IpProtocol.IPv4));
        if (! StringUtils.isEmpty(defaultGatewayDevices.get(IpProtocol.IPv4)))
            network.add("GATEWAYDEV=" + defaultGatewayDevices.get(IpProtocol.IPv4));

        network.add("NETWORKING_IPV6=" + (isIpv6Enabled() ? "yes" : "no"));
        network.add("IPV6FORWARDING=no");

        if (! StringUtils.isEmpty(defaultGatewayIps.get(IpProtocol.IPv6)))
            network.add("IPV6_DEFAULTGW=" + defaultGatewayIps.get(IpProtocol.IPv6));
        if (! StringUtils.isEmpty(defaultGatewayDevices.get(IpProtocol.IPv6)))
            network.add("IPV6_DEFAULTDEV=" + defaultGatewayDevices.get(IpProtocol.IPv6));

        return network;
    }

    private boolean isIpv4Enabled() {
        for (InterfaceConfig ifConfig : interfaceConfigs) {
            if (ifConfig.isIpv4Enabled())
                return true;
        }
        return false;
    }

    private boolean isIpv6Enabled() {
        for (InterfaceConfig ifConfig : interfaceConfigs) {
            if (ifConfig.isIpv6Enabled())
                return true;
        }
        return false;
    }

    private List<String> getResolvConfLines() {
        List<String> resolvConf = new ArrayList<String>();
        if ( ! dhcpConfigured() ) {
            if (! StringUtils.isEmpty(domain)) {
                resolvConf.add("search " + domain);
                for (String nameServer : nameServers) {
                    resolvConf.add("nameserver " + nameServer);
                }
            }
        }
        return resolvConf;
    }

    private boolean dhcpConfigured() {
        for (InterfaceConfig ifConfig : interfaceConfigs) {
            if ( DYNAMIC_BOOT_PROTO.equalsIgnoreCase(ifConfig.getIpv4BootProto()) || ifConfig.isIpv6Dhcp() )
                return true;
        }
        return false;
    }

    public static class InterfaceConfig {

        private NetworkingConfigurationBean networkConfig;
        private String interfaceName;

        private boolean ipv4Enabled;
        private String ipv4BootProto;
        private List<String> ipv4Addresses;
        private String ipv4NetMask;
        private String ipv4Gateway;

        private boolean ipv6Enabled;
        private List<String> ipv6Addresses;
        private boolean ipv6AutoConf;
        private boolean ipv6Dhcp;

        private boolean dirtyFlag;
        NetworkInterface nic;

        protected InterfaceConfig(NetworkInterface nic, String ipv4BootProto, boolean includeIPV6) {
            this.nic = nic;
            ipv4Addresses = new ArrayList<String>();
            ipv6Addresses = new ArrayList<String>();
            if (nic != null) {
                this.interfaceName = nic.getName();
                List<InterfaceAddress> addrs = nic.getInterfaceAddresses();
                for (InterfaceAddress addr : addrs) {
                    if (addr.getAddress() instanceof Inet6Address && includeIPV6)
                        ipv6Addresses.add(addr.getAddress().getHostAddress());
                    else
                        ipv4Addresses.add(addr.getAddress().getHostAddress());
                }
            }
            this.ipv4BootProto = ipv4BootProto;
        }

        /**
         * @return interface configuration formatted for /etc/sysconfig/network-scripts/ifcfg-*
         */
        public String getInterfaceConfig() {
            return concatConfigLines(EOL, getConfigLines());
        }

        public void setNetworkConfig(NetworkingConfigurationBean networkConfig) {
            this.networkConfig = networkConfig;
        }

        public String getInterfaceName() {
            if (nic == null)
                return interfaceName;
            return nic.getName();
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getIpv4BootProto() {
            return ipv4BootProto;
        }

        public void setIpv4BootProto(String ipv4BootProto) {
            this.ipv4BootProto = ipv4BootProto;
        }

        public List<InterfaceAddress> getInterfaceAddresses() {
            if (nic == null)
                return Collections.emptyList();
            return nic.getInterfaceAddresses();
        }

        public void setIpv4Enabled(boolean enabled) {
            this.ipv4Enabled = enabled;
        }

        public boolean isIpv4Enabled() {
            return ipv4Enabled;
        }
        public List<String> getIpv4Addresses() {
           return ipv4Addresses;
        }

        public void setIpv4Address(String ipAddress, boolean overWrite) {
            if (overWrite)
                ipv4Addresses = new ArrayList<String>();
            ipv4Addresses.add(ipAddress);
        }

        public String getIpv4NetMask() {
            return ipv4NetMask;
        }

        public void setIpv4NetMask(String ipv4NetMask) {
            this.ipv4NetMask = ipv4NetMask;
        }

        public String getIpv4Gateway() {
            return ipv4Gateway;
        }

        public void setIpv4Gateway(String ipv4Gateway) {
            this.ipv4Gateway = ipv4Gateway;
        }

        public void setIpv6Enabled(boolean ipv6Enabled) {
            this.ipv6Enabled = ipv6Enabled;
        }

        public boolean isIpv6Enabled() {
            return ipv6Enabled;
        }

        public boolean isIpv6AutoConf() {
            return ipv6AutoConf;
        }

        public void setIpv6AutoConf(boolean ipv6AutoConf) {
            this.ipv6AutoConf = ipv6AutoConf;
        }

        public boolean isIpv6Dhcp() {
            return ipv6Dhcp;
        }

        public void setIpv6Dhcp(boolean ipv6Dhcp) {
            this.ipv6Dhcp = ipv6Dhcp;
        }

        public List<String> getIpv6Addresses() {
            return ipv6Addresses;
        }

        public void addIpv6Address(String ipv6Address) {
            ipv6Addresses.add(ipv6Address);
        }

        public String describe() {
            if (StringUtils.isEmpty(getInterfaceName())) return "Configure an interface not listed";
            StringBuilder sb = new StringBuilder();
            sb.append(getInterfaceName()).append(" (").append(getIpv4BootProto()).append(")");
            return sb.toString();
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

        private List<String> getConfigLines() {
            List<String> config = new ArrayList<String>();

            config.add("DEVICE=" + interfaceName);
            config.add("ONBOOT=yes");

            if(ipv4Enabled) {
                config.add("BOOTPROTO=" + ipv4BootProto);
                if (STATIC_BOOT_PROTO.equalsIgnoreCase(ipv4BootProto) || NO_BOOT_PROTO.equalsIgnoreCase(ipv4BootProto)) {
                    config.add("IPADDR=" + ipv4Addresses.get(0)); // only configure primary (IPv4) interface, no sub-interfaces
                    config.add("NETMASK=" + ipv4NetMask);
                    if ( ! StringUtils.isEmpty(ipv4Gateway) )
                        config.add("GATEWAY=" + ipv4Gateway);
                } else if (DYNAMIC_BOOT_PROTO.equalsIgnoreCase(ipv4BootProto)) {
                    config.add("DHCP_HOSTNAME=" + networkConfig.getHostname());
                }
            }

            config.add("IPV6INIT=" + (ipv6Enabled ? "yes" : "no") );
            if (ipv6Enabled) {
                config.add("IPV6_ROUTER=no");
                config.add("IPV6_AUTOCONF=" + (ipv6AutoConf ? "yes" : "no") );
                config.add("DHCPV6C=" + (ipv6Dhcp ? "yes" : "no"));
                if (ipv6Addresses.size() > 0)
                    config.add("IPV6ADDR=" + ipv6Addresses.get(0));
                if (ipv6Addresses.size() > 1)
                    config.add("IPV6ADDR_SECONDARIES=" + concatConfigLines(" ", ipv6Addresses.subList(1, ipv6Addresses.size())).trim());
            }

            return config;
        }
    }
}
