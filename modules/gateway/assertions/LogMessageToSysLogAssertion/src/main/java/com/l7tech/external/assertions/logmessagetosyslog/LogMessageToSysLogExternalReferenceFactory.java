package com.l7tech.external.assertions.logmessagetosyslog;

import com.l7tech.external.assertions.logmessagetosyslog.console.ResolveForeignLogMessageToSysLogPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by huaal03 on 2017-06-05.
 */
public class LogMessageToSysLogExternalReferenceFactory extends ExternalReferenceFactory<ExternalReference, ExternalReferenceFinder> {

    private static final Logger logger = Logger.getLogger(LogMessageToSysLogExternalReference.class.getName());

    public LogMessageToSysLogExternalReferenceFactory() {
        super(LogMessageToSysLogAssertion.class, LogMessageToSysLogExternalReference.class);
    }

    @Override
    public ExternalReference createExternalReference(ExternalReferenceFinder finder, Assertion assertion) {
        if (!(assertion instanceof LogMessageToSysLogAssertion)) {
            throw new IllegalArgumentException("The assertion isn't a Log Message to SysLog Assertion.");
        }
        LogMessageToSysLogAssertion lmtslAssertion = ((LogMessageToSysLogAssertion) assertion);
        return new LogMessageToSysLogExternalReference(finder, lmtslAssertion.getSyslogGoid());
    }

    @Override
    public ExternalReference parseFromElement(ExternalReferenceFinder finder, Element el) throws InvalidDocumentFormatException {
        return LogMessageToSysLogExternalReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(ExternalReference externalReference) {
        if (! (externalReference instanceof LogMessageToSysLogExternalReference)) return null;

        return new ResolveForeignLogMessageToSysLogPanel(null, (LogMessageToSysLogExternalReference)externalReference);
    }
}
