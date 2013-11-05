package com.l7tech.external.assertions.mqnative.server.decorator;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.external.assertions.mqnative.server.MqMessageProxy;
import com.l7tech.external.assertions.mqnative.server.MqNativeConfigException;
import com.l7tech.external.assertions.mqnative.server.header.MqNativeHeaderHandler;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.OutboundHeadersKnob;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Responsible to decorate the Property part of the MQMessage
 */
public class PropertyDecorator extends MqMessageDecorator {

    private static final String PREFIX = MQ_PREFIX + ".property.";

    public PropertyDecorator(MQMessage mqMessage, MqMessageProxy source, HeadersKnob headersKnob, MqNativeRoutingAssertion assertion, PolicyEnforcementContext context, Audit audit) throws MQException, IOException, MQDataException {
        super(mqMessage, source, headersKnob, assertion, context, audit);
    }

    public PropertyDecorator(MqMessageDecorator decorator) {
        super(decorator);
    }

    @Override
    public MQMessage decorate() throws IOException, MQDataException, MQException, MqNativeConfigException {
        MQMessage mqMessage = super.decorate();

        Map<String, Object> mpOverrides = new LinkedHashMap<String, Object>();

        if (assertion != null) {
            if (isRequest) {
                if (assertion.isRequestCopyHeaderToProperty()) {
                    mpOverrides.putAll(source.getPrimaryHeaderProperties());
                }
            } else {
                if (assertion.isResponseCopyHeaderToProperty()) {
                    mpOverrides.putAll(source.getPrimaryHeaderProperties());
                }
            }
        }

        mpOverrides.putAll(getOutboundHeaderAttributes(PREFIX));


        if (mpOverrides != null && !mpOverrides.isEmpty()) {
            for (Map.Entry<String, Object> entry : mpOverrides.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();

                String nameStr = (String) name;
                if (StringUtils.isNotBlank(nameStr)) {
                    String mqRfh2Key = MqNativeHeaderHandler.JMS_MQ_RFH2_MAP.get(name);
                    if (mqRfh2Key != null) {
                        nameStr = mqRfh2Key;
                    }
                    // delete existing property
                    try {
                        mqMessage.getObjectProperty(nameStr);
                        mqMessage.deleteProperty(nameStr);
                    } catch (MQException e) {
                        // do nothing: getObjectProperty(key) throws MQException if property does not exist
                    }
                    if (value != null) {
                        if (value instanceof String && StringUtils.isBlank((String) value)) continue;
                        mqMessage.setObjectProperty(nameStr, value);
                    }
                } else {
                    audit.logAndAudit(AssertionMessages.MQ_ROUTING_CANT_SET_MSG_PROPERTY, entry.getKey());
                }
            }
        }
        return mqMessage;
    }
}
