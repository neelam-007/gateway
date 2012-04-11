package com.l7tech.message;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
    private Map<String, String> headers;

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
        headers = new HashMap<String, String>();
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
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

    @Override
    public String[] getHeaderValues(final String name) {
        final String header = headers.get(name);
        if (header != null) {
            return new String[]{header};
        } else {
            return new String[0];
        }
    }

    @Override
    public String[] getHeaderNames() {
        return headers.keySet().toArray(new String[headers.size()]);
    }
}
