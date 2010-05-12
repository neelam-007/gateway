package com.l7tech.server.config.systemconfig;

import com.l7tech.util.HexUtils;
import com.l7tech.server.config.wizard.ConfigurationWizard;
import com.l7tech.server.config.wizard.ConsoleWizardUtils;
import com.l7tech.server.config.Utilities;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.List;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:12:55 AM
 */
public class SystemConfigurationWizard extends ConfigurationWizard {
    public SystemConfigurationWizard(InputStream in, PrintStream out) {
        super(in, out);

        //this will throw if we are on windows so we'll fail early
        osFunctions.getNetworkConfigurationDirectory();
    }

    public void printConfigOnly() {
        ConsoleWizardUtils utils = getWizardUtils();
        try {
            List<NetworkingConfigurationBean.NetworkConfig> netConfigs = osFunctions.getNetworkConfigs(false, true);
            if ( !netConfigs.isEmpty() ) {
                for (NetworkingConfigurationBean.NetworkConfig netConfig : netConfigs) {
                    NetworkInterface networkInterface = netConfig.getNetworkInterface();
                    if (!networkInterface.isLoopback()) {
                        printNetworkConfig(netConfig);
                    }
                }
            } else {
                utils.printText("No interfaces found." + ConsoleWizardUtils.EOL_CHAR);
            }
        } catch (SocketException e) {
            utils.printText("Error while determining the interface information for this machine:" + e.getMessage() + ConsoleWizardUtils.EOL_CHAR);
        }
    }

    private void printNetworkConfig(NetworkingConfigurationBean.NetworkConfig networkConfig) {
        ConsoleWizardUtils utils = getWizardUtils();
        try {
            utils.printText(MessageFormat.format("Interface \"{0}\"", networkConfig.getInterfaceName()) + ConsoleWizardUtils.EOL_CHAR);
            utils.printText("Details:" + ConsoleWizardUtils.EOL_CHAR);
            utils.printText("\tBoot Protocol: " + networkConfig.getBootProto().toUpperCase() +  ConsoleWizardUtils.EOL_CHAR);            
            utils.printText("\tCurrently Online: " + (networkConfig.getNetworkInterface().isUp()?"yes":"no") +  ConsoleWizardUtils.EOL_CHAR);
            byte[] hardwareAdress = networkConfig.getHardwareAddress();
            if (hardwareAdress != null) {
                String mac = HexUtils.hexDump(networkConfig.getHardwareAddress()).toUpperCase();
                utils.printText("\tHardware Address (MAC): " + Utilities.getFormattedMac(mac) + ConsoleWizardUtils.EOL_CHAR);
            }
            utils.printText("\tAddresses:" + ConsoleWizardUtils.EOL_CHAR);
            List<InterfaceAddress> interfaceAddresses   = networkConfig.getInterfaceAddresses();
            for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                InetAddress address = interfaceAddress.getAddress();
                short maskLength = interfaceAddress.getNetworkPrefixLength();
                utils.printText("\t\t" + address.getHostAddress() + "/" + maskLength + ConsoleWizardUtils.EOL_CHAR);
            }
        } catch (SocketException e) {
            utils.printText("Error while determining the interface information for this machine:" + e.getMessage() + ConsoleWizardUtils.EOL_CHAR);
        }
    }
}
