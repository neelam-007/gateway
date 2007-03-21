package com.l7tech.skunkworks.gclient;

import com.l7tech.common.http.ParameterizedString;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsUtil;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.net.URI;
import java.net.PasswordAuthentication;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JMS client code "borrowed" from the ServerJmsRoutingAssertion.
 *
 * <p>The JMS url format is "borrowed" from IBM, but is probably not exactly
 * the same.</p>
 *
 * <p>An example url is:</p>
 *
 * <code>jms:/queue?destination=queue1&connectionFactory=ConnectionFactory&initialContextFactory=org.exolab.jms.jndi.InitialContextFactory&jndiProviderURL=tcp://localhost:3035</code>
 * <code>jms:/queue?destination=queue&connectionFactory=qcf&initialContextFactory=fr.dyade.aaa.jndi2.client.NamingContextFactory&jndiProviderURL=scn://localhost:16400</code>
 * <code>jms:/queue?destination=queue&connectionFactory=ConnectionFactory&initialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory&jndiProviderURL=tcp://localhost:61616</code>
 *
 * <p>This class is poorly written and minimally tested.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class JmsClient {

    //- PUBLIC

    /**
     * Create a client that sends messages to the given destination.
     *
     * @param jmsUrl The JMS url
     * @param credentials Connection credentials may be null
     */
    public JmsClient(URI jmsUrl, PasswordAuthentication credentials) {
        if (!jmsUrl.getScheme().equals("jms")) {
            throw new IllegalArgumentException("Not a JMS URL: " + jmsUrl);
        }

        this.credentials = credentials;
        data = new LinkedHashMap();
        ParameterizedString ps = new ParameterizedString(jmsUrl.getQuery());
        Set names = ps.getParameterNames();
        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            String value = ps.getParameterValue(name);
            data.put(name, value);
        }

        routedRequestConnection = new JmsConnection();
        routedRequestConnection.setOid(1);
        routedRequestConnection.setVersion(1);
        routedRequestConnection.setInitialContextFactoryClassname((String)data.get("initialContextFactory"));
        routedRequestConnection.setJndiUrl((String)data.get("jndiProviderURL"));
        routedRequestConnection.setName((String)data.get("destination"));
        if ("/queue".equals(jmsUrl.getPath()))
            routedRequestConnection.setQueueFactoryUrl((String)data.get("connectionFactory"));
        else if ("/topic".equals(jmsUrl.getPath()))
            routedRequestConnection.setTopicFactoryUrl((String)data.get("connectionFactory"));
        else
            routedRequestConnection.setDestinationFactoryUrl((String)data.get("connectionFactory"));

        routedRequestEndpoint = new JmsEndpoint();
        routedRequestEndpoint.setConnectionOid(1);
        routedRequestEndpoint.setDestinationName((String)data.get("destination"));
        routedRequestEndpoint.setMaxConcurrentRequests(1);
        routedRequestEndpoint.setMessageSource(false);

        if (data.containsKey("responseType") && "noreply".equals(data.get("responseType")) ) {
            routedRequestEndpoint.setReplyType(JmsReplyType.NO_REPLY);
        }
    }

    /**
     * Send a message.
     *
     * @param message The message to send
     * @param expectReply true if you expect a reply (this will block while waiting)
     * @return the response message or null
     * @throws Exception if something goes awry
     */
    public String getResponse(String message, boolean expectReply) throws Exception {
        Destination jmsInboundDest = null;
        MessageProducer jmsProducer = null;
        MessageConsumer jmsConsumer = null;

        try {
            Session jmsSession = null;
            Message jmsOutboundRequest = null;
            int oopses = 0;

            while (true) {
                try {
                    JmsBag bag = getJmsBag();
                    jmsSession = bag.getSession();
                    jmsOutboundRequest = makeRequest(message);
                    break; // if successful, no need for further retries
                } catch (Exception e) {
                    if (++oopses < MAX_OOPSES) {
                        String msg = "Failed to establish JMS connection on try #" +
                                oopses +  ".  Will retry after " + RETRY_DELAY + "ms.";
                        if ( jmsSession != null ) try { jmsSession.close(); } catch ( Exception e2 ) { }
                        closeBag();

                        jmsSession = null;
                        jmsOutboundRequest = null;

                        Thread.sleep(RETRY_DELAY);
                    } else {
                        String msg = "Tried " + MAX_OOPSES + " times to establish JMS connection and failed.";
                        throw e;
                    }
                }
            }

            if ( jmsSession == null || jmsOutboundRequest == null ) {
                String msg = "Null session or request escaped from retry loop!";
                throw new Exception(msg + ": " +data.toString());
            }

            Destination jmsOutboundDest = getRoutedRequestDestination();
            jmsInboundDest = responseDestination;

            String corrId = jmsOutboundRequest.getJMSCorrelationID();
            String selector = null;
            if ( corrId != null && !( jmsInboundDest instanceof TemporaryQueue ) ) {
                selector = "JMSCorrelationID = '" + escape( corrId ) + "'";
            }

            boolean inbound = expectReply
                              && jmsInboundDest != null;

            if ( jmsSession instanceof QueueSession ) {
                if ( !(jmsOutboundDest instanceof Queue ) ) throw new Exception("Destination/Session type mismatch: " + data.toString());
                jmsProducer = ((QueueSession)jmsSession).createSender( (Queue)jmsOutboundDest );
                if ( inbound )
                    jmsConsumer = ((QueueSession)jmsSession).createReceiver( (Queue)jmsInboundDest, selector );
            } else if ( jmsSession instanceof TopicSession ) {
                throw new Exception("Not implemented (TopicSession)");
            } else {
                jmsProducer = jmsSession.createProducer( jmsOutboundDest );
                if ( inbound ) jmsConsumer = jmsSession.createConsumer( jmsInboundDest, selector );
            }

            jmsProducer.send( jmsOutboundRequest );

            if ( inbound ) {
                int timeout = 15000;
                final Message jmsResponse = jmsConsumer.receive( timeout );

                if ( jmsResponse == null ) {
                    return null;
                } else {
                    if ( jmsResponse instanceof TextMessage ) {
                        return ((TextMessage)jmsResponse).getText();
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        BytesMessage bmsg = (BytesMessage)jmsResponse;
                        // Dont do this in production code (downcasting / no size checking / etc)
                        int length = (int) bmsg.getBodyLength();
                        byte[] databytes = new byte[length];
                        bmsg.readBytes(databytes);
                        return new String(databytes);
                    } else {
                        throw new Exception("Unknown response message type.");
                    }
                }
            }

            return null;
        } finally {
            try {
                if ( jmsInboundDest instanceof TemporaryQueue ) {
                    if ( jmsConsumer != null ) jmsConsumer.close();
                    ((TemporaryQueue)jmsInboundDest).delete();
                }
            } finally {
                closeBag();
            }
        }
    }

    //- PRIVATE

    private PasswordAuthentication credentials;
    private Map data = null; // props for JMS dest
    private JmsConnectionManager jmsConnectionManager;
    private JmsConnection routedRequestConnection;
    private JmsEndpoint routedRequestEndpoint;

    private JmsBag bag;
    private Destination routedRequestDestination;
    private Destination endpointResponseDestination;
    private Destination responseDestination;

    private final Logger logger = Logger.getLogger(getClass().getName());
    public static final int BUFFER_SIZE = 8192;
    private static final int MAX_OOPSES = 5;
    private static final long RETRY_DELAY = 1000;

    private synchronized void closeBag() {
        if (bag != null)
            bag.close();
        bag = null;
    }

    private String escape( String corrId ) {
        return corrId.replaceAll( "'", "''" );
    }

    private Queue getTemporaryResponseQueue() throws JmsConfigException, JMSException, NamingException {
        return getJmsBag().getSession().createTemporaryQueue();
    }

    private Destination getResponseDestination( JmsEndpoint endpoint )
            throws JMSException, NamingException, JmsConfigException {
        JmsReplyType replyType = endpoint.getReplyType();

        if (replyType == JmsReplyType.NO_REPLY) {
            return null;
        } else {
            if (replyType == JmsReplyType.AUTOMATIC) {
                return getTemporaryResponseQueue();

            } else if (replyType == JmsReplyType.REPLY_TO_OTHER) {
                return getEndpointResponseDestination();

            } else {

                String rt = (replyType == null ? "<null>" : replyType.toString());
                String msg = "Unknown JmsReplyType " + rt;
                throw new java.lang.IllegalStateException(msg);

            }
        }
    }

    private Destination getEndpointResponseDestination() throws JMSException, NamingException, JmsConfigException {
        if ( endpointResponseDestination == null ) {
            JmsEndpoint requestEndpoint = getRoutedRequestEndpoint();
            JmsEndpoint replyEndpoint = requestEndpoint.getReplyEndpoint();

            if ( requestEndpoint.getConnectionOid() != replyEndpoint.getConnectionOid() ) {
                String msg = "Request and reply endpoints must belong to the same connection";
                throw new JMSException( msg );
            }

            endpointResponseDestination = (Destination)getJmsBag().getJndiContext().lookup( replyEndpoint.getDestinationName() );
        }
        return endpointResponseDestination;
    }

    /**
     * Builds a {@link Message} to be routed to a JMS endpoint.
     * @param text contains the text to be converted into a JMS Message
     * @return the JMS Message
     * @throws IOException
     * @throws JMSException
     */
    private javax.jms.Message makeRequest( String text )
        throws IOException, JmsConfigException, JMSException, NamingException
    {
        JmsEndpoint endpoint = getRoutedRequestEndpoint();
        javax.jms.Message outboundRequestMsg = null;

        if (outboundRequestMsg == null) {
            // Default to BytesMessage
            BytesMessage bmsg = bag.getSession().createBytesMessage();
            bmsg.writeBytes(text.getBytes("UTF-8"));
            outboundRequestMsg = bmsg;
        }

        JmsReplyType replyType = endpoint.getReplyType();
        if ( replyType != JmsReplyType.NO_REPLY ) {
            // Set replyTo & correlationId
            responseDestination = getResponseDestination( endpoint );
            outboundRequestMsg.setJMSReplyTo( responseDestination );
            outboundRequestMsg.setJMSCorrelationID( "GClient-" + Long.toString(System.currentTimeMillis()) );
        }

        return outboundRequestMsg;
    }

    private Destination getRoutedRequestDestination() throws JMSException, NamingException, JmsConfigException {
        if ( routedRequestDestination == null ) {
            Context jndiContext = getJmsBag().getJndiContext();
            routedRequestDestination = (Destination)jndiContext.lookup(getRoutedRequestEndpoint().getDestinationName());
        }

        return routedRequestDestination;
    }

    private JmsEndpoint getRoutedRequestEndpoint() {
        return routedRequestEndpoint;
    }

    private JmsConnection getRoutedRequestConnection() {
        return routedRequestConnection;
    }

    private synchronized JmsBag getJmsBag() throws JMSException, NamingException, JmsConfigException {
        if ( bag == null ) {
            JmsConnection conn = getRoutedRequestConnection();
            if ( conn == null ) throw new JMSException( "JmsConnection could not be located! It may have been deleted" );
            JmsEndpoint endpoint = getRoutedRequestEndpoint();
            if ( endpoint == null ) throw new JMSException( "JmsEndpoint could not be located! It may have been deleted" );

            bag = JmsUtil.connect( conn, credentials, null );
            bag.getConnection().start();
        }
        return bag;
    }

}
