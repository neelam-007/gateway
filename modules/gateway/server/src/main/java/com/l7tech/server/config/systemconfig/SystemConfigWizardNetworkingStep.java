package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.common.io.InetAddressUtil;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.InterfaceAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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

    private static final String HOSTNAME_PROMPT = "Enter the fully qualified hostname for this SSG: ";
    private static final String MISSING_IP_ADDRESS_MSG = "Missing IP Address";
    private static final String MISSING_NETMASK_MSG = "Missing Netmask.";
    private static final String MISSING_GATEWAY_MSG = "Missing Gateway.";
    private static final String CONFIGURE_MORE_INTERFACES_PROMPT = "Would you like to configure another interface?";
    private static final String NEW_INTERFACE_NAME_PROMPT = "Please enter the name of the new interface (ex: eth5): ";
    private static final String CONFIGURE_NAMESERVERS_PROMPT = "Would you like to configure the nameservers for this interface?";
    private static final String INVALID_SOMETHING = "\"{0}\" is not a valid {1}";

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

        netBean.setHostname(justHostname);
        netBean.setDomain(domainPart);
    }

    private List<String> validateIpAddress(String ipAddress) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(ipAddress))
            message = MISSING_IP_ADDRESS_MSG;
        else if (!isValidIpAddress(ipAddress))
            message = MessageFormat.format(INVALID_SOMETHING, ipAddress , "IP Address");

        if (message != null)
            errors.add("*** " + message + " ***" + getEolChar());

        return errors;
    }

    private List<String> validateNetMask(String netMask) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(netMask))
            message = MISSING_NETMASK_MSG;
        else if (!isValidIpAddress(netMask))
            message = MessageFormat.format(INVALID_SOMETHING, netMask , "netmask");

        if (message != null)
            errors.add("*** " + message + " ***" + getEolChar());

        return errors;
    }

    private List<String> validateGateway(String gateway) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(gateway))
            message = MISSING_GATEWAY_MSG;
        else if (!isValidIpAddress(gateway))
            message = MessageFormat.format(INVALID_SOMETHING, gateway , "gateway address");

        if (message != null)
            errors.add("*** " + message + " ***" + getEolChar());

        return errors;
    }

    private List<String> validateNameServers(String[] nameServers) {
        if (nameServers == null || nameServers.length == 0) return null;
        List<String> errors = new ArrayList<String>();
        for (String ns : nameServers) {
            if (!isValidIpAddress(ns))
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

    private boolean isValidIpAddress(String address) {
        return InetAddressUtil.isValidIpAddress(address);
    }

    private boolean doRepeatConfiguration() throws IOException, WizardNavigationException {
        boolean doItAgain = getConfirmationFromUser(CONFIGURE_MORE_INTERFACES_PROMPT, "no");

        return (doItAgain) ;
    }

    private void saveConfig(NetworkingConfigurationBean.NetworkConfig whichConfig) {
        netBean.addNetworkingConfig(whichConfig);
    }

    private NetworkingConfigurationBean.NetworkConfig doSelectInterfacePrompts() throws IOException, WizardNavigationException {

        List<NetworkingConfigurationBean.NetworkConfig> allNetworkConfigs = getInterfaces();

        List<String> promptList = new ArrayList<String>();

        int x = 1;
        for (NetworkingConfigurationBean.NetworkConfig networkConfig : allNetworkConfigs) {
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
        String whichChoice = getData(promptList.toArray(new String[0]), "1", allowedEntries,null);

        int choiceNum = Integer.parseInt(whichChoice);
        NetworkingConfigurationBean.NetworkConfig theConfig;

        if (choiceNum < 1 || choiceNum > allNetworkConfigs.size()) {
            //creating a new interface
            theConfig = NetworkingConfigurationBean.makeNetworkConfig(null, null, false);
        } else {
            theConfig = allNetworkConfigs.get(choiceNum -1);
        }
        theConfig.setDirtyFlag(true);
        return theConfig;
    }

    private NetworkingConfigurationBean.NetworkConfig doConfigurationPrompts(NetworkingConfigurationBean.NetworkConfig whichConfig) throws IOException, WizardNavigationException {

        if (StringUtils.isEmpty(whichConfig.getInterfaceName())) {
            whichConfig.setInterfaceName(promptForNewInterfaceName(whichConfig));
        }

        String bootProto = getBootProtocol(whichConfig);
        whichConfig.setBootProto(bootProto);

        if (StringUtils.equalsIgnoreCase(bootProto, NetworkingConfigurationBean.STATIC_BOOT_PROTO)) {
            whichConfig.setIpAddress(getIpAddress(whichConfig), true);
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
        String name;
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

        boolean shouldConfigNameServers = getConfirmationFromUser(CONFIGURE_NAMESERVERS_PROMPT, "no");

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
        if (shouldConfigNameServers) {
                do {
                    nameserversline = getData(
                            new String[] {
                                    "Enter the nameservers to be associated with the \"" + interfaceName + "\" interface (comma separated): "
                            },
                            defaultNameserversLine,
                            (String[])null,
                            null
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
            gateway = getData(new String[] {prompt}, gateway, (String[]) null,null);
            errors = validateGateway(gateway);

            isValid = errors.isEmpty();
            if (!isValid) printText(errors);
        } while (!isValid);

        return gateway;
    }


    private String getIpAddress(NetworkingConfigurationBean.NetworkConfig netConfig) throws IOException, WizardNavigationException {
        String interfaceName = netConfig.getInterfaceName();
        List<InterfaceAddress> addresses = netConfig.getInterfaceAddresses();
        String currentFirstAddress = null;
        if (!addresses.isEmpty()) {
            List<String> stringAddresses = new ArrayList<String>();
            for (InterfaceAddress address : addresses) {
//                if (!(address.getAddress() instanceof Inet6Address)) {
                    stringAddresses.add(address.getAddress().getHostAddress());
//                }
            }
            currentFirstAddress = stringAddresses.get(0);
        }

        boolean isValid;
        List<String> errors;
        String defaultAddress = StringUtils.isNotEmpty(currentFirstAddress)?currentFirstAddress:"";
        String prompt = "Enter the IP for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(defaultAddress))
            prompt += " [" + defaultAddress + "] ";
        prompt += ": ";

        String newAddress;
        do {
            newAddress = getData(new String[] {prompt}, defaultAddress, (String[]) null,null);
            errors = validateIpAddress(newAddress);

            isValid = errors.isEmpty();
            if (!isValid) printText(errors);
        } while (!isValid);

        return newAddress;
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
            netMask = getData(new String[] {prompt}, defaultNetMask, (String[]) null,null);
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

        String input = getData(prompts, defaultValue, new String[]{"1","2"},null);
        bootProto = StringUtils.equals("1", input)?NetworkingConfigurationBean.STATIC_BOOT_PROTO:NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO;
        return bootProto;
    }

    private List<NetworkingConfigurationBean.NetworkConfig> getInterfaces() {
        List<NetworkingConfigurationBean.NetworkConfig> allConfigs = netBean.getAllNetworkInterfaces();
        List<NetworkingConfigurationBean.NetworkConfig> configurableConfigs = new LinkedList<NetworkingConfigurationBean.NetworkConfig>();
        for (NetworkingConfigurationBean.NetworkConfig aConfig : allConfigs) {
            if (!aConfig.isVirtual())
                configurableConfigs.add(aConfig);
        }
        return configurableConfigs;
    }

    public String getTitle() {
        return TITLE;
    }

    public boolean isShowNavigation() {
        return false;
    }

}