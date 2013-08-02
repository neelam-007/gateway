package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Formatter for audit logging.
 *
 * User: vchan
 */
public class AuditLogFormatter<REC extends AuditRecord> {

    private static final Logger logger = Logger.getLogger(AuditLogFormatter.class.getName());

    private static final int MAX_CONTEXT_VAR_LENGTH = 1000;
    private static final int MAX_FORMATTED_MSG_SIZE = 10000;

    private static final String DEFAULT_TEMPLATE_SERVICE_HEADER = "Processing request for service: {3}";
    private static final String DEFAULT_TEMPLATE_SERVICE_FOOTER = "{1}";
    private static final String DEFAULT_TEMPLATE_SERVICE_DETAIL = "{0}: {1}";
    private static final String DEFAULT_TEMPLATE_OTHER_FORMAT   = "{1}";
    private static final String DEFAULT_TEMPLATE_OTHER_DETAIL   = "{0}: {1}";

    private static String templateServiceHeader;
    private static String templateServiceHeaderNoCtxVars;
    private static String templateServiceFooter;
    private static String templateServiceFooterNoCtxVars;
    private static String templateServiceDetail;
    private static String templateServiceDetailNoCtxVars;
    private static String templateOtherFormat;
    private static String templateOtherDetail;
    private static String[] contextVariablesUsed;

    private static final Object updateMutex = new Object();
    private static final List<String> clusterPropertiesNames;
    static {
        clusterPropertiesNames = new ArrayList<String>(10);
        clusterPropertiesNames.add( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_HEADER);
        clusterPropertiesNames.add( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_FOOTER);
        clusterPropertiesNames.add( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_DETAIL);
        clusterPropertiesNames.add( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER);
        clusterPropertiesNames.add( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER_DETAIL);
    }

    protected static Config config;

    private final String serviceHeader;
    private final String serviceFooter;
    private final String serviceDetail;
    private final String otherFormat;
    private final String otherDetail;

    private final String serviceGoid;
    private final String serviceName;

    public AuditLogFormatter(Map<String, Object> ctxVariablesMap) {

        synchronized (updateMutex) {
            if (templateServiceHeader == null || templateServiceFooter == null || templateServiceDetail == null ||
                templateOtherFormat == null || templateOtherDetail == null)
            {
                initializeTemplates();
            }
        }

        if (ctxVariablesMap != null) {
            Audit auditor = new DummyAuditor();
            this.serviceHeader = removeInvalidFormatting(
                    ExpandVariables.process(templateServiceHeader, ctxVariablesMap, auditor, false, MAX_CONTEXT_VAR_LENGTH));
            this.serviceFooter = removeInvalidFormatting(
                    ExpandVariables.process(templateServiceFooter, ctxVariablesMap, auditor, false, MAX_CONTEXT_VAR_LENGTH));
            this.serviceDetail = removeInvalidFormatting(
                    ExpandVariables.process(templateServiceDetail, ctxVariablesMap, auditor, false, MAX_CONTEXT_VAR_LENGTH));
            this.otherFormat = templateOtherFormat;
            this.otherDetail = templateOtherDetail;
        } else {
            // need to remove all context variables in this case
            this.serviceHeader = templateServiceHeaderNoCtxVars;
            this.serviceFooter = templateServiceFooterNoCtxVars;
            this.serviceDetail = templateServiceDetailNoCtxVars;
            this.otherFormat = templateOtherFormat;
            this.otherDetail = templateOtherDetail;
        }

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.getCurrent();
        if (pec != null && pec.getService() != null) {
            this.serviceGoid = (pec.getService().getGoid() != null ? pec.getService().getGoid().toString() : "");
            if ( pec.getService().getRoutingUri() == null ) {
                this.serviceName = pec.getService().getName();
            } else {
                this.serviceName = MessageFormat.format("{0} [{1}]", new String[] { pec.getService().getName(), pec.getService().getRoutingUri() });
            }
        } else {
            this.serviceGoid = "";
            this.serviceName = "";
        }
    }

    public AuditLogFormatter() {
        this(null);
    }

    public static void notifyPropertyChange(String propertyName) {
        if (clusterPropertiesNames.contains(propertyName)) {
            // set the templates to null so they get re-initialized from the cluster properties
            synchronized (updateMutex) {
                templateServiceHeader = templateServiceFooter = templateServiceDetail = null;
                templateOtherFormat = templateOtherDetail = null;
                logger.log(Level.INFO, "Audit log formatter, cluster property updated: " + propertyName);
            }
        }
    }

