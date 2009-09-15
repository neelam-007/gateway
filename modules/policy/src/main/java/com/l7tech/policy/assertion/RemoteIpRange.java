package com.l7tech.policy.assertion;

import java.util.StringTokenizer;

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

    public RemoteIpRange() {
        startIp = "192.168.1.0";
        networkMask = 24;
        allowRange = true;
    }

    /**
     * fully descriptive constructor
     * @param startIp the start ip address of the range specified by this assertion
     * @param networkMask the network mask valid values are 0..32
     * @param allowRange true if addresses is this range are authorised, false if they are unauthorized
     */
    public RemoteIpRange(String startIp, int networkMask, boolean allowRange) {
        if (!checkIPAddressFormat(startIp)) {
            throw new IllegalArgumentException("invalid ip address " + startIp);
        }
        if (networkMask < 0 || networkMask > 32) {
            throw new IllegalArgumentException("invalid network mask " + networkMask);
        }
        this.startIp = startIp;
        this.networkMask = networkMask;
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
        if (!checkIPAddressFormat(startIp)) {
            throw new IllegalArgumentException("invalid ip address " + startIp);
        }
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
        if (networkMask < 0 || networkMask > 32) {
            throw new IllegalArgumentException("invalid network mask " + networkMask);
        }
        this.networkMask = networkMask;
    }

    /**
     * checks that the ip address passed is expressed in expected format (###.###.###.###)
     */
    public static boolean checkIPAddressFormat(String ipAdd) {
        if (ipAdd == null) return false;
        StringTokenizer st = new StringTokenizer(ipAdd, ".");
        if (st.countTokens() == 4) {
            while (st.hasMoreElements()) {
                try {
                    int ipBit = Integer.parseInt((String)st.nextElement());
                    if ((MIN_IP_VALUE > ipBit) || (MAX_IP_VALUE < ipBit)) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
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

    private String startIp;
    private int networkMask;
    private boolean allowRange;
    private String ipSourceContextVariable = null;

    private final static int MIN_IP_VALUE = 0;
    private final static int MAX_IP_VALUE = 255;

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        if (ipSourceContextVariable == null || ipSourceContextVariable.length() < 1) {
            return new String[0];
        } else return new String[] {ipSourceContextVariable};
    }
}
