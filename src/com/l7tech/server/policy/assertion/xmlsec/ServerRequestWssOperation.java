/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ParsedElement;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.ProcessorResultUtil;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Code shared between ServerRequestWssConfidentiality and ServerRequestWssIntegrity.
 */
public abstract class ServerRequestWssOperation implements ServerAssertion {
    private Logger logger;

    protected ServerRequestWssOperation(Logger logger, XpathBasedAssertion data) {
        this.logger = logger;
        this.data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                logger.info("Request not SOAP; cannot verify WS-Security contents");
                return AssertionStatus.BAD_REQUEST;
            }
            wssResults = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
        if (wssResults == null) {
            logger.info("This request did not contain any WSS level security.");
            context.setPolicyViolated(true);
            return AssertionStatus.FALSIFIED;
        }

        // get the document
        Document soapmsg = null;
        try {
            soapmsg = context.getRequest().getXmlKnob().getDocument(false);
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Cannot get payload document.", e);
            return AssertionStatus.BAD_REQUEST;
        }

        ProcessorResultUtil.SearchResult result = null;
        try {
            result = ProcessorResultUtil.searchInResult(logger,
                                                        soapmsg,
                                                        data.getXpathExpression().getExpression(),
                                                        data.getXpathExpression().getNamespaces(),
                                                        isAllowIfEmpty(),
                                                        getElementsFoundByProcessor(wssResults),
                                                        getPastTenseOperationName());
        } catch (ProcessorException e) {
            throw new PolicyAssertionException(e);
        }
        if (result.isFoundButWasntOperatedOn())
            context.setPolicyViolated(true);
        switch (result.getResultCode()) {
            case ProcessorResultUtil.NO_ERROR:
                return AssertionStatus.NONE;
            case ProcessorResultUtil.FALSIFIED:
                return AssertionStatus.FALSIFIED;
            default:
                return AssertionStatus.SERVER_ERROR;
        }
    }

    /** @return the operation name as a past-tense verb, either "encrypted" or "signed". */
    protected abstract String getPastTenseOperationName();

    /**
     * @return true iff. the the matching elements are allowed to be left undecorated if they are empty.
     *         (ie, true for operation "encrypted"; false for operation "signed".)
     */
    protected abstract boolean isAllowIfEmpty();

    /**
     * Given this wssResults, return the list of approved, operated-on elements.
     * @param wssResults the processor results to look at
     * @return either elementsThatWereSigned or elementsThatWereEncrypted
     */
    protected abstract ParsedElement[] getElementsFoundByProcessor(ProcessorResult wssResults);

    protected XpathBasedAssertion data;
}
