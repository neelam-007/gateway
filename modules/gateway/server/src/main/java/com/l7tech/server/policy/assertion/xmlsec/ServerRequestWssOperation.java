/*
 * Copyright (C) 2004-5 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResultUtil;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.CausedIOException;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.DomCompiledXpath;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Code shared between ServerRequestWssConfidentiality and ServerRequestWssIntegrity.
 */
public abstract class ServerRequestWssOperation<AT extends XpathBasedAssertion> extends AbstractServerAssertion<AT> implements ServerAssertion {
    private Logger logger;
    protected final Auditor auditor;

    private final DomCompiledXpath compiledXpath;
    private final InvalidXpathException compileFailure;

    protected ServerRequestWssOperation(Logger logger, AT data, ApplicationContext springContext) {
        super(data);
        this.logger = logger;
        this.data = data;
        auditor = new Auditor(this, springContext, logger);
        DomCompiledXpath xp;
        InvalidXpathException fail;
        try {
            xp = new DomCompiledXpath(data.getXpathExpression());
            fail = null;
        } catch (InvalidXpathException e) {
            xp = null;
            fail = e;
        }
        this.compiledXpath = xp;
        this.compileFailure = fail;
    }

    protected DomCompiledXpath getCompiledXpath() throws PolicyAssertionException {
        if (compileFailure != null)
            throw new PolicyAssertionException(data, compileFailure);
        if (compiledXpath == null)
            throw new PolicyAssertionException(data, "No CompiledXpath"); // can't happen
        return compiledXpath;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(data)) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUESTWSS_NONSOAP);

                return AssertionStatus.BAD_REQUEST;
            }
            wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
        /*
        fla bugfix 1914
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.NO_WSS_LEVEL_SECURITY);
            context.setRequestPolicyViolated();
            return AssertionStatus.FALSIFIED;
        }
        */

        // get the document
        Document soapmsg;
        try {
            soapmsg = context.getRequest().getXmlKnob().getDocumentReadOnly();
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {"Cannot get payload document."}, e);
            return AssertionStatus.BAD_REQUEST;
        }

        ParsedElement[] elements = getElementsFoundByProcessor(wssResults);

        if(!elementsFoundByProcessorAreValid(wssResults, elements)) {
            return AssertionStatus.FALSIFIED;
        }

        ProcessorResultUtil.SearchResult result;
        try {
            result = ProcessorResultUtil.searchInResult(logger,
                                                        soapmsg,
                                                        getCompiledXpath(),
                                                        new PolicyEnforcementContextXpathVariableFinder(context),
                                                        isAllowIfEmpty(),
                                                        elements,
                                                        getPastTenseOperationName());
        } catch (ProcessorException e) {
            throw new PolicyAssertionException(data, e);
        }
        if (result.isFoundButWasntOperatedOn())
            context.setRequestPolicyViolated();
        switch (result.getResultCode()) {
            case ProcessorResultUtil.NO_ERROR:
                return AssertionStatus.NONE;
            case ProcessorResultUtil.FALSIFIED:
                return AssertionStatus.FALSIFIED;
            default:
                return AssertionStatus.SERVER_ERROR;
        }
    }

    /**
     * Check that the ParsedElements are valid.
     *
     * @param wssResults the processor results
     * @param elements the parsed elements to check.
     * @return true if valid, false to falsify this assertion.
     * @see #getElementsFoundByProcessor(ProcessorResult)
     */
    protected boolean elementsFoundByProcessorAreValid(ProcessorResult wssResults, ParsedElement[] elements) {
        return true;
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
