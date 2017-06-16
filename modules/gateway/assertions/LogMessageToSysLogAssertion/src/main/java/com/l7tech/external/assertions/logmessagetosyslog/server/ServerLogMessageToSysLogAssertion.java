package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.log.SinkManager;
import com.l7tech.server.log.syslog.Syslog;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.SyslogSeverity;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Pair;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Server side implementation of the LogMessageToSysLogAssertion.
 *
 * @see com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogAssertion
 */
public class ServerLogMessageToSysLogAssertion extends AbstractServerAssertion<LogMessageToSysLogAssertion> {
    private static final Logger logger = Logger.getLogger(ServerLogMessageToSysLogAssertion.class.getName());

    private final LogMessageToSysLogAssertion assertion;
//    private final Auditor auditor;
    //    private final String[] variablesUsed;
    private final SyslogManager syslogManager;
    private final SinkManager sinkManager;
    private final ServerConfig serverConfig;

    private final static String SYSLOG_MANAGER_BEAN_NAME = "syslogManager";
    private final static String SINK_MANAGER_BEAN_NAME = "sinkManager";
    private final static String SERVER_CONFIG_BEAN_NAME = "serverConfig";

    // also known as process in SSG speak
    private final static String DEFAULT_TAG = "SSG";
    private final static String REQUEST_ID_CONTEXT_VAR_NAME = "requestId";

    protected final static String SYSLOG_CLUSTER_PROPERTY_TAG_NAME = "logMessageToSysLog.tag";

    private String syslogSeverityString;
    private Goid sysLogGoid;
    private SyslogSeverity syslogSeverity;
    private Map<String, String> cefExtensionKeyValuePairs;

    /**
     * Syslog format pattern that includes a hostname
     */
    public static final String LOG_PATTERN_STANDARD = "<{2}>{3} {5} {7}: {9}";

    /**
     * Syslog format pattern that does not have a hostname
     */
    public static final String LOG_PATTERN_NO_HOST = "<{2}>{3} {7}: {9}";

    public ServerLogMessageToSysLogAssertion(LogMessageToSysLogAssertion assertion, ApplicationContext appContext) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;

        this.syslogManager = (SyslogManager) appContext.getBean(SYSLOG_MANAGER_BEAN_NAME);
        this.sinkManager = (SinkManager) appContext.getBean(SINK_MANAGER_BEAN_NAME);
        this.serverConfig = (ServerConfig) appContext.getBean(SERVER_CONFIG_BEAN_NAME);

        /* Make sure all Managers are ready */
        if (syslogManager == null) {
            // to do move up to declaration and throw PolicyAssertionException and Log the reason
            logger.log(Level.WARNING, "SysLog Manager not able to be loaded");
            throw new PolicyAssertionException(assertion, "SysLog Manager not able to be loaded");
        }
        if (sinkManager == null) {
            logger.log(Level.WARNING, "Sink Manager not able to be loaded");
            throw new PolicyAssertionException(assertion, "Sink Manager not able to be loaded");
        }
        if (serverConfig == null) {
            logger.log(Level.WARNING, "Server Config not able to be loaded");
            throw new PolicyAssertionException(assertion, "Server Config not able to be loaded");
        }

        /* -------------------------------*/
        sysLogGoid = assertion.getSyslogGoid();
        if (sysLogGoid == null) {
            logger.log(Level.WARNING, "The selected syslog server is not available");
            throw new PolicyAssertionException(assertion, "The selected syslog server is not available");
        }

        syslogSeverityString = assertion.getSysLogSeverity();
        if (syslogSeverityString == null || syslogSeverityString.length() == 0) {
            logger.log(Level.WARNING, "SysLog Severity has not been set properly");
            throw new PolicyAssertionException(assertion, "SysLog Severity has not been set properly");
        }
        syslogSeverity = SyslogSeverity.valueOf(syslogSeverityString);

