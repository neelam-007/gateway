package com.l7tech.security.xml.processor;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.xml.SoapFaultDetail;
import org.w3c.dom.Element;

/**
 * Exception used to indicate that a referenced WS-SecureConversation security context was not found.
 */
public class BadSecurityContextException extends Exception implements SoapFaultDetail {
    private final String id;
    private final String wsscNamespace;
    private String faultactor = null;

    public BadSecurityContextException( final String contextId,
                                        final String wsscNamespace ) {
        super("The context " + contextId + " cannot be resolved.");
        this.id = contextId;
        this.wsscNamespace = wsscNamespace;
    }

    public String getContextId() {
        return id;
    }

    public String getWsscNamespace() {
        return wsscNamespace;
    }

    @Override
    public String getFaultCode() {
        return SecureSpanConstants.FAULTCODE_BADCONTEXTTOKEN;
    }

    @Override
    public String getFaultString() {
        return "The soap message referred to a secure conversation context that was not recognized. " + id;
    }

    @Override
    public Element getFaultDetail() {
        return null;
    }

    @Override
    public String getFaultActor() {
        return faultactor;
    }

    @Override
    public String getFaultActor(String defaultActor) {
        if (faultactor == null)
            faultactor = defaultActor;
        return faultactor;
    }

    @Override
    public void setFaultActor(String faultActor) {
        this.faultactor = faultActor;
    }
}
