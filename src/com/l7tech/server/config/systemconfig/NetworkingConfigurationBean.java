package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
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

    public static NetworkConfig makeNetworkConfig(String interfaceName, String bootProto) {
        return new NetworkConfig(interfaceName, bootProto);
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
    }

    public List<NetworkConfig> getNetworkingConfigurations() {
        return networkingConfigs;
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
        if (getOsFunctions().isLinux()) {
            getExistingInterfacesLinux();
        } else if (getOsFunctions().isWindows()){
            getExistingInterfacesWindows();            
        }
    }

    private void getExistingInterfacesWindows() {
        System.out.println("getting interfaces on windows");
    }

    private void getExistingInterfacesLinux() {
        if (networkingConfigs == null) {
            logger.info("Determining existing interface information.");
            networkingConfigs = new ArrayList<NetworkConfig>();

            File parentDir = new File(getOsFunctions().getNetworkConfigurationDirectory());
            File[] configFiles = parentDir.listFiles(new FilenameFilter() {
                    public boolean accept(File file, String s) {
                        return s.toLowerCase().startsWith("ifcfg-") && !s.toLowerCase().equals("ifcfg-lo");
                    }
            });

            if (configFiles != null && configFiles.length != 0) {
                for (File file : configFiles) {
                    NetworkConfig theConfig = parseConfigFile(file);
                    if (theConfig != null) {
                        //in case the file name is off, like it has some extra characters at the end of it, or has an extension
                        //we are only interested in the configurations from files with the pattern:
                        //  ifcfg-<interface>
                        String ifName = theConfig.getInterfaceName();
                        if (ifName != null && file.getName().endsWith(ifName)) {
                            networkingConfigs.add(theConfig);
                            logger.info("found existing configuration for interface: " + theConfig.describe());
                        }
                    }
                }
            }
        }
    }

    private NetworkingConfigurationBean.NetworkConfig parseConfigFile(File file) {
        NetworkingConfigurationBean.NetworkConfig theNetConfig = null;

        String justFileName = file.getName();
        int dashIndex = justFileName.indexOf("-");
        if (dashIndex == -1) return null;

        String interfaceNameFromFileName = justFileName.substring(dashIndex + 1);
        BufferedReader reader = null;

        try {
            reader  = new BufferedReader(new FileReader(file));
            String bootProto = null;
            String interfaceName = null;
            String ipAddress = null;
            String netMask = null;
            String gateway = null;

            String line;
            while ((line = reader.readLine()) != null) {
                int equalsIndex = line.indexOf("=");
                //if this is a name=value pair
                if (equalsIndex != -1) {
                    String key = line.substring(0, equalsIndex);
                    String value = line.substring(equalsIndex + 1);
                    if (key.equals("DEVICE")) interfaceName = value;
                    else if (key.equals("BOOTPROTO")) bootProto = value;
                    else if (key.equals("IPADDR")) ipAddress = value;
                    else if (key.equals("NETMASK")) netMask = value;
                    else if (key.equals("GATEWAY")) gateway = value;
                }
            }
            //finished reading the file, now make the network config
            theNetConfig = NetworkingConfigurationBean.makeNetworkConfig(interfaceName, bootProto);
            if (StringUtils.isNotEmpty(ipAddress)) theNetConfig.setIpAddress(ipAddress);
            if (StringUtils.isNotEmpty(netMask)) theNetConfig.setNetMask(netMask);
            if (StringUtils.isNotEmpty(gateway)) theNetConfig.setGateway(gateway);

        } catch (FileNotFoundException e) {
            logger.severe("Error while reading configuration for " + interfaceNameFromFileName + ": " + e.getMessage());
        } catch (IOException e) {
            logger.severe("Error while reading configuration for " + interfaceNameFromFileName + ": " + e.getMessage());
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {}
        }
        return theNetConfig;
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
        private String ipAddress;
        private String netMask;
        private String gateway;
        private String[] nameservers;
        private boolean dirtyFlag;

        public NetworkConfig(String interfaceName, String bootProto) {
            this.interfaceName = interfaceName;
            this.bootProto = bootProto;
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
    }

}
