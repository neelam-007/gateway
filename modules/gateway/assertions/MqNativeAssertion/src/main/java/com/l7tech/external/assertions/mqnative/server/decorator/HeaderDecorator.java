package com.l7tech.external.assertions.mqnative.server.decorator;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQHeaderList;
import com.l7tech.external.assertions.mqnative.MqNativeMessageHeaderType;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.external.assertions.mqnative.server.MqMessageProxy;
import com.l7tech.external.assertions.mqnative.server.MqNativeConfigException;
import com.l7tech.external.assertions.mqnative.server.header.MqNativeHeaderHandler;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.HeadersKnob;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Responsible to decorate the MQMessage header.
 */
public class HeaderDecorator extends MqMessageDecorator {

    private static final String PREFIX = MQ_PREFIX + ".additionalheader.";
    private static final String MD_FORMAT = MQ_PREFIX + ".md.format";

    public HeaderDecorator(MQMessage mqMessage, MqMessageProxy source, HeadersKnob headersKnob, MqNativeRoutingAssertion assertion, PolicyEnforcementContext context, Audit audit) throws MQException, IOException, MQDataException {
        super(mqMessage, source, headersKnob, assertion, context, audit);
    }

    public HeaderDecorator(MqMessageDecorator decorator) {
        super(decorator);
    }

    @Override
    public MQMessage decorate() throws IOException, MQDataException, MQException, MqNativeConfigException {
        MQMessage mqMessage = super.decorate();

        boolean isPassThroughMessageHeader = true;

        Map<String, Object> headerOverrides = new LinkedHashMap<>();

        MqNativeMessageHeaderType headerType = MqNativeMessageHeaderType.fromValue(getOutboundHeaderAttribute(MD_FORMAT));

        //Determine request or response
        if (assertion != null) {
            if (isRequest) {
                isPassThroughMessageHeader = assertion.getRequestMqNativeMessagePropertyRuleSet().isPassThroughMqMessageHeaders();
                headerType = assertion.getRequestMqHeaderType();
                if (assertion.isRequestCopyPropertyToHeader()) {
                    headerOverrides.putAll(source.getMessageProperties());
                }
            } else {
                isPassThroughMessageHeader = assertion.getResponseMqNativeMessagePropertyRuleSet().isPassThroughMqMessageHeaders();
                headerType = assertion.getResponseMqHeaderType();
                if (assertion.isResponseCopyPropertyToHeader()) {
                    headerOverrides.putAll(source.getMessageProperties());
                }
            }
        }
        //put override values
        headerOverrides.putAll(getOutboundHeaderAttributes(PREFIX));

        MqNativeHeaderHandler handler;

        //PassThrough without override and without type conversion
        if (isPassThroughMessageHeader &&
                headerOverrides.isEmpty() &&
                (headerType == MqNativeMessageHeaderType.ORIGINAL || headerType == source.getPrimaryType())) {
            handler = new MqNativeHeaderHandler(source.getPrimaryType());
            if (handler.getMessageHeaderFormat() != null) {
                mqMessage.format = handler.getMessageHeaderFormat();
            }
            source.getHeaders().write(mqMessage);
            return mqMessage;
        }

        if (isPassThroughMessageHeader) {
            if (headerType == MqNativeMessageHeaderType.ORIGINAL || headerType == source.getPrimaryType()) {
                // No header conversion
                handler = new MqNativeHeaderHandler(source.getPrimaryHeader());
            } else {
                //header conversion
                handler = new MqNativeHeaderHandler(headerType);
                handler.applyHeaderValuesToMessage(source.getPrimaryHeaderProperties());
            }
        } else {
            if (headerType == MqNativeMessageHeaderType.ORIGINAL || headerType == source.getPrimaryType()) {
                // No header conversion
                handler = new MqNativeHeaderHandler(source.getPrimaryType());
            } else {
                //header conversion
                handler = new MqNativeHeaderHandler(headerType);
            }
        }

        if (!headerOverrides.isEmpty()) {
            Map<String, Object> convertedProperties = new LinkedHashMap<>(headerOverrides.size());
            for (Map.Entry<String, Object> entry : headerOverrides.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (StringUtils.isNotBlank(name)) {
                    if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                        convertedProperties.put(name, null);
                    } else {
                        convertedProperties.put(name, value);
                    }
                } else {
                    audit.logAndAudit(AssertionMessages.MQ_ROUTING_CANT_SET_MSG_HEADER, entry.getKey());
                }
            }
            handler.applyHeaderValuesToMessage(convertedProperties);
        }
        Object header = handler.getHeader();
        if (handler.getMessageHeaderFormat() != null) {
            mqMessage.format = handler.getMessageHeaderFormat();
        }
        MQHeaderList newHeaderList = new MQHeaderList();
        if (header != null) {
            newHeaderList.add(header);
        }

        if (isPassThroughMessageHeader) {
            MQHeaderList headerList = source.getHeaders();
            if (headerList.size() > 1) {
                for (int i = 1; i < headerList.size(); i++) {
                    newHeaderList.add(headerList.get(i));
                }
            }
        }
        newHeaderList.write(mqMessage);

        return mqMessage;
    }
}
