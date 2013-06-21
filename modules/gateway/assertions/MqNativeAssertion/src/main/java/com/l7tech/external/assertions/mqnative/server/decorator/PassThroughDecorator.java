package com.l7tech.external.assertions.mqnative.server.decorator;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.external.assertions.mqnative.server.MqMessageProxy;
import com.l7tech.external.assertions.mqnative.server.MqNativeConfigException;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.OutboundHeadersKnob;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;
import java.util.Map;

/**
 * Responsible to pass through the MQMessage.
 */
public class PassThroughDecorator extends MqMessageDecorator {

    public PassThroughDecorator(MQMessage mqMessage, MqMessageProxy source, OutboundHeadersKnob outboundHeadersKnob, MqNativeRoutingAssertion assertion, PolicyEnforcementContext context, Audit audit) throws MQException, IOException, MQDataException {
        super(mqMessage, source, outboundHeadersKnob, assertion, context, audit);
    }

    public PassThroughDecorator(MqMessageDecorator decorator) {
        super(decorator);
    }

    @Override
    public MQMessage decorate() throws IOException, MQDataException, MQException, MqNativeConfigException {
        MQMessage mqMessage = super.decorate();

        boolean passThroughMessageDescriptor = true;
        boolean passThroughMessageProperty = true;

        if (assertion != null) {
            if (isRequest) {
                passThroughMessageDescriptor = assertion.getRequestMqNativeMessagePropertyRuleSet().isPassThroughHeaders();
                passThroughMessageProperty = assertion.getRequestMqNativeMessagePropertyRuleSet().isPassThroughMqMessageProperties();
            } else {
                passThroughMessageDescriptor = assertion.getResponseMqNativeMessagePropertyRuleSet().isPassThroughHeaders();
                passThroughMessageProperty = assertion.getResponseMqNativeMessagePropertyRuleSet().isPassThroughMqMessageProperties();
            }
        }

        //Pass through Message Descriptor
        if (passThroughMessageDescriptor) {
            source.getMessageDescriptor().copyTo(mqMessage);
        }

        //Pass through Message Properties
        if (passThroughMessageProperty) {
            for (Map.Entry<String, Object> entry : source.getMessageProperties().entrySet()) {
                mqMessage.setObjectProperty(entry.getKey(), entry.getValue());
            }
        }

        //Handle the Header passthrough in HeaderDecorator
        return mqMessage;
    }
}
