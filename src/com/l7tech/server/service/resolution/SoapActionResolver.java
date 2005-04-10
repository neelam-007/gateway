/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.SoapUtil;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapActionResolver extends WsdlOperationServiceResolver {
    public int getSpeed() {
        return FAST;
    }

    protected String getTargetValue(Definition def, BindingOperation operation) {
        return SoapUtil.findSoapAction(operation);
    }

    protected int getMaxLength() {
        return ResolutionParameters.MAX_LENGTH_RES_PARAMETER;
    }

    protected Object getRequestValue( Message request ) throws ServiceResolutionException {
        HttpRequestKnob httpReqKnob = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob == null)
            return null; // TODO SOAPAction with JMS?
        String soapAction = null;
        try {
            soapAction = httpReqKnob.getHeaderSingleValue(SoapUtil.SOAPACTION);
        } catch (IOException e) {
            throw new ServiceResolutionException("Found multiple " + SoapUtil.SOAPACTION + " headers"); // can't happen
        }
        if (soapAction == null) return null;
        // Strip leading and trailing quotes
        return SoapUtil.stripQuotes(soapAction);

    }

}
