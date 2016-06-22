package com.l7tech.external.assertions.amqpassertion.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpSsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.objectmodel.*;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/8/12
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class AMQPDestinationHelper {

    private static final Logger logger = Logger.getLogger(AMQPDestinationHelper.class.getName());

    public static AMQPDestination[] restoreAmqpDestinations() {
        try {
            Collection<SsgActiveConnector> connectors = getTransportAdmin().findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP);
            ArrayList<AMQPDestination> destinations = new ArrayList<AMQPDestination>();
            for (SsgActiveConnector tempConnector : connectors) {
                destinations.add(ssgConnectorToAmqpDestination(tempConnector));
            }
            return destinations.toArray(new AMQPDestination[0]);
        } catch (FindException fe) {
            logger.log(Level.WARNING, fe.getMessage(), fe);
            return null;
        }
    }

    private static TransportAdmin getTransportAdmin() {
        final Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Admin context not present.");
            return null;
        }
        return registry.getTransportAdmin();
    }

    public static void saveAmqpDestinations(AMQPDestination[] destinations) throws SaveException, UpdateException {
        for (AMQPDestination destination : destinations) {
            save(destination);
        }
    }

    public static Goid addAMQPDestination(AMQPDestination destination) throws SaveException, UpdateException {
        //getEntityManager().save(destination);
        return save(destination);
    }

    public static Goid updateAMQPDestination(AMQPDestination destination) throws SaveException, UpdateException {
        //getEntityManager().save(destination);
        return save(destination);
    }

    private static synchronized Goid save(final AMQPDestination destination) throws SaveException, UpdateException {
        return getTransportAdmin().saveSsgActiveConnector(amqpDestinationToSsgActiveConnector(destination));
    }

    public static synchronized SsgActiveConnector amqpDestinationToSsgActiveConnector(AMQPDestination destination) {
        SsgActiveConnector ssgConnector = new SsgActiveConnector();
        ssgConnector.setName(destination.getName());
        ssgConnector.setGoid(destination.getGoid());
        ssgConnector.setEnabled(destination.isEnabled());
        ssgConnector.setType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTIES_KEY_IS_INBOUND, destination.isInbound(), true);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_VIRTUALHOST, destination.getVirtualHost(), false);
        if (destination.getAddresses() != null) {
            ssgConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES, objectToStringRepresentation(destination.getAddresses()));
        }
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CREDENTIALS_REQUIRED, destination.isCredentialsRequired(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USERNAME, destination.getUsername(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_PASSWORD_GOID, destination.getPasswordGoid(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USESSL, destination.isUseSsl(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CIPHERSPEC, destination.getCipherSpec(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SSL_CLIENT_KEY_ID, destination.getSslClientKeyId(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME, destination.getQueueName(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_THREADPOOLSIZE, destination.getThreadPoolSize(), false); //TODO check with Tien on how to specify that in SsgActiveConnector Framework.
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ACKNOWLEDGEMENT_TYPE, destination.getAcknowledgementType(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_BEHAVIOUR, destination.getInboundReplyBehaviour(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_QUEUE, destination.getInboundReplyQueue(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_CORRELATION_BEHAVIOUR, destination.getInboundCorrelationBehaviour(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID, destination.getServiceGoid(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_VALUE, destination.getContentTypeValue(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_PROPERTY_NAME, destination.getContentTypePropertyName(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_FAILURE_QUEUE_NAME, destination.getFailureQueueName(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME, destination.getExchangeName(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR, destination.getOutboundReplyBehaviour(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_RESPONSE_QUEUE, destination.getResponseQueue(), false);
        setNotNullProperty(ssgConnector, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_CORRELATION_BEHAVIOUR, destination.getOutboundCorrelationBehaviour(), false);
        ssgConnector.setVersion(destination.getVersion());
        return ssgConnector;
    }

    private static void setNotNullProperty(final SsgActiveConnector ssgConnector, final String propertyName, final Object value, boolean isMandatoryNotNull) {
        if (value != null) {
            // set non-null value. allow empty strings. at this point in code, the value should have been trimmed.
            ssgConnector.setProperty(propertyName, value.toString());
        } else {
            if (isMandatoryNotNull) {
                throw new RuntimeException(propertyName + " is null.");
            }
        }
    }

    public static synchronized AMQPDestination ssgConnectorToAmqpDestination(final SsgActiveConnector ssgConnector) {
        if (ssgConnector != null && ssgConnector.getType().equals(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)) {
            AMQPDestination destination = new AMQPDestination();
            destination.setVersion(ssgConnector.getVersion());
            destination.setName(ssgConnector.getName());
            destination.setGoid(ssgConnector.getGoid());
            destination.setEnabled(ssgConnector.isEnabled());
            destination.setInbound(Boolean.parseBoolean(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTIES_KEY_IS_INBOUND)));
            destination.setVirtualHost(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_VIRTUALHOST));

            String tempAddresses = ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES);
            destination.setAddresses(stringRepresentationToObject(tempAddresses));

            destination.setCredentialsRequired(Boolean.parseBoolean(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CREDENTIALS_REQUIRED)));
            destination.setUsername(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USERNAME));
            if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_PASSWORD_GOID) != null) {
                destination.setPasswordGoid(new Goid(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_PASSWORD_GOID)));
            }
            if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USESSL) != null) {
                destination.setUseSsl(Boolean.parseBoolean(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USESSL)));
            }


            destination.setCipherSpec(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CIPHERSPEC));
            destination.setSslClientKeyId(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SSL_CLIENT_KEY_ID));
            destination.setQueueName(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME));
            if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_THREADPOOLSIZE) != null) {
                destination.setThreadPoolSize(Integer.parseInt(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_THREADPOOLSIZE))); //TODO check with Tien on how to specify that in SsgActiveConnector Framework.
            }

            if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ACKNOWLEDGEMENT_TYPE) != null) {
                destination.setAcknowledgementType(JmsAcknowledgementType.valueOf(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ACKNOWLEDGEMENT_TYPE)));
            }
            if (destination.isInbound()) {
                if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_BEHAVIOUR) != null) {
                    destination.setInboundReplyBehaviour(AMQPDestination.InboundReplyBehaviour.valueOf(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_BEHAVIOUR)));
                }
                destination.setInboundReplyQueue(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_QUEUE));
                if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_CORRELATION_BEHAVIOUR) != null) {
                    destination.setInboundCorrelationBehaviour(AMQPDestination.InboundCorrelationBehaviour.valueOf(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_CORRELATION_BEHAVIOUR)));
                }
            } else {
                if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR) != null) {
                    destination.setOutboundReplyBehaviour(AMQPDestination.OutboundReplyBehaviour.valueOf(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR)));
                }
                if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_CORRELATION_BEHAVIOUR) != null) {
                    destination.setOutboundCorrelationBehaviour(AMQPDestination.OutboundCorrelationBehaviour.valueOf(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_CORRELATION_BEHAVIOUR)));
                }
            }

            if (ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID) != null) {
                destination.setServiceGoid(new Goid(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID)));
            }

            destination.setContentTypeValue(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_VALUE));
            destination.setContentTypePropertyName(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_PROPERTY_NAME));
            destination.setFailureQueueName(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_FAILURE_QUEUE_NAME));
            destination.setExchangeName(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME));

            destination.setResponseQueue(ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_RESPONSE_QUEUE));
            return destination;
        }
        return null;
    }

    protected synchronized static String objectToStringRepresentation(String[] addresses) {
        ByteArrayOutputStream bos = null;
        XMLEncoder enc = null;
        try {
            bos = new ByteArrayOutputStream();
            enc = new XMLEncoder(bos);
            enc.writeObject(addresses);
            enc.close();
            return bos.toString();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    //Ignore
                }
            }
        }
    }

    protected static synchronized String[] stringRepresentationToObject(final String xmlRepresentation) {
        ByteArrayInputStream bin = null;
        XMLDecoder dec = null;
        try {
            bin = new ByteArrayInputStream(xmlRepresentation.getBytes());
            dec = new XMLDecoder(bin);
            Object o = dec.readObject();
            if (o instanceof String[]) {
                return ((String[]) o);
            }
        } finally {
            if (bin != null) {
                try {
                    bin.close();
                } catch (IOException e) {
                    //Ignore
                }
            }
            if (dec != null) {
                dec.close();
            }
        }
        return null;
    }

    public static void removeAMQPDestinations(AMQPDestination[] destinationsToDelete) throws SaveException, FindException, DeleteException {
        for (AMQPDestination destination : destinationsToDelete) {
            getTransportAdmin().deleteSsgActiveConnector(destination.getGoid());
        }
    }
}
