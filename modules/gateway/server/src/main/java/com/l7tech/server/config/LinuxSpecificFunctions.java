package com.l7tech.server.config;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.NetworkInterface;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LinuxSpecificFunctions extends UnixSpecificFunctions {

    // - PUBLIC

    public LinuxSpecificFunctions(String osname) {
        super(osname);
    }

    // - PACKAGE

    @Override
    void doOsSpecificSetup() {
        networkConfigDir = "/etc/sysconfig/network-scripts";
        timeZonesDir = "/usr/share/zoneinfo/";
        keymapsDir = "/lib/kbd/keymaps/i386";
    }

    @Override
    NetworkingConfigurationBean.InterfaceConfig createNetworkConfig(NetworkInterface networkInterface, boolean includeIPV6) {
        //get the corresponding ifcfg file from /etc/sysconfig/network-scripts/
        String ifName = networkInterface.getName();
        File ifCfgFile = new File(getNetworkConfigurationDirectory(), "ifcfg-"+ifName);
        return parseConfigFile(networkInterface, ifCfgFile, includeIPV6);
    }

    public String getHostname() {
        Logger logger = Logger.getLogger(LinuxSpecificFunctions.class.getName());
        String command = "/bin/hostname";
        try {
            ProcResult result = ProcUtils.exec(command);
            return new String(result.getOutput());
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Error running " + command + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return "<unknown>";
        }
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(LinuxSpecificFunctions.class.getName());

    private NetworkingConfigurationBean.InterfaceConfig parseConfigFile(NetworkInterface networkInterface, File file, boolean includeIPV6) {
        if (!file.exists()) {
            return null;
        }

        NetworkingConfigurationBean.InterfaceConfig theNetConfig = null;
        BufferedReader reader = null;
        try {
            reader  = new BufferedReader(new FileReader(file));
            String bootProto = null;

            String netMask = null;
            String gateway = null;
            String line;
            while ((line = reader.readLine()) != null) {
                int equalsIndex = line.indexOf("=");
                //if this is a name=value pair
                if (equalsIndex != -1) {
                    String key = line.substring(0, equalsIndex);
                    String value = line.substring(equalsIndex + 1);
                    if (key.equals("BOOTPROTO")) bootProto = value;
                    else if (key.equals("NETMASK")) netMask = value;
                    else if (key.equals("GATEWAY")) gateway = value;
                }
            }
            //finished reading the file, now make the network config
            theNetConfig = NetworkingConfigurationBean.makeNetworkConfig(networkInterface, bootProto==null?NetworkingConfigurationBean.STATIC_BOOT_PROTO:bootProto, includeIPV6);
            if (StringUtils.isNotEmpty(netMask)) theNetConfig.setIpv4NetMask(netMask);
            if (StringUtils.isNotEmpty(gateway)) theNetConfig.setIpv4Gateway(gateway);

        } catch (FileNotFoundException e) {
            logger.severe("Error while reading configuration for " + networkInterface.getName() + ": " + e.getMessage());
        } catch (IOException e) {
            logger.severe("Error while reading configuration for " + networkInterface.getName() + ": " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(reader);
        }
        return theNetConfig;
    }
}
