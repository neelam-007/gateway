package com.l7tech.server.policy.assertion;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;

/**
 * Server side class that checks the remote ip range rule stored in a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 */
public class ServerRemoteIpRange extends AbstractServerAssertion<RemoteIpRange> {

    // - PUBLIC

    public ServerRemoteIpRange(RemoteIpRange rule) {
        super(rule);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            RemoteIpRange.validateRange(assertion.getStartIp(), assertion.getNetworkMask());
        } catch (IllegalArgumentException e) {
            logAndAudit(AssertionMessages.IP_INVALID_RANGE, e.getMessage());
            return AssertionStatus.FAILED;
        }
        
        String remoteAddress;
        // get remote address
        if (assertion.getIpSourceContextVariable() == null) {
            TcpKnob tcp = context.getRequest().getKnob(TcpKnob.class);
            if (tcp == null) {
                logAndAudit(AssertionMessages.IP_NOT_TCP);
                return AssertionStatus.BAD_REQUEST;
            }
            remoteAddress = tcp.getRemoteAddress();
        } else {
            try {
                Object tmp = context.getVariable(assertion.getIpSourceContextVariable());
                if (tmp == null) throw new NoSuchVariableException("could not resolve " + assertion.getIpSourceContextVariable());
                remoteAddress = InetAddressUtil.getHostAndPort(tmp.toString(), null).left;
            } catch (NoSuchVariableException e) {
                logger.log(Level.WARNING, "Remote ip from context variable unavailable. Possible policy error", ExceptionUtils.getDebugException(e));
                logAndAudit(AssertionMessages.IP_ADDRESS_UNAVAILABLE, assertion.getIpSourceContextVariable());
                return AssertionStatus.SERVER_ERROR;
            }
        }

        remoteAddress = InetAddressUtil.stripIpv6Brackets(remoteAddress);
        InetAddress addr = InetAddressUtil.getAddress(remoteAddress);

        if (addr == null) {
            logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, remoteAddress);
            return AssertionStatus.FALSIFIED;
        } else if (assertAddress(addr)) {
            logAndAudit(AssertionMessages.IP_ACCEPTED, remoteAddress);
            return AssertionStatus.NONE;
        } else {
            logAndAudit(AssertionMessages.IP_REJECTED, remoteAddress);
            return AssertionStatus.FALSIFIED;
        }
    }

    // - PACKAGE

    boolean assertAddress(InetAddress addressToCheck) {
        return InetAddressUtil.patternMatchesAddress(assertion.getStartIp() + "/" + assertion.getNetworkMask(), addressToCheck) ^ ! assertion.isAllowRange();
    }
}
