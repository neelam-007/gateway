package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Server side class that checks the remote ip range rule stored in a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerRemoteIpRange implements ServerAssertion {

    public ServerRemoteIpRange(RemoteIpRange subject) {
        this.subject = subject;
        calculateIPRange();
    }

    public AssertionStatus checkRequest(Request req, Response res) throws IOException, PolicyAssertionException {
        // todo
        // check whether request's remote ip is in range
        // decide if that good or not based on rule
        return AssertionStatus.NONE;
    }

    /**
     * calculate 32 bit representation of an ip address
     * @param ipAddress
     * @return
     */
    private int strTo32bitAdd(String ipAddress) {
        StringTokenizer st = new StringTokenizer(ipAddress, ".");
        // grab each bit and parse it
        int ipx24bit = Integer.parseInt((String) st.nextElement());
        int ipx16bit = Integer.parseInt((String) st.nextElement());
        int ipx8bit = Integer.parseInt((String) st.nextElement());
        int ipxbit = Integer.parseInt(((String) st.nextElement()).trim());
        ipx24bit = ipx24bit << 24;
        ipx16bit = ipx16bit << 16;
        ipx8bit = ipx8bit << 8;
        // now add them all together to get the 32 bit val
        return ipx24bit + ipx16bit + ipx8bit + ipxbit;
    }

    /**
     * this only happens once at construction time
     */
    private void calculateIPRange() {
        int mask32 = (0xFFFFFFFF >>> (32 - subject.getNetworkMask())) << (32 - subject.getNetworkMask());
        int ipx32 = strTo32bitAdd(subject.getStartIp()) & mask32;
        int broadcast = ipx32 | (~mask32);
        endIp[0] = (broadcast & 0xFF000000) >>> 24;
        endIp[1] = (broadcast & 0x00FF0000) >>> 16;
        endIp[2] = (broadcast & 0x0000FF00) >>> 8;
        endIp[3] = (broadcast & 0x000000FF);

        StringTokenizer st = new StringTokenizer(subject.getStartIp(), ".");
        startIp[0] = Integer.parseInt((String) st.nextElement());
        startIp[1] = Integer.parseInt((String) st.nextElement());
        startIp[2] = Integer.parseInt((String) st.nextElement());
        startIp[3] = Integer.parseInt(((String) st.nextElement()).trim());
    }

    private RemoteIpRange subject;
    private int[] startIp = new int[4];
    private int[] endIp = new int[4];
}
