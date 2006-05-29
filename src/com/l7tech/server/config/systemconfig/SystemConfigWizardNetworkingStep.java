package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:16:36 AM
 */
public class SystemConfigWizardNetworkingStep extends BaseConsoleStep {

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
        ArrayList<String> promptList = new ArrayList<String>();
        printText("Select the Interface you wish to configure" + getEolChar());

        int x = 1;
        for (NetworkingConfigurationBean.NetworkConfig networkConfig : availableNetworkInterfaces) {
            String infoLine = networkConfig.getBootProto();

            if (StringUtils.equalsIgnoreCase(NetworkingConfigurationBean.STATIC_BOOT_PROTO, networkConfig.getBootProto())) {
                infoLine += ", IP = " + networkConfig.getIpAddress() + ", NETMASK = " + networkConfig.getNetMask();
            }
            promptList.add(String.valueOf(x++) + ") " + networkConfig.getInterfaceName() + " (" + infoLine + ")" + getEolChar());
        }
        promptList.add("Please make a selection [1] : ");
        String[] prompts = promptList.toArray(new String[]{});
        String whichChoice = getData(prompts, "1");

        int choiceIndex = 0;
        try {
            choiceIndex = Integer.parseInt(whichChoice);
        } catch (NumberFormatException e) {
            choiceIndex = 1;
        }

        return availableNetworkInterfaces.get(choiceIndex -1);
    }

    private NetworkingConfigurationBean.NetworkConfig doConfigurationPrompts(NetworkingConfigurationBean.NetworkConfig whichConfig) throws IOException, WizardNavigationException {

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
        if (StringUtils.isEmpty(bootProto)) bootProto = NetworkingConfigurationBean.STATIC_BOOT_PROTO;

        String defaultValue = StringUtils.equalsIgnoreCase(NetworkingConfigurationBean.STATIC_BOOT_PROTO, bootProto)?"1":"2";


        String[] prompts = new String[] {
            "What is the boot protocol for \"" + whichInterface + "\" ?" + getEolChar(),
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
        List<NetworkingConfigurationBean.NetworkConfig> interfaces = new ArrayList<NetworkingConfigurationBean.NetworkConfig>();
        interfaces.add(NetworkingConfigurationBean.makeNetworkConfig("eth0", "static", "192.168.21.1", "255.255.255.0"));
        interfaces.add(NetworkingConfigurationBean.makeNetworkConfig("eth1", "dhcp", "192.168.1.1", "255.255.255.0"));
        return interfaces;
    }

    public String getTitle() {
        return TITLE;
    }

    public boolean isShowNavigation() {
        return false;
    }

}
