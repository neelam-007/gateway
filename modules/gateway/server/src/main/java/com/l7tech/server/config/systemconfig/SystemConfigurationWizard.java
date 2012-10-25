package com.l7tech.server.config.systemconfig;

import com.l7tech.util.HexUtils;
import com.l7tech.server.config.wizard.ConfigurationWizard;
import com.l7tech.server.config.wizard.ConsoleWizardUtils;
import com.l7tech.server.config.Utilities;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.List;
import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:12:55 AM
 */
public class SystemConfigurationWizard extends ConfigurationWizard {
    public SystemConfigurationWizard() {
        //this will throw if we are on windows so we'll fail early
        osFunctions.getNetworkConfigurationDirectory();
    }

    public void printConfigOnly() {
        try {
            List<NetworkingConfigurationBean.InterfaceConfig> netConfigs = osFunctions.getNetworkConfigs(false);
            if ( !netConfigs.isEmpty() ) {
                for (NetworkingConfigurationBean.InterfaceConfig netConfig : netConfigs) {
                    NetworkInterface networkInterface = netConfig.getNetworkInterface();
                    if (!networkInterface.isLoopback()) {
                        printNetworkConfig(netConfig);
                    }
                }
            } else {
                ConsoleWizardUtils.printText("No interfaces found." + EOL);
            }
        } catch (SocketException e) {
            ConsoleWizardUtils.printText("Error while determining the interface information for this machine:" + e.getMessage() + EOL);
        }
    }

    private void printNetworkConfig(NetworkingConfigurationBean.InterfaceConfig interfaceConfig) {
        try {
            ConsoleWizardUtils.printText("Hostname: " + osFunctions.getHostname() + EOL);
            ConsoleWizardUtils.printText(MessageFormat.format("Interface \"{0}\"", interfaceConfig.getInterfaceName()) + EOL);
            ConsoleWizardUtils.printText("Details:" + EOL);
            ConsoleWizardUtils.printText("\tBoot Protocol: " + interfaceConfig.getIpv4BootProto().toUpperCase() +  EOL);
            ConsoleWizardUtils.printText("\tCurrently Online: " + (interfaceConfig.getNetworkInterface().isUp()?"yes":"no") +  EOL);
            byte[] hardwareAddress = interfaceConfig.getHardwareAddress();
            if (hardwareAddress != null) {
                String mac = HexUtils.hexDump(interfaceConfig.getHardwareAddress()).toUpperCase();
                ConsoleWizardUtils.printText("\tHardware Address (MAC): " + Utilities.getFormattedMac(mac) + EOL);
            }
            ConsoleWizardUtils.printText("\tAddresses:" + EOL);
            List<InterfaceAddress> interfaceAddresses   = interfaceConfig.getInterfaceAddresses();
            for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                InetAddress address = interfaceAddress.getAddress();
                short maskLength = interfaceAddress.getNetworkPrefixLength();
                ConsoleWizardUtils.printText("\t\t" + address.getHostAddress() + "/" + maskLength + EOL);
            }
            ConsoleWizardUtils.printText("\tFQDNs:" + EOL);
            for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                InetAddress address = interfaceAddress.getAddress();
                String hostname = address.getCanonicalHostName();
                // if we couldn't resolve the hostname
                // this can be due to the SecurityManager not allowing it or because no record exists (among other reasons)
                if(!hostname.equals(address.getHostAddress())) {
                    ConsoleWizardUtils.printText("\t\t" + hostname + EOL);
                    } else {
                    ConsoleWizardUtils.printText("\t\t" + "Could not find name for " + address.getHostAddress() + EOL);
                }
            }
        } catch (SocketException e) {
            ConsoleWizardUtils.printText("Error while determining the interface information for this machine:" + e.getMessage() + EOL);
        }
    }
}
