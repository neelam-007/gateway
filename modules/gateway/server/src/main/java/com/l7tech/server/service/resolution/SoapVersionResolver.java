package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.xml.soap.SoapVersion;

import java.util.Arrays;
import java.util.List;

/**
 * A resolver that filters based on SOAP version.
 * <p/>
 * This resolver is based on the content type of first part of the message, so it needn't use the message contents.
 */
public class SoapVersionResolver extends NameValueServiceResolver<String> {
    public SoapVersionResolver(final AuditFactory auditorFactory) {
        super(auditorFactory);
    }

    @Override
    protected List<String> buildTargetValues(PublishedService service) throws ServiceResolutionException {
        SoapVersion ver = service.getSoapVersion();
        if (ver.getContentType() == null ) {
            return Arrays.asList(SoapVersion.SOAP_1_1.getContentType(), SoapVersion.SOAP_1_2.getContentType());
        } else {
            return Arrays.asList(ver.getContentType());
        }
    }

    @Override
    protected String getRequestValue(Message request) throws ServiceResolutionException {
        try {
            return request.getMimeKnob().getFirstPart().getContentType().getMainValue();
        } catch (Exception e) {
            throw new ServiceResolutionException("Unable to check request first part content type", e);
        }
    }

    @Override
    public boolean isApplicableToMessage(Message msg) throws ServiceResolutionException {
        try {
            return !SoapVersion.UNKNOWN.equals(SoapVersion.contentTypeToSoapVersion(msg.getMimeKnob().getFirstPart().getContentType().getMainValue()));
        } catch (Exception e) {
            throw new ServiceResolutionException("Unable to determine whether message uses a SOAP content type", e);
        }
    }

    @Override
    public boolean usesMessageContent() {
        return false;
    }
}
