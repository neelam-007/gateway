/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.service.PublishedService;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapActionResolver extends WsdlOperationServiceResolver {
    private final Logger logger = Logger.getLogger(SoapActionResolver.class.getName());

    public int getSpeed() {
        return FAST;
    }

    protected String getTargetValue(Definition def, BindingOperation operation) {
        return SoapUtil.findSoapAction(operation);
    }

    protected Object getRequestValue( Message request ) throws ServiceResolutionException {
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
            logger.fine("soapaction is null");
            return "";
        }
        // Strip leading and trailing quotes
        return SoapUtil.stripQuotes(soapAction);

    }

    public Set<PublishedService> resolve(Message request, Set<PublishedService> serviceSubset) throws ServiceResolutionException {
        // since this only applies to http messages, we dont want to narrow down subset if msg is not http
        boolean notHttp = (request.getKnob(HttpRequestKnob.class) == null);
        if (notHttp) {
            logger.fine("soapaction resolver skipped because the request is not http");
            return serviceSubset;
        } else {
            return super.resolve(request, serviceSubset);
        }
    }
}
