package com.l7tech.external.assertions.mqnative.server.decorator;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.external.assertions.mqnative.server.MqMessageProxy;
import com.l7tech.external.assertions.mqnative.server.MqNativeConfigException;
import com.l7tech.external.assertions.mqnative.server.MqNativeUtils;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.OutboundHeadersKnob;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides the interface of MQMessage decorator. Handle the decorator chain and provide common method to retrieve
 * customized header attributes.
 */
public abstract class MqMessageDecorator extends MQMessage {

    protected final static String MQ_PREFIX = MqNativeUtils.PREIFX;
    protected final MQMessage mqMessage;
    protected final OutboundHeadersKnob outboundHeadersKnob;
    protected final MqMessageProxy source;
    protected final MqNativeRoutingAssertion assertion;
    protected final PolicyEnforcementContext context;
    protected final Audit audit;
    protected boolean isRequest = true;

    /**
     * Construct a decorator to decorate the MQMessage
     *
     * @param mqMessage The decorated MQMessage
     * @param source The source MQMessage
     * @param outboundHeadersKnob The OutboundHeaderKnob which may contains the customized header.
     * @param assertion Assertion for configuration attribute
     * @param context PolicyEnforcementContext
     * @param audit Audit
     * @throws MQException
     * @throws IOException
     * @throws MQDataException
     */
    public MqMessageDecorator(MQMessage mqMessage, MqMessageProxy source, OutboundHeadersKnob outboundHeadersKnob,
                              MqNativeRoutingAssertion assertion, PolicyEnforcementContext context, Audit audit) throws MQException, IOException, MQDataException {
        this.mqMessage = mqMessage;
        this.outboundHeadersKnob = outboundHeadersKnob;
        this.assertion = assertion;
        this.context = context;
        this.audit = audit;
        this.source = source;

    }

    /**
     * Construct a decorator to decorate the MQMessage and chain with the provided decorator
     *
     * @param decorator The decorator to chain
     */
    protected MqMessageDecorator(MqMessageDecorator decorator) {
        this.mqMessage = decorator;
        this.outboundHeadersKnob = decorator.outboundHeadersKnob;
        this.assertion = decorator.assertion;
        this.context = decorator.context;
        this.audit = decorator.audit;
        this.isRequest = decorator.isRequest;
        this.source = decorator.source;
    }

    /**
     * Set the request attribute, the decorator may decorate a request or response message.
     *
     * @param isRequest Set True when decorate a request message, set False to decorate a response message
     */
    public void setRequest(boolean isRequest) {
        this.isRequest = isRequest;
    }

    /**
     * Decorate the MQMessage. All chained decorator will be invoked.
     *
     * @return The Decorated MQMessage
     * @throws IOException
     * @throws MQDataException
     * @throws MQException
     * @throws MqNativeConfigException
     */
    public MQMessage decorate() throws IOException, MQDataException, MQException, MqNativeConfigException {
        if (mqMessage instanceof MqMessageDecorator) {
            return ((MqMessageDecorator) mqMessage).decorate();
        }
        return mqMessage;
    }

    /**
     * Retrieve all override attributes from the Knob with the provided prefix.
     *
     * @param prefix The prefix of the header attribute.
     * @return All name-value pair of header attributes.
     */
    protected Map<String, Object> getOutboundHeaderAttributes(String prefix) {
        Map<String, Object> override = new LinkedHashMap<String, Object>();
        if (outboundHeadersKnob != null) {
            String[] headernames = outboundHeadersKnob.getHeaderNames();
            for (String headername : headernames) {
                if (headername.startsWith(prefix)) {
                    String[] headervalues = outboundHeadersKnob.getHeaderValues(headername);
                    if (headervalues != null && headervalues.length > 0) {
                        override.put(headername.substring(prefix.length()), headervalues[0]);
                    }
                }
            }
        }
        return override;
    }

    protected String getOutboundHeaderAttribute(String name) {
        if (outboundHeadersKnob != null) {
            String[] headervalues = outboundHeadersKnob.getHeaderValues(name);
            if (headervalues != null && headervalues.length > 0) {
                return headervalues[0];
            }
        }
        return null;
    }


}
