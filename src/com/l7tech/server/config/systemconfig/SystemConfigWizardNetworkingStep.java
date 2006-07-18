package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    private NetworkingConfigurationBean netBean;
    private List<NetworkingConfigurationBean.NetworkConfig> availableNetworkInterfaces;

    public SystemConfigWizardNetworkingStep(ConfigurationWizard parentWiz) {
        super(parentWiz);

        configBean = new NetworkingConfigurationBean("Network Interface Configuration", "");
        configCommand = new NetworkingConfigurationCommand(configBean);
        netBean = (NetworkingConfigurationBean) configBean;

        availableNetworkInterfaces = getInterfaceInfo();
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
        NetworkingConfigurationBean.NetworkConfig whichConfig = null;

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
                new String[] {"Enter the hostname for this SSG: "},
                ""
        );

        netBean.setHostname(newHostname);
    }

    private List<String> validateIpAddress(String ipAddress) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (StringUtils.isEmpty(ipAddress))
            message = "Missing IP Address";
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
            message = "Missing Netmask.";
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
            message = "Missing Gateway.";
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
            "Would you like to configure another interface? [no]: ",
        };
        String doItAgain = getData(prompts, "no");

        return (StringUtils.equalsIgnoreCase(doItAgain, "yes") || StringUtils.equalsIgnoreCase(doItAgain, "y") ) ;
    }

    private void saveConfig(NetworkingConfigurationBean.NetworkConfig whichConfig) {
        netBean.addNetworkingConfig(whichConfig);
    }

    private NetworkingConfigurationBean.NetworkConfig doSelectInterfacePrompts() throws IOException, WizardNavigationException {

        Map<String, NetworkingConfigurationBean.NetworkConfig> choicesMap = new TreeMap<String, NetworkingConfigurationBean.NetworkConfig>();
        List<String> promptList = new ArrayList<String>();

        int x = 1;
        for (NetworkingConfigurationBean.NetworkConfig networkConfig : availableNetworkInterfaces) {
            String indexStr = String.valueOf(x);
            String prompt = indexStr + ") " + networkConfig.describe();

            choicesMap.put(indexStr, networkConfig);
            promptList.add(prompt + getEolChar());
            x++;
        }
        promptList.add("Please make a selection [1] : ");

        printText("Select the Interface you wish to configure." + getEolChar());
        printText("Current configurations are shown in ()" + getEolChar());

        String whichChoice = getData(promptList, "1", choicesMap.keySet().toArray(new String[]{}));

        return choicesMap.get(whichChoice);
    }

    private NetworkingConfigurationBean.NetworkConfig doConfigurationPrompts(NetworkingConfigurationBean.NetworkConfig whichConfig) throws IOException, WizardNavigationException {

        if (whichConfig.isNew()) {
            whichConfig = promptForNewInterfaceName(whichConfig);
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

    private NetworkingConfigurationBean.NetworkConfig promptForNewInterfaceName(NetworkingConfigurationBean.NetworkConfig theConfig) throws IOException, WizardNavigationException {

        String[] prompts = new String[] {
            "Please enter the name of the new interface (ex: eth5): ",
        };

        String input = getData(prompts, "");
        theConfig.setInterfaceName(input);
        return theConfig;
    }

    private String[] getNameServer(String[] currentNameServers, String interfaceName) throws IOException, WizardNavigationException {
        boolean hasCurrentNameServers = (currentNameServers != null && currentNameServers.length != 0);
        String[] nameServers = null;

        String shouldConfigNameServers = getData(
                new String[]{"Would you like to configure the nameservers for this interface? [no]"},
                "no"
        );

        String defaultNameserversLine = null;
        boolean isFirst = true;
        if (hasCurrentNameServers) {
            for (String s : currentNameServers) {
                defaultNameserversLine += (isFirst?"":", ") + s;
            }
        }

        boolean isValid = false;
        String nameserversline;
        if (isYes(shouldConfigNameServers)) {
                do {
                    isValid = false;
                    nameserversline = null;

                    nameserversline =
                        getData(
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

        boolean isValid = false;
        List<String> errors = new ArrayList<String>();

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

        boolean isValid = false;
        List<String> errors = new ArrayList<String>();
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

        boolean isValid = false;
        List<String> errors = new ArrayList<String>();

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

    private List<NetworkingConfigurationBean.NetworkConfig> getInterfaceInfo() {
        List<NetworkingConfigurationBean.NetworkConfig> interfaces = getExistingNetworkInterfaces();

        NetworkingConfigurationBean.NetworkConfig newInterface = NetworkingConfigurationBean.makeNetworkConfig(null, null);
        newInterface.isNew(true);
        interfaces.add(newInterface);
        return interfaces;
    }

    private List<NetworkingConfigurationBean.NetworkConfig> getExistingNetworkInterfaces() {

        logger.info("Determining existing interface information.");
        List<NetworkingConfigurationBean.NetworkConfig> configs = new ArrayList<NetworkingConfigurationBean.NetworkConfig>();

        String pathName = osFunctions.getNetworkConfigurationDirectory();
        File parentDir = new File(pathName);

        File[] configFiles = parentDir.listFiles(new FilenameFilter() {
                public boolean accept(File file, String s) {
                    return s.toLowerCase().startsWith("ifcfg-") && !s.toLowerCase().equals("ifcfg-lo");
                }
        });

        if (configFiles != null && configFiles.length != 0) {
            for (File file : configFiles) {
                NetworkingConfigurationBean.NetworkConfig theConfig = parseConfigFile(file);
                if (theConfig != null) {
                    configs.add(theConfig);
                    logger.info("found existing configuration for interface: " + theConfig.describe());
                }
            }
        }

        return configs;
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

            String line = null;
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

    public String getTitle() {
        return TITLE;
    }

    public boolean isShowNavigation() {
        return false;
    }

}
