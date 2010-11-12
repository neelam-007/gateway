package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.wizard.BaseConsoleStep;
import com.l7tech.server.config.wizard.ConfigurationWizard;
import com.l7tech.util.InetAddressUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:16:36 AM
 */
public class SystemConfigWizardNetworkingStep extends BaseConsoleStep<NetworkingConfigurationBean, NetworkingConfigurationCommand> {

    // - PUBLIC

    public SystemConfigWizardNetworkingStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        configBean = new NetworkingConfigurationBean(NETBEANNAME, "");
        configCommand = new NetworkingConfigurationCommand(configBean);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public boolean isShowNavigation() {
        return false;
    }

    @Override
    public boolean validateStep() {
        return true;
    }

    @Override
    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + EOL);

        try {
            doInterfaceConfigPrompts();
            doDefaultGatewayPrompt();
            doHostnamePrompt();
            doNameserversPrompt();
            storeInput();
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(SystemConfigWizardNetworkingStep.class.getName());

    // global network config
    private static final String TITLE = "Configure Network Interfaces";
    private static final String NETBEANNAME = "Network Interface Configuration";
    private static final String STEP_INFO = "This step lets you configure the network adapters on this machine" + EOL;

    // hostname
    private static final String HOSTNAME_PROMPT = EOL + "Enter the fully qualified hostname for this SSG: ";

    // nameserver(s)
    private static final String CONFIGURE_NAMESERVERS_PROMPT = EOL + "Would you like to configure the nameservers?";

    // interface(s)
    private static final String CONFIGURE_MORE_INTERFACES_PROMPT = EOL + "Would you like to configure another interface?";
    private static final String NEW_INTERFACE_NAME_PROMPT = EOL + "Please enter the name of the new interface (ex: eth5): ";
    private static final String CONFIGURE_IPV4 = EOL + "Would you like to configure IPv4 networking?";
    private static final String CONFIGURE_IPV6 = EOL + "Would you like to configure IPv6 networking?";
    private static final String CONFIGURE_IPV6_UNAVAILABLE = EOL + "IPv6 support is not available in the currently running configuration. Please enable it with the corresponding OS update patch.";
    private static final String CONFIGURE_IPV6_AUTO = EOL + "Enable IPv6 auto-configuration for this interface?";
    private static final String CONFIGURE_IPV6_DHCP = EOL + "Enable DHCPv6 for this interface?";
    private static final String CONFIGURE_IPV6_STATIC_FIRST = EOL + "Add static IPv6 address(es) for this interface?";
    private static final String CONFIGURE_IPV6_STATIC_MORE = EOL + "Add more static IPv6 address(es) for this interface?";

    private static final String HEADER_BOOTPROTO = "-- Boot Protocol --" + EOL;
    private static final String PROMPT_STATIC_NIC = NetworkingConfigurationBean.STATIC_BOOT_PROTO + " - all configuration is fixed" + EOL;
    private static final String PROMPT_DYNAMIC_NIC = NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO + " - all configuration is determined by the DHCP server" + EOL;

    // errors
    private static final String MISSING_NETMASK_MSG = "Missing Netmask.";
    private static final String INVALID_SOMETHING = "\"{0}\" is not a valid {1}";

    private static final Pattern interfaceNamePattern = Pattern.compile("\\S+");

    private void doInterfaceConfigPrompts() throws IOException, WizardNavigationException {
        NetworkingConfigurationBean.InterfaceConfig ifConfig;
        ifConfig = doSelectInterfacePrompts();
        doInterfaceConfigPrompts(ifConfig);
        saveConfig(ifConfig);
        if (getConfirmationFromUser(CONFIGURE_MORE_INTERFACES_PROMPT, "no")) {
            printText(EOL);
            doInterfaceConfigPrompts();
        }
    }

    private void doDefaultGatewayPrompt() throws IOException, WizardNavigationException {
        for (IpProtocol ipProtocol : EnumSet.allOf(IpProtocol.class)) {
            if (getConfirmationFromUser(ipProtocol.getConfigureDefaultGatewayPrompt(), "no")) {

                boolean isValid;
                List<String> errors;
                String gatewayIP;
                do {
                    gatewayIP = getData(
                        new String[]{ipProtocol.getDefaultGatewayPrompt()},
                        "",
                        (String[]) null,
                        null
                    );
                    errors = ipProtocol.validateAddress(gatewayIP);
                    isValid = errors.isEmpty();
                    if (!isValid) {
                        printText("Invalid default gateway IP address: " + EOL);
                        printText(errors);
                        printText(EOL);
                    }
                } while (!isValid);

                //get the gateway device
                List<NetworkingConfigurationBean.InterfaceConfig> allInterfaceConfigs = getInterfaces();
                List<String> promptList = new ArrayList<String>();

                int x = 1;
                for (NetworkingConfigurationBean.InterfaceConfig interfaceConfig : allInterfaceConfigs) {
                    String indexStr = String.valueOf(x);
                    x++;
                    String prompt = indexStr + ") " + interfaceConfig.describe();
                    promptList.add(prompt + EOL);
                }
                promptList.add("Please make a selection [1] : ");
                printText(ipProtocol.getDefaultGatewayDevicePrompt() + EOL);
                String[] allowedEntries = new String[x - 1];
                int allowedSize = x - 1;
                for (int index = 0; index < allowedSize; ++index) {
                    allowedEntries[index] = String.valueOf(index + 1);
                }

                String whichChoice = getData(promptList.toArray(new String[promptList.size()]), "1", allowedEntries, null);
                int choiceNum = Integer.parseInt(whichChoice);

                String theDevice = allInterfaceConfigs.get(choiceNum - 1).getInterfaceName();

                configBean.setDefaultGatewayIp(gatewayIP, ipProtocol);
                configBean.setDefaultGatewayDevice(theDevice, ipProtocol);
            }
        }
    }

    private void doHostnamePrompt() throws IOException, WizardNavigationException {
        String fqdn = getData(
                new String[] {HOSTNAME_PROMPT},
                "",
                (String[]) null,
                null
        );
        int firstDotPos = fqdn.indexOf(".");
        String justHostname = fqdn;
        String domainPart = "";

        if (firstDotPos > 0) {
            justHostname = fqdn.substring(0, firstDotPos);
            domainPart = fqdn.substring(firstDotPos+1);
        }

        configBean.setHostname(justHostname);
        configBean.setDomain(domainPart);
    }

    private void doNameserversPrompt() throws IOException, WizardNavigationException {
        if (getConfirmationFromUser(CONFIGURE_NAMESERVERS_PROMPT, "yes")) {
            configBean.addNameservers(getNameServers());
        }
    }

    private static List<String> validateNetMask(String netMask) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(netMask))
            message = MISSING_NETMASK_MSG;
        else if (!InetAddressUtil.isValidIpv4Address(netMask))
            message = MessageFormat.format(INVALID_SOMETHING, netMask , "netmask");

        if (message != null)
            errors.add("*** " + message + " ***" + EOL);

        return errors;
    }

    private void saveConfig(NetworkingConfigurationBean.InterfaceConfig whichConfig) {
        configBean.addNetworkingConfig(whichConfig);
    }

    private NetworkingConfigurationBean.InterfaceConfig doSelectInterfacePrompts() throws IOException, WizardNavigationException {
        List<NetworkingConfigurationBean.InterfaceConfig> allInterfaceConfigs = getInterfaces();
        List<String> promptList = new ArrayList<String>();

        int x = 1;
        for (NetworkingConfigurationBean.InterfaceConfig interfaceConfig : allInterfaceConfigs) {
            String indexStr = String.valueOf(x);
            x++;
            String prompt = indexStr + ") " + interfaceConfig.describe();
            promptList.add(prompt + EOL);
        }

        promptList.add(x + ") Configure an unlisted interface" + EOL);
        promptList.add("Please make a selection [1] : ");
        printText("Select the interface you wish to configure." + EOL);
        printText("Current configurations are shown in ()" + EOL);

        String[] allowedEntries = new String[x];
        for (int index=1; index <= x; ++index) {
            allowedEntries[index-1] = String.valueOf(index);
        }
        String whichChoice = getData(promptList.toArray(new String[promptList.size()]), "1", allowedEntries,null);

        int choiceNum = Integer.parseInt(whichChoice);
        NetworkingConfigurationBean.InterfaceConfig theConfig;

        if (choiceNum < 1 || choiceNum > allInterfaceConfigs.size()) {
            //creating a new interface
            theConfig = NetworkingConfigurationBean.makeNetworkConfig(null, null, false);
        } else {
            theConfig = allInterfaceConfigs.get(choiceNum -1);
        }
        theConfig.setNetworkConfig(configBean);
        theConfig.setDirtyFlag(true);
        return theConfig;
    }

    private void doInterfaceConfigPrompts(NetworkingConfigurationBean.InterfaceConfig ifConfig) throws IOException, WizardNavigationException {
        if (StringUtils.isEmpty(ifConfig.getInterfaceName())) {
            ifConfig.setInterfaceName(promptForNewInterfaceName());
        }

        if (getConfirmationFromUser(CONFIGURE_IPV4, "yes")) {
            ifConfig.setIpv4Enabled(true);
            printText(EOL);
            doIpv4ConfigPrompts(ifConfig);
        }

        if (getConfirmationFromUser(CONFIGURE_IPV6, "yes")) {
            if (new File("/proc/net/if_inet6").exists()) {
                ifConfig.setIpv6Enabled(true);
                printText(EOL);
                doIpv6ConfigPrompts(ifConfig);
            } else {
                printText(CONFIGURE_IPV6_UNAVAILABLE);
            }
        }
    }

    private void doIpv4ConfigPrompts(NetworkingConfigurationBean.InterfaceConfig ifConfig) throws IOException, WizardNavigationException {
        String bootProto = getBootProtocol(ifConfig);
        ifConfig.setIpv4BootProto(bootProto);
        if (StringUtils.equalsIgnoreCase(bootProto, NetworkingConfigurationBean.STATIC_BOOT_PROTO)) {
            ifConfig.setIpv4Address(getIpAddress(ifConfig, IpProtocol.IPv4), true);
            ifConfig.setIpv4Gateway(getGateway(ifConfig, IpProtocol.IPv4));
            ifConfig.setIpv4NetMask(getIpv4NetMask(ifConfig));
        }
    }

    private void doIpv6ConfigPrompts(NetworkingConfigurationBean.InterfaceConfig ifConfig) throws IOException, WizardNavigationException {
        ifConfig.setIpv6AutoConf(getConfirmationFromUser(CONFIGURE_IPV6_AUTO, "yes"));
        ifConfig.setIpv6Dhcp(getConfirmationFromUser(CONFIGURE_IPV6_DHCP, "no"));
        String prompt = CONFIGURE_IPV6_STATIC_FIRST;
        while (getConfirmationFromUser(prompt, "yes")) {
            ifConfig.addIpv6Address(getIpAddress(ifConfig, IpProtocol.IPv6));
            prompt = ifConfig.getIpv6Addresses().isEmpty() ? CONFIGURE_IPV6_STATIC_FIRST : CONFIGURE_IPV6_STATIC_MORE;
        }
    }

    private String promptForNewInterfaceName() throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            NEW_INTERFACE_NAME_PROMPT,
        };

        boolean duplicateName;
        String name;
        do {
            duplicateName = false;
            name = getData(prompts, "", interfaceNamePattern, "*** Please specify an interface name ***");
            List<NetworkingConfigurationBean.InterfaceConfig> existingConfigs = getInterfaces();
            for (NetworkingConfigurationBean.InterfaceConfig interfaceConfig : existingConfigs) {
                if (StringUtils.equals(name, interfaceConfig.getInterfaceName()))
                    duplicateName = true;
            }
            if (duplicateName) printText("*** The interface \"" + name + "\" already exists, please choose a different name ***\n");
        } while (duplicateName);
        return name;
    }

    private List<String> getNameServers() throws IOException, WizardNavigationException {
        String[] nameServers = null;
        boolean isValid;
        String nameserversline;
        do {
            nameserversline = getData(
                new String[]{
                    "Enter the nameservers (comma separated): "
                },
                configBean.getNameservesLine(),
                (String[]) null,
                null
            );

            if (StringUtils.isEmpty(nameserversline)) {
                printText("*** No nameserver(s) specified ***" + EOL);
                isValid = false;
            } else {
                nameServers = nameserversline.split(",");
                if (nameServers == null || nameServers.length == 0) {
                    printText("*** No nameserver(s) specified ***" + EOL);
                    isValid = false;
                } else {
                    for (int i = 0; i < nameServers.length; i++) {
                        nameServers[i] = nameServers[i].trim();
                    }
                    List<String> invalidNameServers = validateNameServers(nameServers);
                    isValid = invalidNameServers.isEmpty();
                }
            }
        } while (!isValid);
        return Arrays.asList(nameServers);
    }

    private List<String> validateNameServers(String[] nameServers) {
        List<String> errors = new ArrayList<String>();

        if (nameServers == null || nameServers.length == 0) {
            errors.add("*** No nameserver(s) specified ***");
        } else {
            nameservers_for:
            for (String ns : nameServers) {
                for(IpProtocol ipProtocol : EnumSet.allOf(IpProtocol.class)) {
                    if (ipProtocol.validateAddress(ns).isEmpty())
                        continue nameservers_for;
                }
                errors.add(ns);
            }
        }

        if (!errors.isEmpty()) {
            printText("*** The following nameserver entries are not valid ***" + EOL);
            for (String invalidNs : errors) {
               printText("\t" + invalidNs + EOL);
            }
        }

        return errors;
    }

    private String getGateway(NetworkingConfigurationBean.InterfaceConfig ifConfig, IpProtocol ipProtocol) throws IOException, WizardNavigationException {
        String interfaceName = ifConfig.getInterfaceName();
        String prompt = "Enter the default " + ipProtocol + " gateway for interface \"" + interfaceName + "\" (optional): ";
        String gateway;
        boolean isValid;
        List<String> errors;
        do {
            gateway = getData(new String[] {prompt}, "", (String[]) null,null);
            errors = ipProtocol.validateAddress(gateway);
            isValid = StringUtils.isEmpty(gateway) || errors.isEmpty();
            if (!isValid) {
                printText("Invalid gateway IP address: " + EOL);
                printText(errors);
            }
        } while (!isValid);

        return gateway;
    }


    private String getIpAddress(NetworkingConfigurationBean.InterfaceConfig ifConfig, IpProtocol ipProtocol) throws IOException, WizardNavigationException {
        String interfaceName = ifConfig.getInterfaceName();
        List<InterfaceAddress> addresses = ifConfig.getInterfaceAddresses();
        String currentFirstAddress = null;
        if (!addresses.isEmpty()) {
            List<InetAddress> matchingAddresses = new ArrayList<InetAddress>();
            for (InterfaceAddress address : addresses) {
                InetAddress addr = address.getAddress();
                if (ipProtocol.validateAddress(InetAddress.getByAddress(addr.getAddress()).getHostAddress()).isEmpty()) {
                    matchingAddresses.add(addr);
                }
            }

            if (!matchingAddresses.isEmpty()) {
                if (ipProtocol == IpProtocol.IPv6) {
                    for (InetAddress addr : matchingAddresses) {
                        if (addr.isLinkLocalAddress()) continue;
                        currentFirstAddress = InetAddress.getByAddress(addr.getAddress()).getHostAddress();
                        break;
                    }
                }

                if (currentFirstAddress == null)
                    currentFirstAddress = InetAddress.getByAddress(matchingAddresses.get(0).getAddress()).getHostAddress();
            }
        }

        boolean isValid;
        List<String> errors;
        String defaultAddress = StringUtils.isNotEmpty(currentFirstAddress)?currentFirstAddress:"";
        // todo: clarify expected format (prefix for ipv6)
        String prompt = EOL + "Enter " + ipProtocol + " address for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(defaultAddress))
            prompt += " [" + defaultAddress + "] ";
        prompt += ": ";

        String newAddress;
        do {
            newAddress = getData(new String[] {prompt}, defaultAddress, (String[]) null,null);
            errors = ipProtocol.validateAddress(newAddress);

            isValid = errors.isEmpty();
            if (!isValid) {
                printText("Invalid " + ipProtocol + " address: " + EOL);
                printText(errors);
            }
        } while (!isValid);

        return newAddress;
    }

    private String getIpv4NetMask(NetworkingConfigurationBean.InterfaceConfig netConfig) throws IOException, WizardNavigationException {
        String interfaceName = netConfig.getInterfaceName();
        String netMask = netConfig.getIpv4NetMask();

        String prompt = "Enter the netmask for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(netMask)) prompt += " [" + netMask + "] ";
        prompt += ": ";

        boolean isValid;
        List<String> errors;

        String defaultNetMask = StringUtils.isNotEmpty(netMask)?netMask:"";
        do {
            netMask = getData(new String[] {prompt}, defaultNetMask, (String[]) null,null);
            errors = validateNetMask(netMask);

            isValid = errors.isEmpty();
            if (!isValid)
                printText(errors);

        } while (!isValid);

        return netMask;
    }

    private String getBootProtocol(NetworkingConfigurationBean.InterfaceConfig netConfig) throws IOException, WizardNavigationException {
        String whichInterface = netConfig.getInterfaceName();
        String bootProto = netConfig.getIpv4BootProto();

        String defaultValue = (StringUtils.isEmpty(bootProto) || StringUtils.equalsIgnoreCase(NetworkingConfigurationBean.STATIC_BOOT_PROTO, bootProto)?"1":"2");

        String protoQuestion = "What is the boot protocol for \"" + whichInterface + "\" ?";

        if (StringUtils.isNotEmpty(bootProto))
            protoQuestion += " (Currently " + bootProto + ")";

        String[] prompts = new String[] {
            protoQuestion + EOL,
            "1) " + PROMPT_STATIC_NIC,
            "2) " + PROMPT_DYNAMIC_NIC,
            "Please make a selection [" + defaultValue + "] : "
        };

        printText(EOL + HEADER_BOOTPROTO);

        String input = getData(prompts, defaultValue, new String[]{"1","2"},null);
        bootProto = StringUtils.equals("1", input)?NetworkingConfigurationBean.STATIC_BOOT_PROTO:NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO;
        return bootProto;
    }

    private List<NetworkingConfigurationBean.InterfaceConfig> getInterfaces() {
        List<NetworkingConfigurationBean.InterfaceConfig> allConfigs = configBean.getAllNetworkInterfaces();
        List<NetworkingConfigurationBean.InterfaceConfig> configurableConfigs = new LinkedList<NetworkingConfigurationBean.InterfaceConfig>();
        for (NetworkingConfigurationBean.InterfaceConfig aConfig : allConfigs) {
            if (!aConfig.isVirtual())
                configurableConfigs.add(aConfig);
        }
        return configurableConfigs;
    }
}