package com.l7tech.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public enum IpProtocol {

    IPv4 {
        @Override
        public List<String> validateAddress(String address) {
            return validateIpv4Address(address);
        }

        @Override
        public boolean isEnabled() {
            return InetAddressUtil.isIpv4Enabled();
        }},

    IPv6 {
        @Override
        public List<String> validateAddress(String address) {
            return validateIpv6Address(address);
        }

        @Override
        public boolean isEnabled() {
            return InetAddressUtil.isIpv6Enabled();
        }};

    /**
     * @return list of validation errors, or empty list if address is valid; never null. 
     */
    public abstract List<String> validateAddress(String address);

    /**
     * @return true if the IP protocol is enabled, false otherwise
     */
    public abstract boolean isEnabled();

    public String getConfigureDefaultGatewayPrompt() {
        return MessageFormat.format(CONFIGURE_GATEWAY_PROMPT, this);
    }

    public String getDefaultGatewayPrompt() {
        return MessageFormat.format(DEFAULT_GATEWAY_PROMPT, this);
    }

    public String getDefaultGatewayDevicePrompt() {
        return MessageFormat.format(DEFAULT_GATEWAY_INTERFACE_PROMPT, this);
    }

    // - PRIVATE

    private static final String CONFIGURE_GATEWAY_PROMPT = "Would you like to configure a default {0} gateway and interface?";
    private static final String DEFAULT_GATEWAY_PROMPT = "Enter {0} Address of the default gateway: ";
    private static final String DEFAULT_GATEWAY_INTERFACE_PROMPT = "Select the Interface you wish to use as the {0} gateway device: ";
    private static final String MISSING_IP_ADDRESS_MSG = "Missing IP Address";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static List<String> validateIpv4Address(String ipAddress) {
        List<String> errors = new ArrayList<String>();

        String message = null;
        if (ipAddress == null || ipAddress.trim().length() == 0)
            message = MISSING_IP_ADDRESS_MSG;
        else if (!InetAddressUtil.isValidIpv4Address(ipAddress))
            message = "Invalid IPv4 address: " + ipAddress;

        if (message != null)
            errors.add("*** " + message + " ***" + LINE_SEPARATOR);

        return errors;
    }

    private static List<String> validateIpv6Address(String address) {
        List<String> errors = new ArrayList<String>();
        try {
            String ipv6Literal = ( address != null && (address.length() == 0 || address.charAt(0) != '[')) ? "[" + address + "]" : address;
            InetAddress addr = InetAddress.getByName(ipv6Literal);
            if (!(addr instanceof Inet6Address)) {
                errors.add("IPv6 address expected.");
            }
        } catch (UnknownHostException e) {
            errors.add(ExceptionUtils.getMessage(e));
        }
        return errors;
    }
}
