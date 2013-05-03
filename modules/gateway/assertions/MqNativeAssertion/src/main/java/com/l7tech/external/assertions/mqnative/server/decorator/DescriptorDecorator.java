package com.l7tech.external.assertions.mqnative.server.decorator;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.external.assertions.mqnative.server.MqMessageProxy;
import com.l7tech.external.assertions.mqnative.server.MqNativeConfigException;
import com.l7tech.external.assertions.mqnative.server.MqNativeUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.OutboundHeadersKnob;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.HexUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * Responsible to decorate the MQMessage Descriptor
 */
public class DescriptorDecorator extends MqMessageDecorator {

    private static final String PREFIX = MQ_PREFIX + ".md.";
    private static final String VERSION = "version";

    public DescriptorDecorator(MQMessage mqMessage, MqMessageProxy source, OutboundHeadersKnob outboundHeadersKnob, MqNativeRoutingAssertion assertion, PolicyEnforcementContext context, Audit audit) throws MQException, IOException, MQDataException {
        super(mqMessage, source, outboundHeadersKnob, assertion, context, audit);
    }

    public DescriptorDecorator(MqMessageDecorator decorator) {
        super(decorator);
    }

    @Override
    public MQMessage decorate() throws IOException, MQDataException, MQException, MqNativeConfigException {
        MQMessage mqMessage = super.decorate();

        //Get customized header from Knob
        Map<String, Object> mdOverrides = getOutboundHeaderAttributes(PREFIX);

        //Get customized header by assertion configuration.
        if (assertion != null) {
            if (isRequest) {
                if (assertion.getRequestMessageDescriptorOverrides() != null && !assertion.getRequestMessageDescriptorOverrides().isEmpty()) {
                    mdOverrides.putAll(assertion.getRequestMessageDescriptorOverrides());
                }
            } else {
                if (assertion.getResponseMessageDescriptorOverrides() != null && !assertion.getResponseMessageDescriptorOverrides().isEmpty()) {
                    mdOverrides.putAll(assertion.getResponseMessageDescriptorOverrides());
                }
            }
        }

        //Populate message descriptor
        if (mdOverrides != null && !mdOverrides.isEmpty()) {

            for (final Map.Entry<String, Object> propertyEntry : mdOverrides.entrySet()) {
                String name = propertyEntry.getKey();
                Object value = propertyEntry.getValue();

                try {
                    Field field = MQMessage.class.getField(name);
                    Class type = field.getType();
                    if (type.isAssignableFrom(byte[].class)) {
                        field.set(mqMessage, asBytes((String) value));
                    } else if (type.isAssignableFrom(int.class)) {
                        field.set(mqMessage, asInt(name, (String) value));
                    } else if (type.isAssignableFrom(GregorianCalendar.class)) {
                        field.set(mqMessage, asCalendar(name, (String) value));
                    } else {
                        field.set(mqMessage, value);
                    }
                } catch (NoSuchFieldException e) {
                    if (name.equals(VERSION)) {
                        mqMessage.setVersion(asInt(name, (String) value));
                    } else {
                        audit.logAndAudit(AssertionMessages.MQ_ROUTING_CANT_SET_MSG_DESCRIPTOR, name);
                    }
                } catch (IllegalAccessException e) {
                    audit.logAndAudit(AssertionMessages.MQ_ROUTING_CANT_SET_MSG_DESCRIPTOR, name);
                }
            }
        }

        return mqMessage;
    }

    /**
     * Convert String to Integer
     *
     * @param name  The name of the descriptor
     * @param value The value of the descriptor
     * @return The value as Integer
     * @throws MqNativeConfigException
     */
    private static int asInt(final String name, final String value) throws MqNativeConfigException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            throw new MqNativeConfigException("Invalid value '" + value + "' for property '" + name + "'");
        }
    }

    /**
     * Convert Base64 String as Byte[]
     *
     * @param value The value of the descriptor
     * @return The value as byte[]
     */
    private static byte[] asBytes(final String value) {
        return HexUtils.decodeBase64(value);
    }

    /**
     * Convert String to GregorianCalendar
     *
     * @param name  The name of the descriptor
     * @param value The value of the descriptor
     * @return The value as GregorianCalendar
     * @throws MqNativeConfigException When fail to format the string
     */
    private static GregorianCalendar asCalendar(final String name, final String value) throws MqNativeConfigException {
        try {
            GregorianCalendar c = (GregorianCalendar) GregorianCalendar.getInstance();
            c.setTime(MqNativeUtils.DATE_FORMAT.parse(value));
            return c;
        } catch (ParseException e) {
            throw new MqNativeConfigException("Invalid value '" + value + "' for property '" + name + "'");
        }
    }

}
