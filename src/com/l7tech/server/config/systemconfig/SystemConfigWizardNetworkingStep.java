package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:16:36 AM
 */
public class SystemConfigWizardNetworkingStep extends BaseConsoleStep {

    private static final Logger logger = Logger.getLogger(SystemConfigWizardNetworkingStep.class.getName());

    private static final String STEP_INFO = "This step lets you configure the network adapters on this machine" + getEolChar();
    private static final String HEADER_BOOTPROTO = "-- Boot Protocol --" + getEolChar();
    private static final String PROMPT_STATIC_NIC = NetworkingConfigurationBean.STATIC_BOOT_PROTO + " - all configuration is fixed" + getEolChar();
    private static final String PROMPT_DYNAMIC_NIC = NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO + " - all configuration is determined by the DHCP server" + getEolChar();
    private static final String TITLE = "Configure Network Interfaces";

    private static final String NETBEANNAME = "Network Interface Configuration";

    private static final String HOSTNAME_PROMPT = "Enter the hostname for this SSG: ";
    private static final String MISSING_IP_ADDRESS_MSG = "Missing IP Address";
    private static final String MISSING_NETMASK_MSG = "Missing Netmask.";
    private static final String MISSING_GATEWAY_MSG = "Missing Gateway.";
    private static final String CONFIGURE_MORE_INTERFACES_PROMPT = "Would you like to configure another interface? [no]: ";
    private static final String NEW_INTERFACE_NAME_PROMPT = "Please enter the name of the new interface (ex: eth5): ";
    private static final String CONFIGURE_NAMESERVERS_PROMPT = "Would you like to configure the nameservers for this interface? [no]";

    private NetworkingConfigurationBean netBean;
    private static final Pattern interfaceNamePattern = Pattern.compile("\\S+");

    public SystemConfigWizardNetworkingStep(ConfigurationWizard parentWiz) {
        super(parentWiz);

        configBean = new NetworkingConfigurationBean(NETBEANNAME, "");
        configCommand = new NetworkingConfigurationCommand(configBean);
        netBean = (NetworkingConfigurationBean) configBean;
    }

    public boolean validateStep() {
        return true;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + getEolChar());

        try {
            doNetConfigPrompts();
            doHostnamePrompt();
            storeInput();

        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    private void doNetConfigPrompts() throws IOException, WizardNavigationException {
        NetworkingConfigurationBean.NetworkConfig whichConfig;

        whichConfig = doSelectInterfacePrompts();
        whichConfig = doConfigurationPrompts(whichConfig);
        saveConfig(whichConfig);
        if (doRepeatConfiguration()) {
            printText(getEolChar());
            doNetConfigPrompts();
        }
    }

    private void doHostnamePrompt() throws IOException, WizardNavigationException {
        String newHostname = getData(
                new String[] {HOSTNAME_PROMPT},
                ""
        );

        netBean.setHostname(newHostname);
    }

    private List<String> validateIpAddress(String ipAddress) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(ipAddress))
            message = MISSING_IP_ADDRESS_MSG;
        else if (!isValidIpAddress(ipAddress, false))
            message = ipAddress + "\" is not a valid IP Address";

        if (message != null)
            errors.add("*** " + message + " ***" + getEolChar());

