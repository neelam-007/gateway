package com.l7tech.server.policy.assertion;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
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
import java.net.InetAddress;
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
public class ServerRemoteIpRange extends AbstractServerAssertion<RemoteIpRange> {

    // - PUBLIC

    public ServerRemoteIpRange(RemoteIpRange rule, ApplicationContext context) {
        super(rule);
        this.auditor = new Auditor(this, context, logger);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            RemoteIpRange.validateRange(assertion.getStartIp(), assertion.getNetworkMask());
        } catch (IllegalArgumentException e) {
            auditor.logAndAudit(AssertionMessages.IP_INVALID_RANGE, e.getMessage());
            return AssertionStatus.FAILED;
        }
        
        String input;
        // get remote address
        if (assertion.getIpSourceContextVariable() == null) {
            TcpKnob tcp = context.getRequest().getKnob(TcpKnob.class);
            if (tcp == null) {
                auditor.logAndAudit(AssertionMessages.IP_NOT_TCP);
                return AssertionStatus.BAD_REQUEST;
            }
            input = tcp.getRemoteAddress();
        } else {
            try {
                Object tmp = context.getVariable(assertion.getIpSourceContextVariable());
                if (tmp == null) throw new NoSuchVariableException("could not resolve " + assertion.getIpSourceContextVariable());
                input = tmp.toString();
            } catch (NoSuchVariableException e) {
                logger.log(Level.WARNING, "Remote ip from context variable unavailable. Possible policy error", ExceptionUtils.getDebugException(e));
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_UNAVAILABLE, assertion.getIpSourceContextVariable());
                return AssertionStatus.SERVER_ERROR;
            }
        }

        String host = InetAddressUtil.getHostAndPort(input, null).left;
        if (host != null && host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length()-1);
        }
        InetAddress addr = InetAddressUtil.getAddress(host);

        if (addr == null) {
            auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, input);
            return AssertionStatus.FALSIFIED;
        } else if (assertAddress(addr)) {
            auditor.logAndAudit(AssertionMessages.IP_ACCEPTED, input);
            return AssertionStatus.NONE;
        } else {
            auditor.logAndAudit(AssertionMessages.IP_REJECTED, input);
            return AssertionStatus.FALSIFIED;
        }
    }

    // - PACKAGE

    boolean assertAddress(InetAddress addressToCheck) {
        return InetAddressUtil.patternMatchesAddress(assertion.getStartIp() + "/" + assertion.getNetworkMask(), addressToCheck) ^ ! assertion.isAllowRange();
    }

    // - PRIVATE

    private final Auditor auditor;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
