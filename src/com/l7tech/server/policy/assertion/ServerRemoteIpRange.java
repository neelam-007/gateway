package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

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
    private final Auditor auditor;

    public ServerRemoteIpRange(RemoteIpRange rule, ApplicationContext context) {
        this.rule = rule;
        this.auditor = new Auditor(this, context, logger);

        calculateIPRange();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        // get remote address
        TcpKnob tcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
        if (tcp == null) {
            auditor.logAndAudit(AssertionMessages.CANNOT_VALIDATE_IP_ADDRESS);
            return AssertionStatus.BAD_REQUEST;
        }

        String remoteAddress = tcp.getRemoteAddress();
        if (!RemoteIpRange.checkIPAddressFormat(remoteAddress)) {
            auditor.logAndAudit(AssertionMessages.CANNOT_VALIDATE_IP_ADDRESS, new String[] {remoteAddress});

            return AssertionStatus.FALSIFIED;
        }
        // check assertion with this address
        if (assertAddress(remoteAddress)) {
            auditor.logAndAudit(AssertionMessages.REQUESTOR_ADDRESS_ACCEPTED, new String[] {remoteAddress});
            return AssertionStatus.NONE;
        } else {
            auditor.logAndAudit(AssertionMessages.REQUESTOR_ADDRESS_REJECTED, new String[] {remoteAddress});                        
            return AssertionStatus.FALSIFIED;
        }
    }

    /**
     * decide whether the address passed passes or fails the assertion
     */
    boolean assertAddress(String addressToCheck) {
        boolean isInRange = true;
        int[] subject = decomposeAddress(addressToCheck, null);
        for (int i = 0; i < 4; i++) {
            if (subject[i] < startIp[i] || subject[i] > endIp[i]) {
                isInRange = false;
                break;
            }
        }
        // if the address is not in range and the assertion calls for a range inclusion, the assertAddress fails
        if (rule.isAllowRange() && !isInRange) {
            logger.info("This assertion is failing because address " + addressToCheck +
                        " is not in assertion range " + serializedRange);
            return false;
        }

        if (!rule.isAllowRange() && isInRange) {
            logger.info("This assertion is failing because address " + addressToCheck +
                        " is in assertion range " + serializedRange + " (exclusion range)");
            return false;
        }
        return true;
    }

    /**
     * calculate 32 bit representation of an ip address
     * @param ipAddress
     * @return
     */
    private int strTo32bitAdd(String ipAddress) {
        int[] ipx = decomposeAddress(ipAddress, null);
        ipx[0] = ipx[0] << 24;
        ipx[1] = ipx[1] << 16;
        ipx[2] = ipx[2] << 8;
        return ipx[0] + ipx[1] + ipx[2] + ipx[3];
    }

    /**
     * this only happens once at construction time
     */
    private void calculateIPRange() {
        // calculate end address
        int mask32 = (0xFFFFFFFF >>> (32 - rule.getNetworkMask())) << (32 - rule.getNetworkMask());
        int ipx32 = strTo32bitAdd(rule.getStartIp()) & mask32;
        int broadcast = ipx32 | (~mask32);
        endIp[0] = (broadcast & 0xFF000000) >>> 24;
        endIp[1] = (broadcast & 0x00FF0000) >>> 16;
        endIp[2] = (broadcast & 0x0000FF00) >>> 8;
        endIp[3] = (broadcast & 0x000000FF);
        // decompose begin address
        decomposeAddress(rule.getStartIp(), startIp);
        // remember this range in str format for logging feedback (serialize it once only)
        serializedRange = rule.getStartIp() + " .. " + endIp[0] + "." +
                                                       endIp[1] + "." +
                                                       endIp[2] + "." +
                                                       endIp[3];
    }

    private int[] decomposeAddress(String arg, int[] optionalOutput) {
        int[] output = null;
        if (optionalOutput != null && optionalOutput.length == 4) {
            output = optionalOutput;
        } else output = new int[4];
        StringTokenizer st = new StringTokenizer(arg, ".");
        output[0] = Integer.parseInt((String) st.nextElement());
        output[1] = Integer.parseInt((String) st.nextElement());
        output[2] = Integer.parseInt((String) st.nextElement());
        output[3] = Integer.parseInt(((String) st.nextElement()).trim());
        return output;
    }

    private RemoteIpRange rule;
    private int[] startIp = new int[4];
    private int[] endIp = new int[4];
    // for logging purposes
    private String serializedRange;

    private final Logger logger = Logger.getLogger(getClass().getName());
}
