package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.message.HasSoapAction;
import com.l7tech.message.Message;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.MimeKnob;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.SoapConstants;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author alex
 */
public class SoapActionResolver extends WsdlOperationServiceResolver<String> {

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public SoapActionResolver( final AuditFactory auditorFactory ) {
        super( auditorFactory );
    }

    @Override
    public void configure( final ResolutionConfiguration resolutionConfiguration ) {
        super.configure( resolutionConfiguration );
        enabled.set( resolutionConfiguration.isUseSoapAction() );
    }

    @Override
    public boolean usesMessageContent() {
        return false;
    }

    @Override
    protected Set<String> getTargetValues(Definition def, BindingOperation operation) {
        return Collections.singleton(SoapUtil.findSoapAction(operation));
    }

    @Override
    protected String getRequestValue(Message request) throws ServiceResolutionException {
        HasSoapAction hsa = request.getKnob(HasSoapAction.class);
        if (hsa == null)
            return null;
        String soapAction;
        try {
            soapAction = hsa.getSoapAction();
        } catch (IOException e) {
            throw new ServiceResolutionException("Found multiple " + SoapConstants.SOAPACTION + " headers"); // can't happen
        }
        if (soapAction == null) {
            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPACTION_NONE);
            return "";
        }
        // Strip leading and trailing quotes
        return SoapUtil.stripQuotes(soapAction);

    }

    @Override
    public boolean isApplicableToMessage(Message request) {
        if (!enabled.get()) return false;

        // Filter out requests for which resolution by soap action is not appropriate.
        //
        MimeKnob mimeKnob = request.getKnob(MimeKnob.class);
        HasSoapAction hsa = request.getKnob(HasSoapAction.class);
        boolean isHttp = request.getKnob(HttpRequestKnob.class) != null;
        boolean isXml = false;
        boolean soapActionAvailable = false;

        // If we want to check for a soap knob here we need to mark this as a
        // resolver that required parsing of the message body (#usesMessageContent())
        if ( mimeKnob != null && request.isInitialized() ) {
            try {
                isXml = mimeKnob.getFirstPart().getContentType().isXml();
            } catch (IOException e) {
                // then it is not xml
            }
        }

        // For non HTTP messages, SOAPAction may not always be available
        try {
            if (hsa != null)
                soapActionAvailable = hsa.getSoapAction() != null;
        } catch (IOException e) {
            // let resolve() handle multivalue case
        }

        // If it is an HTTP/XML service it should have been resolved by now.
        if ( (isHttp && !isXml) || (!soapActionAvailable) ) {
            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPACTION_NOT_HTTP_OR_SOAP);
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean isApplicableToConflicts() {
        return enabled.get();
    }
}
