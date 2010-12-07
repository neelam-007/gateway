package com.l7tech.server.policy.assertion;

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
import java.net.Inet4Address;
import java.net.Inet6Address;
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
                logger.log(Level.WARNING, "Remote ip from context variable unavailable. Possible policy error", e);
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_UNAVAILABLE, assertion.getIpSourceContextVariable());
                return AssertionStatus.SERVER_ERROR;
            }
        }

        InetAddress addr = InetAddressUtil.getAddress(input);
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
        String startIp = assertion.getStartIp();
        if (startIp == null || startIp.isEmpty()) {
            if (addressToCheck instanceof Inet4Address)
                startIp = InetAddressUtil.getAnyHostAddress4();
            else if (addressToCheck instanceof Inet6Address)
                startIp = InetAddressUtil.getAnyHostAddress6();
        }
        return InetAddressUtil.patternMatchesAddress(startIp + "/" + assertion.getNetworkMask(), addressToCheck) ^ ! assertion.isAllowRange();
    }

    // - PRIVATE

    private final Auditor auditor;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
