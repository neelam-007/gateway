package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.server.config.ui.console.ConsoleWizardUtils;
import com.l7tech.common.util.HexUtils;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.text.MessageFormat;

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
        String eol = ConsoleWizardUtils.EOL_CHAR;
        try {
            Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = allInterfaces.nextElement();
                utils.printText(MessageFormat.format("Interface \"{0}\"", networkInterface.getDisplayName()) + eol);
                utils.printText("Details:" + eol);
                utils.printText(MessageFormat.format("\tName: {0}", networkInterface.getName()) + eol);
                byte[] hardwareAdress = networkInterface.getHardwareAddress();
                if (hardwareAdress != null)
                    utils.printText(MessageFormat.format("\tHardware Address: {0}", HexUtils.hexDump(networkInterface.getHardwareAddress())) + eol);
                utils.printText("\tAddresses:" + eol);
                List<InterfaceAddress> interfaceAddresses   = networkInterface.getInterfaceAddresses();
                for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                    InetAddress address = interfaceAddress.getAddress();
                    short maskLength = interfaceAddress.getNetworkPrefixLength();
                    utils.printText("\t\t" + address.getHostAddress() + "/" + maskLength + eol);
                }
            }
        } catch (SocketException e) {
            utils.printText("Error while determining the IP Addresses for this machine:" + e.getMessage() + eol);
        }
    }
}
