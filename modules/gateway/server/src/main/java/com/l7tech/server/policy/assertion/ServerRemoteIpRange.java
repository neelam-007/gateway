package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side class that checks the remote ip range rule stored in a RemoteIpRange assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/><br/>
 * User: rraquepo<br/>
 * Date: Mar 2012<br/>
 */
public class ServerRemoteIpRange extends AbstractServerAssertion<RemoteIpRange> {

    // - PUBLIC

    public ServerRemoteIpRange(RemoteIpRange rule) {
        super(rule);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            String startIp = formatStringWithVariables(assertion.getStartIp(), context);
            RemoteIpRange.validateRange(startIp, assertion.getNetworkMask(), false);
        } catch (IllegalArgumentException e) {
            logAndAudit(AssertionMessages.IP_INVALID_RANGE, e.getMessage());
            return AssertionStatus.FAILED;
        } catch (NoSuchVariableException e) {
            logger.log(Level.WARNING, assertion.getStartIp() + " - referenced context variable unavailable. Possible policy error", ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        String remoteAddress;
        // get remote address
        final String ipSourceContextVariable = assertion.getIpSourceContextVariable();
        if (ipSourceContextVariable == null) {
            TcpKnob tcp = context.getRequest().getKnob(TcpKnob.class);
            if (tcp == null) {
                logAndAudit(AssertionMessages.IP_NOT_TCP);
                return AssertionStatus.BAD_REQUEST;
            }
            remoteAddress = tcp.getRemoteAddress();
        } else {
            try {
                Object tmp = context.getVariable(ipSourceContextVariable);
                if (tmp == null)
                    throw new NoSuchVariableException(ipSourceContextVariable, "could not resolve " + ipSourceContextVariable);
                remoteAddress = InetAddressUtil.getHostAndPort(tmp.toString(), null).left;
            } catch (NoSuchVariableException e) {
                logger.log(Level.WARNING, "Remote ip from context variable unavailable. Possible policy error", ExceptionUtils.getDebugException(e));
                logAndAudit(AssertionMessages.IP_ADDRESS_UNAVAILABLE, ipSourceContextVariable);
                return AssertionStatus.SERVER_ERROR;
            }
        }

        remoteAddress = InetAddressUtil.stripIpv6Brackets(remoteAddress);
        InetAddress addr = InetAddressUtil.getAddress(remoteAddress);

        if (addr == null) {
            logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, remoteAddress);
            return AssertionStatus.FALSIFIED;
        }

        try {
            if (assertAddress(addr, context)) {
                logAndAudit(AssertionMessages.IP_ACCEPTED, remoteAddress);
                return AssertionStatus.NONE;
            } else {
                logAndAudit(AssertionMessages.IP_REJECTED, remoteAddress);
                return AssertionStatus.FALSIFIED;
            }
        } catch (NoSuchVariableException e) {
            logger.log(Level.WARNING, assertion.getStartIp() + " - referenced context variable unavailable. Possible policy error", ExceptionUtils.getDebugException(e));
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, assertion.getStartIp());
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private Pattern getSearchPattern(String varWithIndex, Map<String, Pattern> varPatternMap) {
        if (varPatternMap.containsKey(varWithIndex)) {
            return varPatternMap.get(varWithIndex);
        }

        final Pattern searchPattern = Pattern.compile("${" + varWithIndex + "}", Pattern.LITERAL);
        varPatternMap.put(varWithIndex, searchPattern);

        return searchPattern;
    }

    private String formatStringWithVariables(String str, PolicyEnforcementContext context) throws NoSuchVariableException {
        final String[] strWithIndex = Syntax.getReferencedNamesIndexedVarsNotOmitted(str);
        if (strWithIndex.length < 1) {
            return str;//nothing to format
        }
        final Map<String, Pattern> varPatternMap = new HashMap<String, Pattern>();
        for (final String varWithIndex : strWithIndex) {
            final Pattern searchPattern = getSearchPattern(varWithIndex, varPatternMap);
            Matcher matcher = searchPattern.matcher(str);
            Object val = null;
            try {
                val = context.getVariable(varWithIndex);
                if (val == null)
                    throw new NoSuchVariableException(varWithIndex, "could not resolve ");
            } catch (NoSuchVariableException e) {
                logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, varWithIndex);
                throw new NoSuchVariableException(varWithIndex, "could not resolve ");
            }
            str = matcher.replaceFirst(val.toString());
        }
        return str;
    }
    // - PACKAGE

    boolean assertAddress(InetAddress addressToCheck, PolicyEnforcementContext context) throws NoSuchVariableException {
        String startIp = formatStringWithVariables(assertion.getStartIp(), context);
        return InetAddressUtil.patternMatchesAddress(startIp + "/" + assertion.getNetworkMask(), addressToCheck) ^ !assertion.isAllowRange();
    }
}
