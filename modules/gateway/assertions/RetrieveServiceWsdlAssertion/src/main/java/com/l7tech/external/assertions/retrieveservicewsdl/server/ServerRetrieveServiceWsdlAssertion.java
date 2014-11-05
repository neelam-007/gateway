package com.l7tech.external.assertions.retrieveservicewsdl.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.util.ExceptionUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import static com.l7tech.gateway.common.audit.AssertionMessages.MESSAGE_TARGET_ERROR;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;

/**
 * Server side implementation of the RetrieveServiceWsdlAssertion.
 *
 * @see com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class ServerRetrieveServiceWsdlAssertion extends AbstractServerAssertion<RetrieveServiceWsdlAssertion> {
    @Inject
    protected ServiceCache serviceCache;

    private final String[] variablesUsed;

    public ServerRetrieveServiceWsdlAssertion(final RetrieveServiceWsdlAssertion assertion) {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        final String serviceIdString = ExpandVariables.process(assertion.getServiceId(), vars, getAudit(), true);
        final String hostName = ExpandVariables.process(assertion.getHostname(), vars, getAudit(), true);

        // get target message
        Message targetMessage;

        try {
            targetMessage = context.getOrCreateTargetMessage(assertion.getTargetMessage(), false);
        } catch (NoSuchVariableException e) {
            logAndAudit(MESSAGE_TARGET_ERROR, e.getVariable(), getMessage(e));
            return AssertionStatus.FAILED;
        }

        // parse service goid
        Goid serviceGoid;

        try {
            serviceGoid = Goid.parseGoid(serviceIdString);
        } catch (IllegalArgumentException e) {
            logAndAudit(AssertionMessages.RETRIEVE_WSDL_INVALID_SERVICE_ID,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            return AssertionStatus.FAILED;
        }

        // get published service by goid
        PublishedService service = serviceCache.getCachedService(serviceGoid);

        if (null == service) {
            logAndAudit(AssertionMessages.RETRIEVE_WSDL_SERVICE_NOT_FOUND, serviceIdString);
            return AssertionStatus.FAILED;
        }

        // does the service have a WSDL?
        if (!service.isSoap()) {
            logAndAudit(AssertionMessages.RETRIEVE_WSDL_SERVICE_NOT_SOAP);
            return AssertionStatus.FAILED;
        }

        // get & parse WSDL xml
        Document wsdlDoc;

        InputSource input = new InputSource();
        input.setSystemId(service.getWsdlUrl());
        input.setCharacterStream(new StringReader(service.getWsdlXml()));

        try {
            wsdlDoc = XmlUtil.parse(input, false);
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        // ---

        // rewrite references

        // update endpoints

        // ---

        // add security policy?

        // ---

        // save wsdl to target message
        targetMessage.initialize(wsdlDoc, ContentTypeHeader.XML_DEFAULT);

        return AssertionStatus.NONE;
    }
}
