package com.l7tech.policy.assertion;

import java.util.StringTokenizer;

/**
 * Assertion that verifies that remote ip address of requestor is within valid range.
 * The ip range is recorded in CIDR format.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 * $Id$<br/>
 *
 */
public class RemoteIpRange extends Assertion {

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

    private String startIp;
    private int networkMask;
    private boolean allowRange;

    private final static int MIN_IP_VALUE = 0;
    private final static int MAX_IP_VALUE = 255;
}
