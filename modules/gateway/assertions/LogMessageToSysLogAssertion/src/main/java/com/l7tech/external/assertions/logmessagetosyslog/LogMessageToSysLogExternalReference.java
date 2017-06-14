package com.l7tech.external.assertions.logmessagetosyslog;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by huaal03 on 2017-06-05.
 */
public class LogMessageToSysLogExternalReference extends ExternalReference {
    private static final String El_NAME_REF = "LogMessageToSysLogExternalReference";
    private static final String EL_NAME_GOID = "GOID";
    private static final String EL_NAME_LOG_SINK_NAME = "LogSinkName";
    private static final String EL_NAME_LOG_SINK_DESCRIPTION = "LogSinkDescription";
    private static final String EL_NAME_LOG_SINK_TYPE = "LogSinkType";
    private static final String EL_NAME_LOG_SINK_SEVERITY = "LogSinkSeverity";

    private static final Logger logger = Logger.getLogger(LogMessageToSysLogExternalReference.class.getName());

    private Goid goid;
    private String logSinkName;
    private String logSinkDescription;
    private SinkConfiguration.SinkType logSinkType;
    private SinkConfiguration.SeverityThreshold logSinkSeverity;
    private LocalizeAction localizeType;
    private LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();

    public LogMessageToSysLogExternalReference(ExternalReferenceFinder finder) {
        super(finder);
    }

    public LogMessageToSysLogExternalReference(ExternalReferenceFinder finder, final Goid sinkId) {
        this(finder);
        goid = sinkId;

        try {
            SinkConfiguration sinkConfiguration = logSinkAdmin.getSinkConfigurationByPrimaryKey(goid);

            if (sinkConfiguration == null) {
                throw new IllegalArgumentException("Cannot export policy, Log Sink id: " + sinkId.toString() + " does not exist");
            }

            logSinkName = sinkConfiguration.getName();
            logSinkDescription = sinkConfiguration.getDescription();
            logSinkType = sinkConfiguration.getType();
            logSinkSeverity = sinkConfiguration.getSeverity();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot retrieve entire log sink, partial reference is created instead");
        }
    }

    public String getLogSinkName() {
        return logSinkName;
    }

    public String getLogSinkDescription() {
        return logSinkDescription;
    }

    public SinkConfiguration.SinkType getLogSinkType() {
        return logSinkType;
    }

    public SinkConfiguration.SeverityThreshold getLogSinkSeverity() {
        return logSinkSeverity;
    }

    @Override
    public String getRefId() {
        return goid.toString();
    }

    @Override
    public boolean setLocalizeReplace(Goid sinkId) {
        localizeType = LocalizeAction.REPLACE;
        this.goid = sinkId;
        return true;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElementNS(null, El_NAME_REF);
        setTypeAttribute(refEl);
        referencesParentElement.appendChild(refEl);

        if (goid != null) {
            addParamEl(refEl, EL_NAME_GOID, goid.toHexString(), false);
            addParamEl(refEl, EL_NAME_LOG_SINK_NAME, logSinkName, false);
            addParamEl(refEl, EL_NAME_LOG_SINK_DESCRIPTION, logSinkDescription, false);
            addParamEl(refEl, EL_NAME_LOG_SINK_TYPE, logSinkType.toString(), false);
            addParamEl(refEl, EL_NAME_LOG_SINK_SEVERITY, logSinkSeverity.name(), false);
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        if (logSinkAdmin != null) {

            try {
                Collection<SinkConfiguration> allLogSinks = logSinkAdmin.findAllSinkConfigurations();
                if (logSinkAdmin.getSinkConfigurationByPrimaryKey(this.goid) != null) {
                    logger.fine("The log message to syslog assertion log sink found an exact GOID match: " + goid.toString());
                    return true;
                }
                for (SinkConfiguration oneLogSink : allLogSinks) {
                    if (oneLogSink.getName().equals(this.logSinkName) && oneLogSink.getType() == this.logSinkType) {
                        logger.fine("The log message to syslog assertion log sink was resolved from GOID '" +
                                goid.toString() + "' to '" + oneLogSink.getGoid());

                        this.goid = oneLogSink.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }
            } catch (FindException e) {
                if (goid != null){
                    logger.warning("Cannot find a log sink with that goid, " + goid.toString());
                } else {
                    logger.warning("Cannot find a log sink with that goid. The Goid is null");
                }
            }
        }
        return false;
    }

    @Override
    protected boolean localizeAssertion(@Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof LogMessageToSysLogAssertion) {
                final LogMessageToSysLogAssertion logMessageToSysLogAssertion = (LogMessageToSysLogAssertion) assertionToLocalize;
                final Goid sinkId = logMessageToSysLogAssertion.getSyslogGoid();
                if (sinkId != null) {
                    if (localizeType == LocalizeAction.REPLACE) {
                        logMessageToSysLogAssertion.setSyslogGoid(this.goid);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static LogMessageToSysLogExternalReference parseFromElement(ExternalReferenceFinder context, Element el)
            throws InvalidDocumentFormatException {
        if (!el.getNodeName().equals(El_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of connectorName " + El_NAME_REF);
        }

        LogMessageToSysLogExternalReference parsedExternalReference = new LogMessageToSysLogExternalReference(context);

        String parsedElement = getParamFromEl(el, EL_NAME_GOID);
        if (parsedElement != null) {
            try {
                parsedExternalReference.goid = new Goid(parsedElement);
            } catch (IllegalArgumentException e) {
                throw new InvalidDocumentFormatException("Invalid log message to syslog goid: " + ExceptionUtils.getMessage(e), e);
            }
        }

        parsedExternalReference.logSinkName = getParamFromEl(el, EL_NAME_LOG_SINK_NAME);
        parsedExternalReference.logSinkDescription = getParamFromEl(el, EL_NAME_LOG_SINK_DESCRIPTION);
        parsedExternalReference.logSinkType = SinkConfiguration.SinkType.valueOf(getParamFromEl(el, EL_NAME_LOG_SINK_TYPE));
        parsedExternalReference.logSinkSeverity = SinkConfiguration.SeverityThreshold.valueOf(getParamFromEl(el, EL_NAME_LOG_SINK_SEVERITY));

        return parsedExternalReference;
    }
}
