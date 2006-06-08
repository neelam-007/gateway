package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

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

            NetworkingConfigurationBean.NetworkConfig whichConfig = null;

            whichConfig = doSelectInterfacePrompts();
            whichConfig = doConfigurationPrompts(whichConfig);
            saveConfig(whichConfig);

            if (doRepeatConfiguration()) {
                doUserInterview(true);
            }
            storeInput();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> validateGateway(String gateway) {
        List<String> errors = new ArrayList<String>();
        if (StringUtils.isEmpty(gateway)) errors.add("Missing Gateway." + getEolChar());
        if (!isValidIpAddress(gateway)) errors.add("Gateway \"" + gateway + "\" is an invalid IP Address" + getEolChar());
        return errors;
    }

    private boolean isValidIpAddress(String address) {
        String[] octets = address.split("\\.");

        if (octets == null || octets.length < 4) {
            return false;
        }

        for (String octetString : octets) {
            try {
                int octet = Integer.parseInt(octetString);

                if (octet < 0 || octet > 255) return false;

            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private List<String> validateNetMask(String netMask) {
        List<String> errors = new ArrayList<String>();

        if (StringUtils.isEmpty(netMask)) errors.add("Missing Netmask." + getEolChar());
        if (!isValidIpAddress(netMask)) errors.add("Netmask \"" + netMask + "\" is an invalid IP Address" + getEolChar());

        return errors;
    }

    private List<String> validateIpAddress(String ipAddress) {
        List<String> errors = new ArrayList<String>();

        if (StringUtils.isEmpty(ipAddress)) errors.add("*** Missing IP Address ***" + getEolChar());
        if (!isValidIpAddress(ipAddress)) errors.add("*** \"" + ipAddress+ "\" is an invalid IP Address ***" + getEolChar());

        return errors;
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

        printText("Select the Interface you wish to configure. Current configurations are shown in ()" + getEolChar());

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

            String ipAddress = getIpAddress(whichConfig.getIpAddress(), whichConfig.getInterfaceName());
            whichConfig.setIpAddress(ipAddress);

            String netMask = getNetMask(whichConfig.getNetMask(), whichConfig.getInterfaceName());
            whichConfig.setNetMask(netMask);

            String gateway = getGateway(whichConfig.getGateway(), whichConfig.getInterfaceName());
            whichConfig.setGateway(gateway);
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

    private String getGateway(String gateway, String interfaceName) throws IOException, WizardNavigationException {

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


    private String getIpAddress(String ipAddress, String interfaceName) throws IOException, WizardNavigationException {
        String prompt = "Enter the IP for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(ipAddress)) prompt += " [" + ipAddress + "] ";
        prompt += ": ";

        boolean isValid = false;
        List<String> errors = new ArrayList<String>();

        do {
            ipAddress = getData(new String[] {prompt}, ipAddress);
            errors = validateIpAddress(ipAddress);

            isValid = errors.isEmpty();
            if (!isValid) printText(errors);
        } while (!isValid);

        return ipAddress;
    }

    private String getNetMask(String netMask, String interfaceName) throws IOException, WizardNavigationException {
        String prompt = "Enter the netmask for interface \"" + interfaceName + "\"";
        if (StringUtils.isNotEmpty(netMask)) prompt += " [" + netMask + "] ";
        prompt += ": ";

        boolean isValid = false;
        List<String> errors = new ArrayList<String>();

        do {
            netMask = getData(new String[] {prompt}, netMask);
            errors = validateNetMask(netMask);

            isValid = errors.isEmpty();
            if (!isValid) printText(errors);

        } while (!isValid);

        return netMask;
    }

    private String getBootProtocol(NetworkingConfigurationBean.NetworkConfig whichConfig) throws IOException, WizardNavigationException {
        String whichInterface = whichConfig.getInterfaceName();
        String bootProto = whichConfig.getBootProto();

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

        NetworkingConfigurationBean.NetworkConfig newInterface = NetworkingConfigurationBean.makeNetworkConfig(null, null, null, null, null);
        newInterface.isNew(true);
        interfaces.add(newInterface);
        return interfaces;
    }

    private List<NetworkingConfigurationBean.NetworkConfig> getExistingNetworkInterfaces() {

        logger.info("Determine existing interface information");
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
            theNetConfig = NetworkingConfigurationBean.makeNetworkConfig(interfaceName, bootProto, ipAddress, netMask, gateway);
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
