package com.l7tech.message;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Stub implementation of HttpRequestKnob, for testing.
 *
 * <p>Add functionality as required.</p>
 */
public class JmsKnobStub implements JmsKnob {

    private final long serviceOid;
    private final boolean bytesMessage;
    private final String soapAction;

    /**
     * Create a new stub jms knob.
     *
     * @param serviceOid The hardcoded id or zero for none
     * @param bytesMessage True for a bytes message
     * @param soapAction The soap action or null for none
     */
    public JmsKnobStub( final long serviceOid,
                        final boolean bytesMessage,
                        final String soapAction ) {
        this.serviceOid = serviceOid;
        this.bytesMessage = bytesMessage;
        this.soapAction = soapAction;
    }

    @Override
    public Map<String, Object> getJmsMsgPropMap() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isBytesMessage() {
        return bytesMessage;
    }

    @Override
    public long getServiceOid() {
        return serviceOid;
    }

    @Override
    public String getSoapAction() throws IOException {
        return soapAction;
    }
}