        cefExtensionKeyValuePairs = assertion.getCefExtensionKeyValuePairs();


    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        /* get values from assertion */
        Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), getAudit());

        if (assertion.isCEFEnabled()) {
            String signatureId = ExpandVariables.process(assertion.getCefSignatureId(), vars, getAudit());
            String signatureName = ExpandVariables.process(assertion.getCefSignatureName(), vars, getAudit());
            boolean isLogMessageToSyslogAssertionFailed = false;
            if (signatureId.isEmpty()) {
                getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "CEF Header's Signature-ID was not set");
                isLogMessageToSyslogAssertionFailed = true;
            }
            if (signatureName.isEmpty()) {
                getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "CEF Header's Signature Name was not set");
                isLogMessageToSyslogAssertionFailed = true;
            }
            if (isLogMessageToSyslogAssertionFailed) {
                return AssertionStatus.FAILED;
            }
        }

        // Block of code to verify all required data has been submitted
        String str = ExpandVariables.process(assertion.getMessageToBeLogged(), vars, getAudit());
        final StringBuilder sbSysLogMessage = new StringBuilder(str == null ? "" : str);
        if (sbSysLogMessage.length() == 0) {
            getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "No Message has been set to send");
            return AssertionStatus.FAILED;
        }

        Object process = "";
        if (assertion.isCEFEnabled()) {
            String value;
            for (String key : cefExtensionKeyValuePairs.keySet()) {
                value = ExpandVariables.process(cefExtensionKeyValuePairs.get(key), vars, getAudit(), true);
                sbSysLogMessage.append(key).append("=").append(value.replaceAll("[=]", "\\\\=").replaceAll("[\r\n]+", "\n")).append(" ");
            }
        } else {
            try {
                // set the default value
                Object objRequestIdString = context.getVariable(REQUEST_ID_CONTEXT_VAR_NAME);
                process = "SSG[" + objRequestIdString + "]";
                if (!(objRequestIdString instanceof String)) {
                    getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "Unexpectedly could not get " + REQUEST_ID_CONTEXT_VAR_NAME + " Context Variable");
                    return AssertionStatus.FAILED;
                }
            } catch (NoSuchVariableException nsve) {
                getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "Unexpectedly could not get " + REQUEST_ID_CONTEXT_VAR_NAME + " Context Variable");
                return AssertionStatus.FAILED;
            }
            try {
                process = context.getVariable("gateway." + SYSLOG_CLUSTER_PROPERTY_TAG_NAME);
            } catch (NoSuchVariableException e) {
                // do nothing as we have already set the process.
            }
        }

        // note the Level will not be used
        LogRecord record = new LogRecord(Level.INFO, sbSysLogMessage.toString());

        /* -------------------------------*/

        /* main Logic */
        SinkConfiguration sysLogSinkConfig = null;
        try {
            sysLogSinkConfig = sinkManager.findByPrimaryKey(sysLogGoid);
        } catch (Exception e) {
            throw new PolicyAssertionException(assertion, e);
        }
        if (!sysLogSinkConfig.getName().startsWith(LogMessageToSysLogAssertion.SYSLOG_LOG_SINK_PREFIX)) {
            getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "Only syslog servers with prefix 'syslogwrite_' are supported");
            return AssertionStatus.FAILED;
        }
        if (sysLogSinkConfig.isEnabled()) {
            getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "Only non-enabled syslog servers are supported");
            return AssertionStatus.FAILED;
        }
        if (!(sysLogSinkConfig.getType() == SinkConfiguration.SinkType.SYSLOG)) {
            getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "Only log sinks of type 'Syslog' are supported");
            return AssertionStatus.FAILED;
        }

        SyslogProtocol protocol;
        String configProtocol = sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL);
        if (SinkConfiguration.SYSLOG_PROTOCOL_TCP.equals(configProtocol)) {
            protocol = SyslogProtocol.TCP;
        } else if (SinkConfiguration.SYSLOG_PROTOCOL_SSL.equals(configProtocol)) {
            protocol = SyslogProtocol.SSL;
        } else {
            protocol = SyslogProtocol.UDP;
        }

        // get the host:port from the configuration
        String[][] syslogHosts = getSyslogHost(sysLogSinkConfig);

        String format = LOG_PATTERN_STANDARD;
        if (!Boolean.valueOf(sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME))) {
            format = LOG_PATTERN_NO_HOST;
        }

        String timeZoneId = sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE);
        int facility = Integer.parseInt(sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_FACILITY));
        String charset = sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET);

        // get properties for SSL with client auth if flag is set
        String sslKeystoreAlias = null;
        String sslKeystoreId = null;
        if (SyslogProtocol.SSL.equals(protocol) && "true".equals(sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_CLIENTAUTH))) {
            sslKeystoreAlias = sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS);
            sslKeystoreId = sysLogSinkConfig.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID);
        }

        try {
            Syslog syslog = syslogManager.getSyslog(protocol, syslogHosts, format, timeZoneId, facility, serverConfig.getHostname(), charset, null, sslKeystoreAlias, sslKeystoreId);
            syslog.log(syslogSeverity, (String) process, record.getThreadID(), record.getMillis(), record.getMessage());
        } catch (IllegalArgumentException iae) {
            // something was wrong with the arguments we are trying to write to SysLog with.  Throw exception
            throw new IllegalArgumentException("Error creating syslog client", iae);
            // right now its throwing an exception but maybe just fail the assertion instead?
        }

        /* -------------------------------*/

        return AssertionStatus.NONE;


    }

    /*
    * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
    * that would otherwise keep our instances from getting collected.
    */

    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerLogMessageToSysLogAssertion is preparing itself to be unloaded");
    }

    /**
     * Returns the host & port number values from the configuration's SyslogHost list given
     * the specified list index.
     *
     * @param configuration the sink configuration
     * @return 2-dimentional array of string values where: index 0 = host; and index 1 = port;
     * @throws IllegalArgumentException if the host value pulled from the configuration is not valid
     */
    private String[][] getSyslogHost(final SinkConfiguration configuration)
            throws IllegalArgumentException {
        String[][] result = new String[configuration.syslogHostList().size()][];

        int index = 0;
        for (String value : configuration.syslogHostList()) {

            Pair<String, String> hostAndPort = InetAddressUtil.getHostAndPort(value, null);

            // this error should not occur if the UI is validating the input data correctly
            if (hostAndPort.right == null) {
                throw new IllegalArgumentException("Invalid Syslog host format encountered=" + value);
            }

            result[index++] = new String[]{hostAndPort.left, hostAndPort.right};
        }

        return result;
    }
}