    private static void initializeTemplates() {

        loadClusterProperties( config != null ? config : ConfigFactory.getCachedConfig());
        parseTemplatesForVariables();

        templateServiceHeaderNoCtxVars = removeContextVariables(templateServiceHeader);
        templateServiceFooterNoCtxVars = removeContextVariables(templateServiceFooter);
        templateServiceDetailNoCtxVars = removeContextVariables(templateServiceDetail);
        templateOtherFormat = removeContextVariables(templateOtherFormat);
        templateOtherDetail = removeContextVariables(templateOtherDetail);

        // for debugging purposes
        if (logger.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("AuditLogFormatter initialized to:\n");
            sb.append( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_HEADER).append(" = ").append(templateServiceHeader).append("\n");
            sb.append( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_FOOTER).append(" = ").append(templateServiceFooter).append("\n");
            sb.append( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_DETAIL).append(" = ").append(templateServiceDetail).append("\n");
            sb.append( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER).append(" = ").append(templateOtherFormat).append("\n");
            sb.append( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER_DETAIL).append(" = ").append(templateOtherDetail);
            logger.log(Level.FINER, sb.toString());
        }
    }

    protected static void loadClusterProperties(Config config ) {

        final String badSyntaxRegex = "\\$\\{[\\s]*\\}|\\{[\\s]*\\}";

        templateServiceHeader = config.getProperty( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_HEADER );
        if (templateServiceHeader == null)
            templateServiceHeader = DEFAULT_TEMPLATE_SERVICE_HEADER;
        else
            templateServiceHeader = templateServiceHeader.replaceAll(badSyntaxRegex, "");

        templateServiceFooter = config.getProperty( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_FOOTER );
        if (templateServiceFooter == null)
            templateServiceFooter = DEFAULT_TEMPLATE_SERVICE_FOOTER;
        else
            templateServiceFooter = templateServiceFooter.replaceAll(badSyntaxRegex, "");

        templateServiceDetail = config.getProperty( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_DETAIL );
        if (templateServiceDetail == null)
            templateServiceDetail = DEFAULT_TEMPLATE_SERVICE_DETAIL;
        else
            templateServiceDetail = templateServiceDetail.replaceAll(badSyntaxRegex, "");

        templateOtherFormat = config.getProperty( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER );
        if (templateOtherFormat == null)
            templateOtherFormat = DEFAULT_TEMPLATE_OTHER_FORMAT;
        else
            templateOtherFormat = templateOtherFormat.replaceAll(badSyntaxRegex, "");

        templateOtherDetail = config.getProperty( ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER_DETAIL );
        if (templateOtherDetail == null)
            templateOtherDetail = DEFAULT_TEMPLATE_OTHER_DETAIL;
        else
            templateOtherDetail = templateOtherDetail.replaceAll(badSyntaxRegex, "");
    }

    private static void parseTemplatesForVariables() {
        Set<String> names = new HashSet<String>();
        collectVars(names, templateServiceHeader);
        collectVars(names, templateServiceFooter);
        collectVars(names, templateServiceDetail);
        collectVars(names, templateOtherFormat);
        collectVars(names, templateOtherDetail);

        if (names.size() > 0) {
            contextVariablesUsed = names.toArray(new String[names.size()]);
        } else {
            contextVariablesUsed = new String[0];
        }
    }

    /**
     * Remove all context variables in the specified template.
     *
     * @param template the template to remove the variables from
     * @return the resultant template string without any context variables
     */
    private static String removeContextVariables(String template) {
        final String regexPrefix = "\\$\\{";
        final String regexSuffix = "\\}";
        for (String var : contextVariablesUsed) {
            template = template.replaceAll(regexPrefix + var + regexSuffix, "");
        }

        return removeInvalidFormatting(template);
    }

    /** Formatting filter 1 that removes the bulk of the bad replacement formats {x} {11x} {x11} {1 2} {1.2} { 1_2} etc */
    private static final String badFormatFilter1 = "\\{[\\d\\s]*[^\\d|\\}]+[\\d\\s]*\\}";
    /** Formatting filter 2 that removes the rest of the bad replacement formats {x1} {x11} {.1.b} {1x2x3} */
    private static final String badFormatFilter2 = "\\{[\\D]+[^\\}]*\\}|\\{[\\d]+[^\\d\\}]+[^\\}]*\\}";

