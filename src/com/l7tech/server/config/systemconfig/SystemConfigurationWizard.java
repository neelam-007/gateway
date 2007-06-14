package com.l7tech.server.config.systemconfig;

import com.l7tech.common.util.HexUtils;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.server.config.ui.console.ConsoleWizardUtils;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
            Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = allInterfaces.nextElement();
                if (!networkInterface.isLoopback()) {
                    printNetworkInterfaceInfo(networkInterface);
                }
            }
        } catch (SocketException e) {
            utils.printText("Error while determining the interface information for this machine:" + e.getMessage() + ConsoleWizardUtils.EOL_CHAR);
        }
    }

    private void printNetworkInterfaceInfo(NetworkInterface networkInterface) {
        ConsoleWizardUtils utils = getWizardUtils();
        try {
            utils.printText(MessageFormat.format("Interface \"{0}\"", networkInterface.getDisplayName()) + ConsoleWizardUtils.EOL_CHAR);
            utils.printText("Details:" + ConsoleWizardUtils.EOL_CHAR);
            utils.printText(MessageFormat.format("\tName: {0}", networkInterface.getName()) + ConsoleWizardUtils.EOL_CHAR);
            byte[] hardwareAdress = networkInterface.getHardwareAddress();
            if (hardwareAdress != null) {
                String mac = HexUtils.hexDump(networkInterface.getHardwareAddress()).toUpperCase();
                utils.printText("\tHardware Address (MAC): " + getFormattedMac(mac));
            }
            utils.printText("\tAddresses:" + ConsoleWizardUtils.EOL_CHAR);
            List<InterfaceAddress> interfaceAddresses   = networkInterface.getInterfaceAddresses();
            for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                InetAddress address = interfaceAddress.getAddress();
                short maskLength = interfaceAddress.getNetworkPrefixLength();
                utils.printText("\t\t" + address.getHostAddress() + "/" + maskLength + ConsoleWizardUtils.EOL_CHAR);
            }
        } catch (SocketException e) {
            utils.printText("Error while determining the interface information for this machine:" + e.getMessage() + ConsoleWizardUtils.EOL_CHAR);
        }
    }

    private String getFormattedMac(String mac) {
        String formattedMac = mac;
        //format the mac with colons
        Pattern macPattern = Pattern.compile("(\\w\\w)(\\w\\w)(\\w\\w)(\\w\\w)(\\w\\w)(\\w\\w)");
        Matcher macMatcher = macPattern.matcher(mac);

        if (macMatcher.matches()) {
            formattedMac = MessageFormat.format("{0}:{1}:{2}:{3}:{4}:{5}",
                macMatcher.group(1),
                macMatcher.group(2),
                macMatcher.group(3),
                macMatcher.group(4),
                macMatcher.group(5),
                macMatcher.group(6));
        }
        formattedMac += ConsoleWizardUtils.EOL_CHAR;
        return formattedMac;
    }
}
