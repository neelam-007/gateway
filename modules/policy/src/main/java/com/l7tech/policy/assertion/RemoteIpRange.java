package com.l7tech.policy.assertion;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Assertion that verifies that remote ip address of requestor is within valid range.
 * The ip range is recorded in CIDR format.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 */
@ProcessesRequest
public class RemoteIpRange extends Assertion implements UsesVariables {

    // - PUBLIC

    public static final int IPV4_MAX_PREFIX = 32;
    public static final int IPV6_MAX_PREFIX = 128;

    public RemoteIpRange() {
        startIp = DEFAULT_START_IP;
        networkMask = DEFAULT_NETWORK_PREFIX;
        allowRange = true;
    }

    /**
     * fully descriptive constructor
     *
     * @param startIp the start ip address of the range specified by this assertion
     * @param networkPrefix the network prefix; valid values are 0..32 for IPv4 address ranges, 0..128 for IPv6 address ranges
     * @param allowRange true if addresses is this range are authorised, false if they are unauthorized
     */
    public RemoteIpRange(String startIp, int networkPrefix, boolean allowRange) {
        validateRange(startIp, networkPrefix);
        this.startIp = startIp;
        this.networkMask = networkPrefix;
        this.allowRange = allowRange;
    }

    /**
     * the start ip address of the range specified by this assertion
     */
    public String getStartIp() {
        return startIp;
    }

    /**
     * the start ip address of the range specified by this assertion
     */
    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    /**
     * the network mask that goes with the start ip for the ip range specified by this assertion
     */
    public int getNetworkMask() {
        return networkMask;
    }

    /**
     * the network mask that goes with the start ip for the ip range specified by this assertion
     * @param networkMask valid values are 0..32
     */
    public void setNetworkMask(int networkMask) {
        this.networkMask = networkMask;
    }

    public void setAddressRange(String address, Integer prefix) {
        validateRange(address, prefix);
        this.startIp = address;
        this.networkMask = prefix;
    }

    /**
     * whether the range specified by this assertion represents an inclusion or an exclusion
     * @return true if addresses is this range are authorised, false if they are unauthorized
     */
    public boolean isAllowRange() {
        return allowRange;
    }

    /**
     * whether the range specified by this assertion represents an inclusion or an exclusion
     * @param allowRange true if addresses is this range are authorised, false if they are unauthorized
     */
    public void setAllowRange(boolean allowRange) {
        this.allowRange = allowRange;
    }

    /**
     * the context variable to use to get the source ip address to use for the evaluation of this
     * assertion. when null, the source ip address should be taken from the tcp layer
     */
    public String getIpSourceContextVariable() {
        return ipSourceContextVariable;
    }

    /**
     * the context variable to use to get the source ip address to use for the evaluation of this
     * assertion. when null, the source ip address should be taken from the tcp layer
     */
    public void setIpSourceContextVariable(String ipSourceContextVariable) {
        this.ipSourceContextVariable = ipSourceContextVariable;
    }

    private final static String baseName = "Restrict Access to IP Address Range";
    
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RemoteIpRange>(){
        @Override
        public String getAssertionName( final RemoteIpRange assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder sb = new StringBuilder();
            sb.append((assertion.isAllowRange())? "Allow": "Forbid");
            sb.append(" access to IP Address Range");
            sb.append(" [");
            sb.append(assertion.getStartIp());
            sb.append("/");
            sb.append(assertion.getNetworkMask());
            sb.append("]");
            return sb.toString();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"misc"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Restrict or allow service access based on the IP address of the Web service or XML application requestor.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/network.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.RemoteIpRangePropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "IP Address Range Properties");

        return meta;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        if (ipSourceContextVariable == null || ipSourceContextVariable.length() < 1) {
            return new String[0];
        } else return new String[] {ipSourceContextVariable};
    }

    /**
     * @throws IllegalArgumentException if the startIp/networkPrefix combination are not a valid IPv4 or IPv6 address range.
     */
    public static void validateRange(String startIp, int networkPrefix) {
        String errMsg = null;
        String pattern = startIp + "/" + Integer.toString(networkPrefix);

        if (InetAddressUtil.isValidIpv4Pattern(pattern)) {
            if (networkPrefix < 0 || networkPrefix > IPV4_MAX_PREFIX)
                errMsg = "Invalid IPv4 network prefix" + networkPrefix;
        } else if (InetAddressUtil.isValidIpv6Pattern(pattern)) {
            if (networkPrefix < 0 || networkPrefix > IPV6_MAX_PREFIX)
                errMsg = "Invalid IPv6 network prefix: " + networkPrefix;
        } else {
            errMsg = "Invalid IP address range: " + pattern;
        }

        if (errMsg != null)
            throw new IllegalArgumentException(errMsg);
    }

    // - PRIVATE

    private static final String DEFAULT_START_IP = "192.168.1.0";
    private static final int DEFAULT_NETWORK_PREFIX = 24;

    private String startIp;
    private int networkMask;
    private boolean allowRange;
    private String ipSourceContextVariable = null;
}
