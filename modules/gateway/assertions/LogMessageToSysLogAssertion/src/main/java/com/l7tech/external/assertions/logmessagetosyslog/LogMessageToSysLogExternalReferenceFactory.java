package com.l7tech.external.assertions.logmessagetosyslog;

import com.l7tech.external.assertions.logmessagetosyslog.console.ResolveForeignLogMessageToSysLogPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

/**
 * The purpose of the LogMessageToSysLogExternalReferenceFactory is to create an external reference, parse an external
 * reference, and deliver a wizard step panel for references to unresolved log sinks.
 *
 * @author huaal03
 * @see LogMessageToSysLogAssertion
 * @see ExternalReference
 * @see LogMessageToSysLogExternalReference
 * @see com.l7tech.external.assertions.logmessagetosyslog.server.LogMessageToSysLogModuleLoadListener
 */
public class LogMessageToSysLogExternalReferenceFactory extends ExternalReferenceFactory<ExternalReference, ExternalReferenceFinder> {

    public LogMessageToSysLogExternalReferenceFactory() {
        super(LogMessageToSysLogAssertion.class, LogMessageToSysLogExternalReference.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param finder: External Reference Finder
     * @param assertion: provide searching information to the finder to retrieve reference details
     * @return a new LogMessageToSysLogExternalReference object to be used as part of the policy export
     */
    @Override
    public ExternalReference createExternalReference(ExternalReferenceFinder finder, Assertion assertion) {
        if (!(assertion instanceof LogMessageToSysLogAssertion)) {
            throw new IllegalArgumentException("The assertion isn't a Log Message to SysLog Assertion.");
        }
        LogMessageToSysLogAssertion lmtslAssertion = ((LogMessageToSysLogAssertion) assertion);
        return new LogMessageToSysLogExternalReference(finder, lmtslAssertion.getSyslogGoid());
    }

    /**
     * {@inheritDoc}
     *
     * @param finder: ExternalReferenceFinder
     * @param el: external reference element
     * @return a log message to syslog ExternalReference
     * @throws InvalidDocumentFormatException {@inheritDoc}
     */
    @Override
    public ExternalReference parseFromElement(ExternalReferenceFinder finder, Element el) throws InvalidDocumentFormatException {
        return LogMessageToSysLogExternalReference.parseFromElement(finder, el);
    }

    /**
     * {@inheritDoc}
     *
     * @param externalReference: used to fill out the reference details in the wizard step panel
     * @return {@inheritDoc}
     */
    @Override
    public Object getResolveExternalReferenceWizardStepPanel(ExternalReference externalReference) {
        if (! (externalReference instanceof LogMessageToSysLogExternalReference)) return null;

        return new ResolveForeignLogMessageToSysLogPanel(null, (LogMessageToSysLogExternalReference)externalReference);
    }
}
