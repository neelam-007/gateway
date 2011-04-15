package com.l7tech.external.assertions.sophos.server;

import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.external.assertions.sophos.SophosAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.springframework.context.ApplicationContext;

import com.l7tech.server.ServerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the SophosAssertion.
 *
 * @see com.l7tech.external.assertions.sophos.SophosAssertion
 */
public class ServerSophosAssertion extends AbstractMessageTargetableServerAssertion<SophosAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSophosAssertion.class.getName());

    private final SophosAssertion assertion;
    private final Auditor auditor;
    private final ClusterPropertyCache clusterPropertyCache;
    private final String[] variablesUsed;
    /**
     * Used for cluster property management
     */

    private FailoverStrategy<String> failoverStrategy;

     public ServerSophosAssertion(SophosAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();

        clusterPropertyCache = context.getBean("clusterPropertyCache", ClusterPropertyCache.class);

        failoverStrategy = AbstractFailoverStrategy.makeSynchronized(FailoverStrategyFactory.createFailoverStrategy(assertion.getFailoverStrategyName(), assertion.getAddresses()));
    }

    protected Auditor getAuditor() {
        return auditor;
    }

    private AuditDetailMessage getVirusFoundMessage() {
        String logLevelString = clusterPropertyCache.getPropertyValue(SophosAssertion.CPROP_SOPHOS_VIRUS_FOUND_LOG_LEVEL);
        Level logLevel = Level.WARNING;
        if(logLevelString != null) {
            try {
                logLevel = Level.parse(logLevelString);
            } catch(IllegalArgumentException e) {
            }
        }

        if(logLevel.equals(Level.FINEST)) {
            return AssertionMessages.SOPHOS_RESPONSE_FINEST;
        } else if(logLevel.equals(Level.FINER)) {
            return AssertionMessages.SOPHOS_RESPONSE_FINER;
        } else if(logLevel.equals(Level.FINE)) {
            return AssertionMessages.SOPHOS_RESPONSE_FINE;
        } else if(logLevel.equals(Level.INFO)) {
            return AssertionMessages.SOPHOS_RESPONSE_INFO;
        } else {
            return AssertionMessages.SOPHOS_RESPONSE_WARNING;
        }
    }

    private Pair<Integer, Integer> getClusterProperties() {
        String connectTimeoutStr = clusterPropertyCache.getPropertyValue(SophosAssertion.CPROP_SOPHOS_SOCKET_CONNECT_TIMEOUT);
        String readTimeoutStr = clusterPropertyCache.getPropertyValue(SophosAssertion.CPROP_SOPHOS_SOCKET_READ_TIMEOUT);
        int connectTimeout;
        try{
            if (connectTimeoutStr == null){
                connectTimeout = Integer.parseInt(SophosAssertion.DEFAULT_TIMEOUT);
            } else{
                connectTimeout = Integer.parseInt(connectTimeoutStr);
            }
            connectTimeout = connectTimeout > -1 ? connectTimeout : Integer.parseInt(SophosAssertion.DEFAULT_TIMEOUT);
        }catch (NumberFormatException ne){
            connectTimeout = Integer.parseInt(SophosAssertion.DEFAULT_TIMEOUT);
        }

        int readTimeout;
        try{
            if (readTimeoutStr == null){
                readTimeout = Integer.parseInt(SophosAssertion.DEFAULT_TIMEOUT);
            } else{
                readTimeout = Integer.parseInt(readTimeoutStr);
            }
            readTimeout = readTimeout > -1 ? readTimeout : Integer.parseInt(SophosAssertion.DEFAULT_TIMEOUT);
        }catch (NumberFormatException ne){
            readTimeout = Integer.parseInt(SophosAssertion.DEFAULT_TIMEOUT);
        }

        return new Pair<Integer, Integer>(connectTimeout, readTimeout);
    }

    public AssertionStatus doCheckRequest(PolicyEnforcementContext context,
                                          final Message message,
                                          final String messageDescription,
                                          final AuthenticationContext authContext)
    throws IOException, PolicyAssertionException
    {
        SsspClient client = null;
        try {
                for(int i = 0;i < ServerConfig.getInstance().getIntProperty(SophosAssertion.PARAM_SOPHOS_FAILOVER_RETRIES, 5);i++) {
                    String hostPort = failoverStrategy.selectService();
                    Pair<String, String> hostAndPort = InetAddressUtil.getHostAndPort(hostPort, SophosAssertion.DEFAULT_PORT);

                    String host = hostAndPort.left;
                    String portStr = hostAndPort.right;
                    int port;

                    try{
                        // check for context vars
                        host = host.indexOf("${") > -1 ? getContextVariable(context, host): host;
                        portStr = portStr.indexOf("${") > -1 ? getContextVariable(context, portStr): portStr;
                        port = Integer.parseInt(portStr);
                    }catch (NumberFormatException ne){
                        port = 0;
                        // do nothing
                    }
                    Pair<Integer, Integer> connectAndReadTimeouts = getClusterProperties();
                    int connectTimeout = connectAndReadTimeouts.left.intValue();
                    int readTimeout = connectAndReadTimeouts.right.intValue();

                    try {
                        client = new SsspClient(host, port);
                        client.connect(connectTimeout, readTimeout);
                        failoverStrategy.reportSuccess(hostPort);
                        break;
                    } catch(IOException ioe) {
                        client = null;
                        failoverStrategy.reportFailure(hostPort);
                        auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Sophos AV connect failed attempt " + i + ": " + ExceptionUtils.getMessage(ioe) }, ExceptionUtils.getDebugException(ioe));
                    }
                }

                if(client == null) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Failed to connect to a Sophos AV host for virus scanning."});
                    return AssertionStatus.FAILED;
                    //throw new IOException("Failed to connect to a Sophos AV host for virus scanning.");
                }

                client.setOption("savigrp", "GrpArchiveUnpack 1");
                client.setOption("output", "xml");
                client.setOption("report", "virus");
                client.setOption("event", "onvirusfound terminate");

                List<String> virusFoundNameList = new ArrayList<String>();
                List<String> virusFoundTypeList = new ArrayList<String>();
                List<String> virusFoundLocationList = new ArrayList<String>();
                List<String> virusFoundDisinfectList = new ArrayList<String>();

                int virusCount = 0;
                for(PartIterator pi = message.getMimeKnob().getParts();pi.hasNext();) {
                    PartInfo partInfo = pi.next();
                    InputStream is = partInfo.getInputStream(false);

                    client.scanData(partInfo.getContentLength(), is);
                    SsspClient.ScanResult result = client.getScanResult();

                    if(!result.isClean()) {
                        virusCount++;
                        virusFoundNameList.add(result.getVirusName());
                        virusFoundTypeList.add(result.getVirusType());
                        virusFoundLocationList.add(result.getVirusLocation());
                        virusFoundDisinfectList.add(result.getDisinfectable());

                        auditor.logAndAudit(getVirusFoundMessage(), new String[] {result.getVirusName(), result.getVirusType(), result.getVirusLocation(), result.getDisinfectable()});

                    }

                }
                client.closeSophosSession();
                client.close();

                if(!virusFoundNameList.isEmpty()){ context.setVariable(assertion.getPrefixVariable() + ".name", virusFoundNameList.toArray(new String[virusFoundNameList.size()])); }
                if(!virusFoundTypeList.isEmpty()){ context.setVariable(assertion.getPrefixVariable() + ".type", virusFoundTypeList.toArray(new String[virusFoundTypeList.size()])); }
                if(!virusFoundLocationList.isEmpty()){  context.setVariable(assertion.getPrefixVariable() + ".location", virusFoundLocationList.toArray(new String[virusFoundLocationList.size()])); }
                if(!virusFoundDisinfectList.isEmpty()){ context.setVariable(assertion.getPrefixVariable() + ".disinfectable", virusFoundDisinfectList.toArray(new String[virusFoundDisinfectList.size()])); }
                context.setVariable(assertion.getPrefixVariable() + ".count", virusCount);

                if(!virusFoundNameList.isEmpty()){
                    if ( assertion.isFailOnError() ){
                        return AssertionStatus.FAILED;
                    }else{
                        return AssertionStatus.NONE;
                    }
                }
        } catch(NoSuchPartException nspe) {
                // Skip to the next part
        } finally {
            if (client != null){
                client.close();
            }
        }
        
        return AssertionStatus.NONE;
    }

    private String getContextVariable(PolicyEnforcementContext context, String conVar) {
        if(conVar != null && conVar.length() > 0) {
          Map<String, Object> vars = context.getVariableMap(Syntax.getReferencedNames(conVar), auditor);
          conVar = ExpandVariables.process(conVar, vars, auditor);
        }
        return conVar;

    }

}
