package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.message.JmsKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23/02/12
 * Time: 1:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplyConsumer extends DefaultConsumer {
    private byte[] replyBytes;
    private Envelope envelope;
    private AMQP.BasicProperties properties;

    private String correlationId;
    private boolean done = false;

    public ReplyConsumer(Channel channel, String correlationId) {
        super(channel);

        this.correlationId = correlationId;
    }

    @Override
    public void handleDelivery(String consumerTg,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               byte[] body)
            throws IOException {
        synchronized (this) {
            if (done) {
                return;
            }

            if (correlationId.equals(properties.getCorrelationId())) {
                getChannel().basicAck(envelope.getDeliveryTag(), false);

                this.replyBytes = body;
                this.envelope = envelope;
                this.properties = properties;

                done = true;
                notifyAll();
            }
        }
    }

    public void initializeMessage(StashManager stashManager, Message message, final Goid serviceGoid) throws IOException {
        message.initialize(stashManager, ContentTypeHeader.parseValue(properties.getContentType()), new ByteArrayInputStream(replyBytes));

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
                return serviceGoid == null ? new Goid(0, -1) : serviceGoid;
            }

            @Override
            public String getSoapAction() throws IOException {
                return null;
            }

            // Added for 7.0 due to API addition.
            @Override
            public String[] getHeaderValues(String name) {
                return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String[] getHeaderNames() {
                return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        message.attachJmsKnob(knob);
    }

    public void stop() {
        done = true;
    }

    public boolean isDone() {
        return done;
    }
}
