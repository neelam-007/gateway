package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.message.JmsKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import com.rabbitmq.client.*;
import org.springframework.context.ApplicationEventPublisher;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/13/12
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class AMQPConsumer extends DefaultConsumer {
    private static final Logger logger = Logger.getLogger(AMQPConsumer.class.getName());

    private AMQPDestination destination;
    private StashManagerFactory stashManagerFactory;
    private MessageProcessor messageProcessor;
    private ApplicationEventPublisher messageProcessingEventChannel;
    private ServerAMQPDestinationManager serverAMQPDestinationManager;

    public AMQPConsumer(Channel channel,
                        AMQPDestination destination,
                        StashManagerFactory stashManagerFactory,
                        MessageProcessor messageProcessor,
                        ApplicationEventPublisher messageProcessingEventChannel,
                        ServerAMQPDestinationManager serverAMQPDestinationManager) {
        super(channel);
        this.destination = destination;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessor = messageProcessor;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.serverAMQPDestinationManager = serverAMQPDestinationManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, byte[] body)
            throws java.io.IOException {
        final ContentTypeHeader ctype = getContentType(properties.getContentType(), properties.getHeaders());

        boolean messageTooLarge = serverAMQPDestinationManager.getMaxMessageSize() > 0 && serverAMQPDestinationManager.getMaxMessageSize() < body.length;

        Message request = new Message();
        request.initialize(stashManagerFactory.createStashManager(), ctype, new ByteArrayInputStream(body));

        JmsKnob knob = new JmsKnob() {
            @Override
            public boolean isBytesMessage() {
                return true;
            }

            @Override
            public Map<String, Object> getJmsMsgPropMap() {
                return properties.getHeaders();
            }

            @Override
            public Goid getServiceGoid() {
                return destination.getServiceGoid() == null ? new Goid(0, -1) : destination.getServiceGoid();
            }

            @Override
            public String getSoapAction() throws IOException {
                return null;
            }

            // These methods were added for 7.0 due to API change.
            @Override
            public String[] getHeaderValues(String name) {
                return (String[]) properties.getHeaders().values().toArray();
                //return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String[] getHeaderNames() {
                return (String[]) properties.getHeaders().keySet().toArray();
                //return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
            // End 7.0 addition.
        };
        request.attachJmsKnob(knob);

        // When messages come in via AMQP, there may not be any headers, so we need to ensure they exist first
        if (null != knob.getJmsMsgPropMap()) {
            // As of 8.2.00 you must add the headers from the Jmsknob into the request HeadersKnob so they are accessible in policy
            for (Map.Entry<String, Object> headers : knob.getJmsMsgPropMap().entrySet()) {
                Map.Entry pairs = (Map.Entry) headers;
                request.getHeadersKnob().addHeader((String) pairs.getKey(), pairs.getValue(), HEADER_TYPE_JMS_PROPERTY);
            }
        }

        PolicyEnforcementContext context = null;
        String faultMessage = null;
        String faultCode = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;
        boolean responseSuccess = false;
        InputStream responseStream = null;
        ContentTypeHeader responseContentType = ContentTypeHeader.XML_DEFAULT;

        try {
            final boolean replyExpected = getReplyExpected(properties.getReplyTo(), properties.getCorrelationId(), properties.getMessageId());
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, replyExpected);

            boolean stealthMode = false;
            if (!messageTooLarge) {
                try {
                    status = messageProcessor.processMessage(context);
                    context.setPolicyResult(status);
                    logger.finest("Policy resulted in status " + status);
                    if (context.getResponse().getKnob(XmlKnob.class) != null ||
                            context.getResponse().getKnob(MimeKnob.class) != null) {
                        // if the policy is not successful AND the stealth flag is on, drop connection
                        if (status != AssertionStatus.NONE && context.isStealthResponseMode()) {
                            logger.info("Policy returned error and stealth mode is set. " +
                                    "Not sending response message.");
                            stealthMode = true;
                        } else {
                            // add more detailed diagnosis message
                            if (!context.getResponse().isXml()) {
                                logger.log(Level.WARNING, "Response message is non-XML, the ContentType is: {0}", context.getRequest().getMimeKnob().getOuterContentType());
                                responseStream = context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream();
                            } else {
                                responseStream = new ByteArrayInputStream(XmlUtil.nodeToString(context.getResponse().getXmlKnob().getDocumentReadOnly()).getBytes());
                            }
                            responseContentType = context.getResponse().getMimeKnob().getOuterContentType();
                        }
                    } else {
                        logger.finer("No response received");
                        responseStream = null;
                    }
                } catch (PolicyVersionException pve) {
                    String msg1 = "Request referred to an outdated version of policy";
                    logger.log(Level.INFO, msg1);
                    faultMessage = msg1;
                    faultCode = SoapUtil.FC_CLIENT;
                } catch (Throwable t) {
                    logger.warning("Exception while processing AMQP message: " + ExceptionUtils.getMessage(t));
                    faultMessage = t.getMessage();
                    if (faultMessage == null) faultMessage = t.toString();
                }
            } else {
                String msg1 = "Request message too large";
                logger.log(Level.INFO, msg1);
                faultMessage = msg1;
                faultCode = SoapUtil.FC_CLIENT;
            }

            if (responseStream == null) {
                if (context.isStealthResponseMode()) {
                    logger.info("No response data available and stealth mode is set. " +
                            "Not sending response message.");
                    stealthMode = true;
                } else {
                    if (faultMessage == null) faultMessage = status.getMessage();
                    try {
                        String faultXml = SoapFaultUtils.generateSoapFaultXml(
                                (context.getService() != null) ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN,
                                faultCode == null ? SoapUtil.FC_SERVER : faultCode,
                                faultMessage, null, "");

                        responseStream = new ByteArrayInputStream(faultXml.getBytes(Charsets.UTF8));

                        if (faultXml != null) {
                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        }
                    } catch (SAXException e) {
                        throw new IOException(e);
                    }
                }
            }

            if (!stealthMode && replyExpected) {
                long startResp = System.currentTimeMillis();
                responseSuccess = sendResponse(getReplyQueueName(properties.getReplyTo()), properties.getCorrelationId(), properties.getMessageId(), responseContentType, null, responseStream);
                logger.log(Level.INFO, "Send response took {0} millis; listener {1}", new Object[]{(System.currentTimeMillis() - startResp), destination.getName()});
            } else { // is stealth mode
                responseSuccess = true;
            }
        } finally {
            ResourceUtils.closeQuietly(context);
            manageFailureDeliveryAndAcknowledgement(envelope, properties, status, responseSuccess, new ByteArrayInputStream(body), responseContentType, properties.getHeaders());
        }
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        // Handle the case where a shutdown signal is sent from the AMQP server; this means that we will have to reconnet to the
        // server as a consumer eventually. Therefore we need to remove the destination from the active server channels and addd the
        // destination to the failed consumers so that there will be re-connect attempts
        logger.log(Level.WARNING, "Connection to AMQP destination " + destination.getGoid() + " was closed by server.");
        serverAMQPDestinationManager.getServerChannels().remove(destination.getGoid());
        serverAMQPDestinationManager.getFailedConsumers().put(destination.getGoid(), System.currentTimeMillis());
        super.handleShutdownSignal(consumerTag, sig);
    }

    /**
     * Determine the content-type to use for the request message when calling the MessageProcessor.  The default
     * will be text/xml.
     *
     * @param headers inbound request headers
     * @return ContentTypeHeader instance that can be used for the MessageProcessor
     * @throws IOException error parsing content type
     */
    private ContentTypeHeader getContentType(String requestContentType, Map<String, Object> headers)
            throws IOException {
        if (destination.getContentTypeValue() != null) {
            try {
                return ContentTypeHeader.parseValue(destination.getContentTypeValue());
            } catch (IOException e) {
                return ContentTypeHeader.XML_DEFAULT;
            }
        } else if (destination.getContentTypePropertyName() != null) {
            if (headers.containsKey(destination.getContentTypePropertyName())) {
                try {
                    return ContentTypeHeader.parseValue((String) headers.get(destination.getContentTypePropertyName()));
                } catch (IOException e) {
                    return ContentTypeHeader.XML_DEFAULT;
                }
            } else {
                return ContentTypeHeader.XML_DEFAULT;
            }
        } else {
            try {
                return ContentTypeHeader.parseValue(requestContentType);
            } catch (IOException e) {
                return ContentTypeHeader.XML_DEFAULT;
            }
        }
    }

    /**
     * This method determines whether a response on a reply queue is required
     *
     * @param replyToQueueName reply to queue name from request message
     * @param correlationId    the correlation ID from the request message
     * @param requestMessageId the message ID from the request message
     * @return flag specifying whether a MQ reply is needed
     */
    private boolean getReplyExpected(String replyToQueueName, String correlationId, String requestMessageId) {
        switch (destination.getInboundReplyBehaviour()) {
            case AUTOMATIC:
                if (replyToQueueName == null || replyToQueueName.trim().isEmpty()) {
                    return false;
                }

                if (AMQPDestination.InboundCorrelationBehaviour.CORRELATION_ID == destination.getInboundCorrelationBehaviour()) {
                    return correlationId != null && !correlationId.trim().isEmpty();
                } else {
                    return requestMessageId != null && !requestMessageId.trim().isEmpty();
                }
            case ONE_WAY:
                return false;
            case SPECIFIED_QUEUE:
                if (AMQPDestination.InboundCorrelationBehaviour.CORRELATION_ID == destination.getInboundCorrelationBehaviour()) {
                    return correlationId != null && !correlationId.trim().isEmpty();
                } else {
                    return requestMessageId != null && !requestMessageId.trim().isEmpty();
                }
        }

        return false;
    }

    private String getReplyQueueName(String replyToQueueName) {
        switch (destination.getInboundReplyBehaviour()) {
            case AUTOMATIC:
                return replyToQueueName;
            case ONE_WAY:
                return null;
            case SPECIFIED_QUEUE:
                return destination.getInboundReplyQueue();
        }

        return null;
    }

    private boolean sendResponse(String queueName, String requestCorrelationId, String requestMessageId, ContentTypeHeader responseContentType,
                                 Map<String, Object> amqpHeaders, InputStream is) throws IOException {
        AMQP.BasicProperties.Builder propertiesBuilder = new AMQP.BasicProperties.Builder().
                contentType(responseContentType.getFullValue()).
                timestamp(new Date()).headers(amqpHeaders);
        if (AMQPDestination.InboundCorrelationBehaviour.CORRELATION_ID == destination.getInboundCorrelationBehaviour()) {
            propertiesBuilder = propertiesBuilder.correlationId(requestCorrelationId);
        } else {
            propertiesBuilder = propertiesBuilder.correlationId(requestMessageId);
        }

        getChannel().basicPublish("", queueName, true, false, propertiesBuilder.build(), IOUtils.slurpStream(is));

        return true;
    }

    /**
     * This method manages delivery of failed messages to a different queue (if set) and acknowledges messages
     * off the queue if 'Acknowledgement Behaviour' is set to On Completion.
     *
     * @throws IOException
     */
    private void manageFailureDeliveryAndAcknowledgement(Envelope envelope,
                                                         AMQP.BasicProperties properties,
                                                         AssertionStatus status,
                                                         boolean responseSuccess,
                                                         InputStream responseStream,
                                                         ContentTypeHeader responseContentType,
                                                         Map<String, Object> amqpHeaders) throws IOException {
        boolean handledAnyFailure;
        handledAnyFailure = status == AssertionStatus.NONE || (destination.isFailureQueueNameSet() &&
                sendResponse(destination.getFailureQueueName(), properties.getCorrelationId(),
                        properties.getMessageId(), responseContentType, amqpHeaders, responseStream));
        if (responseSuccess && handledAnyFailure) {
            if (JmsAcknowledgementType.ON_COMPLETION == destination.getAcknowledgementType()) {
                getChannel().basicAck(envelope.getDeliveryTag(), true);
            }
        } else {
            // If we fail to process the message from the queue, add it back
            // see http://www.rabbitmq.com/blog/2010/08/03/well-ill-let-you-go-basicreject-in-rabbitmq/ for RabbitMQ's
            // implementation. This seems to cause the message to be delivered to the same Consumer repeatedly in a loop.
            // If the customer uses RabbitMQ, they need to configure a Dead Letter Exchange and timeout on the queue or it will
            // repeatedly deliver the same message to the SSG (Consumer)
            getChannel().basicNack(envelope.getDeliveryTag(), false, true);
        }
    }
}
