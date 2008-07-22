package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Server side class that checks the remote ip range rule stored in a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 */
public class ServerRemoteIpRange extends AbstractServerAssertion implements ServerAssertion {
    private final Auditor auditor;
    private final Pattern ipaddresspattern;

    public ServerRemoteIpRange(RemoteIpRange rule, ApplicationContext context) {
        super(rule);
        this.rule = rule;
        this.auditor = new Auditor(this, context, logger);
        ipaddresspattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        calculateIPRange();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String input;
        // get remote address
        if (rule.getIpSourceContextVariable() == null) {
            TcpKnob tcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
            if (tcp == null) {
                auditor.logAndAudit(AssertionMessages.IP_NOT_TCP);
                return AssertionStatus.BAD_REQUEST;
            }
            input = tcp.getRemoteAddress();
        } else {
            try {
                Object tmp = context.getVariable(rule.getIpSourceContextVariable());
                if (tmp == null) throw new NoSuchVariableException("could not resolve " + rule.getIpSourceContextVariable());
                input = tmp.toString();
            } catch (NoSuchVariableException e) {
                logger.log(Level.WARNING, "Remote ip from context variable unavailable. Possible policy error", e);
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_UNAVAILABLE, new String[] {rule.getIpSourceContextVariable()});
                return AssertionStatus.SERVER_ERROR;
            }
        }
        Matcher m = ipaddresspattern.matcher(input);
        if (!m.find()) {
            auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, new String[] {input});
            return AssertionStatus.FALSIFIED;
        }
        String remotiptotest = input.substring(m.start(), m.end());
        // check assertion with this address
        if (assertAddress(remotiptotest)) {
            auditor.logAndAudit(AssertionMessages.IP_ACCEPTED, new String[] {remotiptotest});
            return AssertionStatus.NONE;
        } else {
            auditor.logAndAudit(AssertionMessages.IP_REJECTED, new String[] {remotiptotest});
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
        int[] output;
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
