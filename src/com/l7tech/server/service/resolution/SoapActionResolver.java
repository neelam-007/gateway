/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.SoapKnob;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import java.io.IOException;
import java.util.Set;

/**
 * @author alex
 */
public class SoapActionResolver extends WsdlOperationServiceResolver<String> {
    public SoapActionResolver(ApplicationContext spring) {
        super(spring);
    }

    public int getSpeed() {
        return FAST;
    }

    protected String getTargetValue(Definition def, BindingOperation operation) {
        return SoapUtil.findSoapAction(operation);
    }

    protected String getRequestValue(Message request) throws ServiceResolutionException {
        HttpRequestKnob httpReqKnob = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob == null)
            return null;
        String soapAction;
        try {
            soapAction = httpReqKnob.getHeaderSingleValue(SoapUtil.SOAPACTION);
        } catch (IOException e) {
            throw new ServiceResolutionException("Found multiple " + SoapUtil.SOAPACTION + " headers"); // can't happen
        }
        if (soapAction == null) {
            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPACTION_NONE);
            return "";
        }
        // Strip leading and trailing quotes
        return SoapUtil.stripQuotes(soapAction);

    }

    public Result resolve(Message request, Set<PublishedService> serviceSubset) throws ServiceResolutionException {
        // since this only applies to http messages, we dont want to narrow down subset if msg is not http
        boolean notHttp = (request.getKnob(HttpRequestKnob.class) == null);
        boolean notSoap = (request.getKnob(SoapKnob.class) == null);
        if (notHttp || notSoap) {
            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPACTION_NOT_HTTP_OR_SOAP);
            return Result.NOT_APPLICABLE;
        } else {
            return super.resolve(request, serviceSubset);
        }
    }
}