        return errors;
    }

    private List<String> validateNetMask(String netMask) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(netMask))
            message = MISSING_NETMASK_MSG;
        else if (!isValidIpAddress(netMask, true))
            message = netMask + "\" is not a valid netmask ";

        if (message != null)
            errors.add("*** " + message + " ***" + getEolChar());

        return errors;
    }

    private List<String> validateGateway(String gateway) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(gateway))
            message = MISSING_GATEWAY_MSG;
        else if (!isValidIpAddress(gateway, false))
            message = gateway + "\" is not a valid gateway address";

        if (message != null)
            errors.add("*** " + message + " ***" + getEolChar());

        return errors;
    }

    private List<String> validateNameServers(String[] nameServers) {
        if (nameServers == null || nameServers.length == 0) return null;
        List<String> errors = new ArrayList<String>();
        for (String ns : nameServers) {
            if (!isValidIpAddress(ns, false))
                errors.add(ns);
        }

        if (!errors.isEmpty()) {
            printText("*** The following nameserver entries are not valid ***" + getEolChar());
            for (String invalidNs : errors) {
               printText("\t" + invalidNs + getEolChar());
            }
        }

        return errors;
    }

    private boolean isValidIpAddress(String address, boolean isNetworkAddressAllowed) {
        return consoleWizardUtils.isValidIpAddress(address, isNetworkAddressAllowed);
    }

    private boolean doRepeatConfiguration() throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
            CONFIGURE_MORE_INTERFACES_PROMPT,
        };
        String doItAgain = getData(prompts, "no");

        return (StringUtils.equalsIgnoreCase(doItAgain, "yes") || StringUtils.equalsIgnoreCase(doItAgain, "y") ) ;
    }

    private void saveConfig(NetworkingConfigurationBean.NetworkConfig whichConfig) {
        netBean.addNetworkingConfig(whichConfig);
    }

    private NetworkingConfigurationBean.NetworkConfig doSelectInterfacePrompts() throws IOException, WizardNavigationException {

        List<NetworkingConfigurationBean.NetworkConfig> networkConfigs = getInterfaces();

        List<String> promptList = new ArrayList<String>();

        int x = 1;
        for (NetworkingConfigurationBean.NetworkConfig networkConfig : networkConfigs) {
            String indexStr = String.valueOf(x++);
            String prompt = indexStr + ") " + networkConfig.describe();
            promptList.add(prompt + getEolChar());
        }

        promptList.add(x + ") Configure an unlisted interface" + getEolChar());

        promptList.add("Please make a selection [1] : ");

        printText("Select the Interface you wish to configure." + getEolChar());
        printText("Current configurations are shown in ()" + getEolChar());

        String[] allowedEntries = new String[x];
        for (int index=1; index <= x; ++index) {
            allowedEntries[index-1] = String.valueOf(index);
        }
        String whichChoice = getData(promptList, "1", allowedEntries);

        int choiceNum = Integer.parseInt(whichChoice);
        NetworkingConfigurationBean.NetworkConfig theConfig;

        if (choiceNum < 1 || choiceNum > networkConfigs.size()) {
            //creating a new interface
            theConfig = NetworkingConfigurationBean.makeNetworkConfig(null, null);
        } else {
            theConfig = networkConfigs.get(choiceNum -1);
        }

        return theConfig;
    }

    private NetworkingConfigurationBean.NetworkConfig doConfigurationPrompts(NetworkingConfigurationBean.NetworkConfig whichConfig) throws IOException, WizardNavigationException {

        if (StringUtils.isEmpty(whichConfig.getInterfaceName())) {
            whichConfig.setInterfaceName(promptForNewInterfaceName(whichConfig));
        }

        String bootProto = getBootProtocol(whichConfig);
        whichConfig.setBootProto(bootProto);

        if (StringUtils.equalsIgnoreCase(bootProto, NetworkingConfigurationBean.STATIC_BOOT_PROTO)) {

            whichConfig.setIpAddress(getIpAddress(whichConfig));
            whichConfig.setGateway(getGateway(whichConfig));
            whichConfig.setNetMask(getNetMask(whichConfig));
            whichConfig.setNameServer(getNameServer(whichConfig.getNameServers(), whichConfig.getInterfaceName()));
        }

        return whichConfig;
    }

    private String promptForNewInterfaceName(NetworkingConfigurationBean.NetworkConfig theConfig) throws IOException, WizardNavigationException {

        String[] prompts = new String[] {
            NEW_INTERFACE_NAME_PROMPT,
        };


        boolean duplicateName;
        String name = "";
        do {
            duplicateName = false;
            name = getData(prompts, "", interfaceNamePattern, "*** Please specify an interface name ***");
            List<NetworkingConfigurationBean.NetworkConfig> existingConfigs = getInterfaces();
            for (NetworkingConfigurationBean.NetworkConfig networkConfig : existingConfigs) {
                if (StringUtils.equals(name, networkConfig.getInterfaceName()))
                    duplicateName = true;
            }
            if (duplicateName) printText("*** The interface \"" + name + "\" already exists, please choose a different name ***\n");
        } while (duplicateName);
        return name;
    }

    private String[] getNameServer(String[] currentNameServers, String interfaceName) throws IOException, WizardNavigationException {
        boolean hasCurrentNameServers = (currentNameServers != null && currentNameServers.length != 0);
        String[] nameServers = null;

        String shouldConfigNameServers = getData(
                new String[]{CONFIGURE_NAMESERVERS_PROMPT},
                "no"
        );

        String defaultNameserversLine = null;
        boolean isFirst = true;
        if (hasCurrentNameServers) {
            for (String s : currentNameServers) {
                defaultNameserversLine += (isFirst?"":", ") + s;
                if (isFirst)
                    isFirst = false;
            }
        }

        boolean isValid;
        String nameserversline;
        if (isYes(shouldConfigNameServers)) {
                do {
                    nameserversline = getData(
                            new String[] {"Enter the nameservers to be associated with the \"" + interfaceName + "\" interface (comma separated): "},
                            defaultNameserversLine
                        );

                if (StringUtils.isEmpty(nameserversline)) {
                    printText("*** No nameserver(s) specified ***" + getEolChar());
                    isValid = false;
                } else {
                    nameServers = nameserversline.split(",");
                    for (int i = 0; i < nameServers.length; i++) {
                        nameServers[i] = nameServers[i].trim();
                    }
                    List<String> invalidNameServers = validateNameServers(nameServers);
                    isValid = invalidNameServers.isEmpty();
                }
            } while (!isValid);
        }
        return nameServers;
    }

    private String getGateway(NetworkingConfigurationBean.NetworkConfig whichConfig) throws IOException, WizardNavigationException {

        String interfaceName = whichConfig.getInterfaceName();
        String gateway = whichConfig.getGateway();

        String prompt = "Enter the default gateway for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(gateway)) prompt += " [" + gateway + "] ";
        prompt += ": ";

        boolean isValid;
        List<String> errors;

        do {
            gateway = getData(new String[] {prompt}, gateway);
            errors = validateGateway(gateway);

            isValid = errors.isEmpty();
            if (!isValid) printText(errors);
        } while (!isValid);

        return gateway;
    }


    private String getIpAddress(NetworkingConfigurationBean.NetworkConfig netConfig) throws IOException, WizardNavigationException {
        String interfaceName = netConfig.getInterfaceName();
        String ipAddress = netConfig.getIpAddress();


        String prompt = "Enter the IP for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(ipAddress)) prompt += " [" + ipAddress + "] ";
        prompt += ": ";

        boolean isValid;
        List<String> errors;
        String defaultAddress = StringUtils.isNotEmpty(ipAddress)?ipAddress:"";

        do {
            ipAddress = getData(new String[] {prompt}, defaultAddress);
            errors = validateIpAddress(ipAddress);

            isValid = errors.isEmpty();
            if (!isValid) printText(errors);
        } while (!isValid);

        return ipAddress;
    }

    private String getNetMask(NetworkingConfigurationBean.NetworkConfig netConfig) throws IOException, WizardNavigationException {
        String interfaceName = netConfig.getInterfaceName();
        String netMask = netConfig.getNetMask();

        String prompt = "Enter the netmask for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(netMask)) prompt += " [" + netMask + "] ";
        prompt += ": ";

        boolean isValid;
        List<String> errors;

        String defaultNetMask = StringUtils.isNotEmpty(netMask)?netMask:"";
        do {
            netMask = getData(new String[] {prompt}, defaultNetMask);
            errors = validateNetMask(netMask);

            isValid = errors.isEmpty();
            if (!isValid)
                printText(errors);

        } while (!isValid);

        return netMask;
    }

    private String getBootProtocol(NetworkingConfigurationBean.NetworkConfig netConfig) throws IOException, WizardNavigationException {
        String whichInterface = netConfig.getInterfaceName();
        String bootProto = netConfig.getBootProto();

        String defaultValue = (StringUtils.isEmpty(bootProto) || StringUtils.equalsIgnoreCase(NetworkingConfigurationBean.STATIC_BOOT_PROTO, bootProto)?"1":"2");

        String protoQuestion = "What is the boot protocol for \"" + whichInterface + "\" ?";

        if (StringUtils.isNotEmpty(bootProto))
            protoQuestion += " (Currently " + bootProto + ")";

        String[] prompts = new String[] {
            protoQuestion + getEolChar(),
            "1) " + PROMPT_STATIC_NIC,
            "2) " + PROMPT_DYNAMIC_NIC,
            "Please make a selection [" + defaultValue + "] : "
        };

        printText(getEolChar() + HEADER_BOOTPROTO);

        String input = getData(prompts, defaultValue);
        bootProto = StringUtils.equals("1", input)?NetworkingConfigurationBean.STATIC_BOOT_PROTO:NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO;
        return bootProto;
    }

    private List<NetworkingConfigurationBean.NetworkConfig> getInterfaces() {
        return netBean.getAllNetworkInterfaces();
    }

    public String getTitle() {
        return TITLE;
    }

    public boolean isShowNavigation() {
        return false;
    }

}