    /**
     * Removes invalid formats that causes problems in the MessageFormat formatter.
     * E.g. include: {x} {1x} {x1} {1 1} {1x2y3} - non numeric characters.
     *
     * for bug #6671 - remove any unsupported formatting to prevent MessageFormat from failing
     *
     * @param input the string to parse
     * @return String with formatting filters applied
     */
    private static String removeInvalidFormatting(String input) {
        return input.replaceAll(badFormatFilter1, "").replaceAll(badFormatFilter2, "");
    }

    /**
     * Finds all the context variable names referenced in the given string and add them to the set of names.
     * @param varNames the set to add any additional context var names
     * @param s the string to parse
     */
    private static void collectVars(Set<String> varNames, String s) {
        if (s == null || s.length() == 0) return;
        String[] vars = Syntax.getReferencedNames(s);
        varNames.addAll(Arrays.asList(vars));
    }

    /**
     * Returns the list of context variable names that are used by the log formatter.
     *
     * @return array of String for the context variable names
     */
    public static String[] getContextVariablesUsed() {
        return contextVariablesUsed;
    }

    public String format(REC record, boolean isHeader) {
        if (isHeader)
            return truncateForReturn(MessageFormat.format(this.serviceHeader, getParameters(record, isHeader)));
        else if (record instanceof MessageSummaryAuditRecord)
            return truncateForReturn(MessageFormat.format(this.serviceFooter, getParameters(record, isHeader)));
        else
            return truncateForReturn(MessageFormat.format(this.otherFormat, getParameters(record, isHeader)));
    }

    public String format(REC record) {
        return format(record, false);
    }

    public String formatDetail(REC record, AuditDetailMessage details) {
        if (record instanceof MessageSummaryAuditRecord)
            return truncateForReturn(MessageFormat.format(this.serviceDetail, getParameters(details)));
        return truncateForReturn(MessageFormat.format(this.otherDetail, getParameters(details)));
    }

    public String formatDetail(AuditDetailMessage details) {
        if (serviceGoid.length() > 0 && serviceName.length() > 0)
            return truncateForReturn(MessageFormat.format(this.serviceDetail, getParameters(details)));
        return truncateForReturn(MessageFormat.format(this.otherDetail, getParameters(details)));
    }

    /**
     * Returns the replacement parameters for the given audit record that will be used to
     * perform the audit log formatting.
     *
     * The Object[] will always consist of four entries:
     * <ul>
     * <li>{0} - Audit record id</li>
     * <li>{1} - Audit message</li>
     * <li>{2} - Service Oid</li>
     * <li>{3} - Service name/description</li>
     * </ul>
     *
     * @param record the record to extract the parameters from
     * @param isHeader specifies whether the parameters are for a service header
     * @return Object[] used to pass
     */
    private Object[] getParameters(REC record, boolean isHeader) {

        Object[] parms = new String[4];
        parms[0] = "";
        parms[1] = (isHeader ? "" : record.getMessage());
        parms[2] = (record instanceof MessageSummaryAuditRecord ? serviceGoid : "");
        parms[3] = (record instanceof MessageSummaryAuditRecord ? serviceName : "");
        return parms;
    }

    /**
     *
     * @param details the audit details message
     * @return Object[] used to pass
     */
    private Object[] getParameters(AuditDetailMessage details) {
        return new String[] { Integer.toString(details.getId()),
                              details.getMessage(),
                              serviceGoid,
                              serviceName };
    }

    /**
     * Truncates the formattedMessage to the MAX_FORMATTED_MSG_SIZE if the total message
     * length exceeds the maximum set.
     *
     * @param formattedMessage the formatted message
     * @return the final formatted log message (possibly truncated) that is <= MAX_FORMATTED_MSG_SIZE
     */
    private String truncateForReturn(String formattedMessage) {
        if (formattedMessage.length() == 0) {
            return null;
        }
        if (formattedMessage.length() > MAX_FORMATTED_MSG_SIZE) {
            return formattedMessage.substring(0, MAX_FORMATTED_MSG_SIZE);
        }
        return formattedMessage;
    }

    /**
     *
     */
    private class DummyAuditor implements Audit {
        @Override
        public void logAndAudit( @NotNull AuditDetailMessage msg, String[] params, Throwable e) {
            // do nothing impl
        }

        @Override
        public void logAndAudit( @NotNull AuditDetailMessage msg, String... params) {
            // do nothing impl
        }

        @Override
        public void logAndAudit( @NotNull AuditDetailMessage msg) {
            // do nothing impl
        }
    }
}
